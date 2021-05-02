(ns schafkopf.backend.game
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [schafkopf.game :as g]))

(defn generate-id []
  (.toString (java.util.UUID/randomUUID)))

(defn generate-join-code []
  (format "%04d" (rand-int 10000)))

;; Currently we only support one running game at a time.  When
;; we change that, we propably want to use refs to synchronize
;; IDs and join codes.
(mount/defstate game-atom
  :start (atom nil))

(defn find-game-by-id [game-id]
  (when (= game-id (::game-id @game-atom))
    game-atom))

(defn find-game-by-join-code [join-code]
  (when (= join-code (::join-code @game-atom))
    game-atom))

(defn- client-exists? [server-game client-id]
  (contains? (::clients server-game) client-id))

(defn client? [game-atom client-id]
  (client-exists? @game-atom client-id))

;;;; Client game

(defn- populate-peers [client-game clients]
  (reduce
   (fn [g [_ {::keys [seat name]}]]
     (assoc-in g [:player/peers seat :client/name] name))
   client-game
   clients))

(defn client-game
  "Returns the view of the game for a client identified by their id."
  [server-game client-id]
  (when-let [seat (get-in server-game [::clients client-id ::seat])]
    (->
     (g/player-game (::game server-game) seat)
     (assoc :client/client-id client-id
            :server/game-id (::game-id server-game)
            :server/join-code (::join-code server-game)
            :server/seqno (::seqno server-game))
     (populate-peers (::clients server-game)))))

(defn- broadcast-event!
  "Sends an event to all connected clients."
  [server-game event]
  {:pre [(vector? event)]}
  (doseq [[client-id {::keys [send-fn]}] (::clients server-game)]
    (when send-fn
      (send-fn (conj event (client-game server-game client-id))))))

(defn- broadcaster-watch
  "Watches a game ref and broadcasts changes to all clients."
  [_key _atom old-server-game new-server-game]
  (when (not= old-server-game new-server-game)
    (broadcast-event! new-server-game [:game/update])))

(mount/defstate broadcaster
  :start (add-watch game-atom ::broadcaster broadcaster-watch)
  :stop (remove-watch game-atom ::broadcaster))

;;;; Server game

(defn- in-sync? [server-game seqno]
  (= seqno (::seqno server-game)))

(defn- client-ok? [server-game client-id seqno]
  (and (client-exists? server-game client-id)
       (in-sync? server-game seqno)))

(defn- progress [server-game]
  (update server-game ::seqno inc))

(defn- progressed? [server-game seqno]
  (= (inc seqno) (::seqno server-game)))

;; TODO: annotate what command was ignored?
(defn- unchanged [server-game]
  server-game)

(defn free-seats
  "Returns the set of free seats in a game."
  [server-game]
  (remove (->> (::clients server-game)
               (map (comp ::seat second))
               (set))
          (range 4)))

(defn- destroy-game!
  "Destroys a game, letting all connected clients know."
  [game-atom]
  (when-let [server-game @game-atom]
    (broadcast-event! server-game [:game/stop])
    (log/info "Game destroyed:" (::game-id server-game))))

(defn- register-game! [server-game]
  (destroy-game! game-atom)
  (reset! game-atom (assoc server-game
                           ::game-id (generate-id)
                           ::join-code (generate-join-code)))
  game-atom)

(defn- create-game! [client-id]
  (register-game!
   {::seqno 0
    ::game (g/game)
    ::clients {}
    ::host client-id}))

(defn- join-game [server-game client-id name send-fn]
  (let [seat (first (free-seats server-game))]
    (if (and (some? seat)
             (not (contains? (::clients server-game) client-id)))
      (-> server-game
          (assoc-in [::clients client-id]
                    {::client-id client-id
                     ::send-fn send-fn
                     ::name name
                     ::seat seat})
          (progress))
      (unchanged server-game))))

