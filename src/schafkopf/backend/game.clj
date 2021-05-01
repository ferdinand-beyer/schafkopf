(ns schafkopf.backend.game
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [schafkopf.game :as g]
            ;; for :client/name spec
            [schafkopf.protocol]))

(s/def ::uid string?)

(s/def ::client (s/keys :req-un [::uid]))

(defn generate-code []
  (format "%04d" (rand-int 10000)))

(defn generate-uid []
  (.toString (java.util.UUID/randomUUID)))

;; TODO Replace with API-level spec for payload!
(defn valid-name? [name]
  (s/valid? :client/name name))

;; TODO: Unique game-id, use code only for joining
(defn server-game []
  {::code (generate-code)
   ::seqno 0
   ::game (g/game)
   ::clients {}})

(mount/defstate game-atom
  :start (atom nil))

;; TODO: Create a new game when hosting, keep a map of games
;; (destroy games when hosts disconnect)
(defn ensure-game!
  "Creates a game unless it exists already, then returns the
   game singleton."
  []
  (swap! game-atom #(or % (server-game)))
  game-atom)

(defn find-game
  "Finds a game by its code, nil otherwise."
  [code]
  (when (= code (::code @game-atom))
    game-atom))

;;;; Client game

(defn- populate-peers [client-game clients]
  (reduce
   (fn [g [_ {::keys [seat name]}]]
     (assoc-in g [:player/peers seat :client/name] name))
   client-game
   clients))

(defn client-game
  "Returns the view of the game for a client identified by their uid."
  [server-game uid]
  (when-let [seat (get-in server-game [::clients uid ::seat])]
    (->
     (g/player-game (::game server-game) seat)
     (assoc :server/code (::code server-game)
            :server/seqno (::seqno server-game))
     (populate-peers (::clients server-game)))))

(defn broadcast-event!
  "Sends an event to all connected clients."
  [server-game event]
  {:pre [(vector? event)]}
  (doseq [[uid {::keys [send-fn]}] (::clients server-game)]
    (when send-fn
      (send-fn (conj event (client-game server-game uid))))))

(defn- broadcaster-watch
  "Watches a game ref and broadcasts changes to all clients."
  [_key _atom old-server-game new-server-game]
  (when (not= old-server-game new-server-game)
    (broadcast-event! new-server-game [:game/update])))

(mount/defstate broadcaster
  :start (add-watch game-atom ::broadcaster broadcaster-watch)
  :stop (remove-watch game-atom ::broadcaster))

;;;; Server game

(defn valid-client? [server-game uid]
  (contains? (::clients server-game) uid))

(defn- in-sync? [server-game seqno]
  (= seqno (::seqno server-game)))

(defn client-ok? [server-game uid seqno]
  (and (valid-client? server-game uid)
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

;; TODO: Separate join-game! (for guests) and host-game.
;; Mark the host so that clients can refer to them.
(defn join!
  "Makes a user join a game.  If they are already playing, does nothing.
   Returns the client-game of the joined player, or nil when there are no
   more free seats in the game."
  [game-atom uid name send-fn]
  (let [join (fn [server-game]
               (let [seat (first (free-seats server-game))]
                 (cond-> server-game
                   (and (some? seat)
                        (nil? (get-in server-game [::clients uid])))
                   (-> (assoc-in [::clients uid]
                                 {::uid uid
                                  ::send-fn send-fn
                                  ::name name
                                  ::seat seat})
                       (progress)))))
        server-game (swap! game-atom join)]
    (when (some? (get-in server-game [::clients uid]))
      (log/info "User" uid "joined game" (::code server-game))
      (client-game server-game uid))))

(defn can-start? [server-game]
  (and (= 4 (count (::clients server-game)))
       (not (g/started? (::game server-game)))))

(defn start [server-game uid seqno]
  (if (and (client-ok? server-game uid seqno)
           (can-start? server-game))
    (-> server-game
        (update ::game
                #(-> %
                     (g/start (g/rand-seat))
                     (g/deal (g/shuffled-deck))))
        (progress))
    (unchanged server-game)))

(defn start! [game-atom uid seqno]
  (let [server-game (swap! game-atom start uid seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" uid "started game" (::code server-game)))))

(defn can-play? [server-game seat card]
  (let [game (::game server-game)]
    (and (not (g/trick-complete? game))
         (g/player-turn? game seat)
         (g/has-card? game seat card))))

(defn play [server-game uid seqno card]
  (let [seat (get-in server-game [::clients uid ::seat])]
    (if (and (client-ok? server-game uid seqno)
             (can-play? server-game seat card))
      (-> server-game
          (update ::game g/play-card card)
          (progress))
      (unchanged server-game))))

(defn play! [game-atom uid seqno card]
  (let [server-game (swap! game-atom play uid seqno card)]
    (when (progressed? server-game seqno)
      (log/info "User" uid "played card" card))))

(defn can-take? [server-game]
  (let [game (::game server-game)]
    (g/trick-complete? game)))

(defn take-trick [server-game uid seqno]
  (let [seat (get-in server-game [::clients uid ::seat])]
    (if (and (client-ok? server-game uid seqno)
             (can-take? server-game))
      (let [server-game (update server-game ::game g/take-trick seat)
            game (::game server-game)]
        (cond-> server-game
          (g/all-taken? game)
          (update ::game g/summarize)

          :finally (progress)))
      (unchanged server-game))))

(defn take! [game-atom uid seqno]
  (let [server-game (swap! game-atom take-trick uid seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" uid "took the trick"))))

(defn can-score? [server-game score]
  (let [game (::game server-game)]
    (and (g/can-score? game)
         (g/valid-score? game score))))

(defn score [server-game uid seqno score]
  (if (and (client-ok? server-game uid seqno)
           (can-score? server-game score))
    (-> server-game
        (update ::game g/score score)
        (progress))
    (unchanged server-game)))

(defn score! [game-atom uid seqno game-score]
  (let [server-game (swap! game-atom score uid seqno game-score)]
    (when (progressed? server-game seqno)
      (log/info "User" uid "scored the game"))))

(defn can-start-next? [server-game]
  (let [game (::game server-game)]
    (g/scored? game)))

(defn start-next [server-game uid seqno]
  (if (and (client-ok? server-game uid seqno)
           (can-start-next? server-game))
    (-> server-game
        (update ::game #(-> % (g/start-next) (g/deal (g/shuffled-deck))))
        (progress))
    (unchanged server-game)))

;; TODO: Same as start?
(defn start-next! [game-atom uid seqno]
  (let [server-game (swap! game-atom start-next uid seqno)]
    (when (progressed? server-game seqno)
      (log/info "User" uid "started the next game"))))
