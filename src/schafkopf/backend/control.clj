;; TODO rename to game
(ns schafkopf.backend.control
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [schafkopf.game :as game]))

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
   ::game (game/game)
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
     (game/player-game (::game server-game) seat)
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

;; TODO: Will this fire if old == new?
(defn- broadcaster-watch
  "Watches a game ref and broadcasts changes to all clients."
  [_key _atom _old new-server-game]
  (broadcast-event! new-server-game [:game/update]))

(mount/defstate broadcaster
  :start (add-watch game-atom ::broadcaster broadcaster-watch)
  :stop (remove-watch game-atom ::broadcaster))

(defn free-seats
  "Returns the set of free seats in a game."
  [server-game]
  (remove (->> (::clients server-game)
               (map (comp ::seat second))
               (set))
          (range 4)))

;; TODO: Separate join-game! (for guests) and host-game.
;; Mark the host so that clients can refer to them.
(defn join-game!
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
                       (update ::seqno inc)))))
        server-game (swap! game-atom join)]
    (when (some? (get-in server-game [::clients uid]))
      (timbre/info "User" uid "joined game" (::code server-game))
      (client-game server-game uid))))

(defn- in-sync? [server-game seqno]
  (= seqno (::seqno server-game)))

(defn- progress [server-game]
  (update server-game ::seqno inc))

(defn- progressed? [server-game seqno]
  (= (inc seqno) (::seqno server-game)))

;; TODO: annotate what command was ignored?
(defn- unchanged [server-game]
  server-game)

(defn can-start? [server-game]
  (and (= 4 (count (::clients server-game)))
       (not (game/started? (::game server-game)))))

(defn start-game [server-game seqno]
  (if (and (in-sync? server-game seqno)
           (can-start? server-game))
    (let [dealer-seat (game/rand-seat)
          deck (game/shuffled-deck)]
      (-> server-game
          (update ::game
                  #(-> %
                       (game/start dealer-seat)
                       (game/deal deck)))
          (progress)))
    (unchanged server-game)))

;; TODO - verify uid is a player?
(defn start-game! [game-atom uid seqno]
  (let [server-game (swap! game-atom start-game seqno)]
    (when (progressed? server-game seqno)
      (timbre/info "User" uid "started game" (::code server-game)))))

(defn can-play? [server-game seat]
  (let [game (::game server-game)]
    (and (not (game/trick-complete? game))
         (game/player-turn? game seat))))

;; TODO - ensure card in hand
(defn play [server-game uid seqno card]
  (let [seat (get-in server-game [::clients uid ::seat])]
    (if (and (in-sync? server-game seqno)
             (can-play? server-game seat))
      (-> server-game
          (update ::game game/play-card card)
          (progress))
      (unchanged server-game))))

(defn play! [game-atom uid seqno card]
  (let [server-game (swap! game-atom play uid seqno card)]
    (when (progressed? server-game seqno)
      (timbre/info "User" uid "played card" card))))