(defn host!
  "Hosts a new game."
  [client-id name send-fn]
  (let [game-atom (create-game! client-id)
        server-game (swap! game-atom join-game client-id name send-fn)]
    (log/info "User" client-id "named" name "created game"
              (::game-id server-game) "- Join Code:" (::join-code server-game))
    (client-game server-game client-id)))

(defn join!
  "Joins a running game."
  [game-atom client-id name send-fn]
  (let [server-game (swap! game-atom join-game client-id name send-fn)]
    (when (client-exists? server-game client-id)
      (log/info "User" client-id "named" name "joined game"
                (::game-id server-game))
      (client-game server-game client-id))))

(defn can-start? [server-game]
  (and (= 4 (count (::clients server-game)))
       (not (g/started? (::game server-game)))))

(defn start [server-game client-id seqno]
  (if (and (client-ok? server-game client-id seqno)
           (can-start? server-game))
    (-> server-game
        (update ::game
                #(-> %
                     (g/start (g/rand-seat))
                     (g/deal (g/shuffled-deck))))
        (progress))
    (unchanged server-game)))

(defn start! [game-atom client-id seqno]
  (let [server-game (swap! game-atom start client-id seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "started game" (::join-code server-game)))))

(defn can-skip? [server-game]
  (let [game (::game server-game)]
    (and (g/started? game)
         (not (g/scored? game)))))

(defn skip [server-game client-id seqno]
  (if (and (client-ok? server-game client-id seqno)
           (can-skip? server-game))
    (-> server-game
        (update ::game g/skip)
        (progress))
    (unchanged server-game)))

(defn skip! [game-atom client-id seqno]
  (let [server-game (swap! game-atom skip client-id seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "skipped this game"))))

(defn can-play? [server-game seat card]
  (let [game (::game server-game)]
    (and (not (g/trick-complete? game))
         (g/player-turn? game seat)
         (g/has-card? game seat card))))

(defn play [server-game client-id seqno card]
  (let [seat (get-in server-game [::clients client-id ::seat])]
    (if (and (client-ok? server-game client-id seqno)
             (can-play? server-game seat card))
      (-> server-game
          (update ::game g/play-card card)
          (progress))
      (unchanged server-game))))

(defn play! [game-atom client-id seqno card]
  (let [server-game (swap! game-atom play client-id seqno card)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "played card" card))))

(defn can-take? [server-game]
  (let [game (::game server-game)]
    (g/trick-complete? game)))

(defn take-trick [server-game client-id seqno]
  (let [seat (get-in server-game [::clients client-id ::seat])]
    (if (and (client-ok? server-game client-id seqno)
             (can-take? server-game))
      (let [server-game (update server-game ::game g/take-trick seat)
            game (::game server-game)]
        (cond-> server-game
          (g/all-taken? game)
          (update ::game g/summarize)

          :finally (progress)))
      (unchanged server-game))))

(defn take! [game-atom client-id seqno]
  (let [server-game (swap! game-atom take-trick client-id seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "took the trick"))))

(defn can-score? [server-game score]
  (let [game (::game server-game)]
    (and (g/can-score? game)
         (g/valid-score? game score))))

(defn score [server-game client-id seqno score]
  (if (and (client-ok? server-game client-id seqno)
           (can-score? server-game score))
    (-> server-game
        (update ::game g/score score)
        (progress))
    (unchanged server-game)))

(defn score! [game-atom client-id seqno game-score]
  (let [server-game (swap! game-atom score client-id seqno game-score)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "scored the game"))))

(defn can-start-next? [server-game]
  (let [game (::game server-game)]
    (g/scored? game)))

(defn start-next [server-game client-id seqno]
  (if (and (client-ok? server-game client-id seqno)
           (can-start-next? server-game))
    (-> server-game
        (update ::game #(-> % (g/start-next) (g/deal (g/shuffled-deck))))
        (progress))
    (unchanged server-game)))

;; TODO: Same as start?
(defn start-next! [game-atom client-id seqno]
  (let [server-game (swap! game-atom start-next client-id seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" client-id "started the next game"))))
