(ns schafkopf.backend.control
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [schafkopf.game :as game]))

(s/def ::uid string?)

(s/def ::client (s/keys :req-un [::uid]))

(mount/defstate game-atom
  :start (atom nil))

(defn generate-code []
  (format "%04d" (rand-int 10000)))

(defn generate-uid []
  (.toString (java.util.UUID/randomUUID)))

;; TODO Replace with API-level spec for payload!
(defn valid-name? [name]
  (s/valid? :client/name name))

(defn server-game []
  (assoc (game/game)
         ::code (generate-code)
         ::users {}))

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

(defn enrich-peers [client-game clients]
  (reduce
   (fn [g [_ {::keys [seat name]}]]
     (assoc-in g [:player/peers seat :client/name] name))
   client-game
   clients))

(defn client-game
  "Returns the view of the game for a client identified by their uid."
  [game uid]
  (when-let [seat (get-in game [::users uid ::seat])]
    (->
     (game/player-game game seat)
     (assoc :server/code (::code game))
     (enrich-peers (::users game)))))

(defn free-seats
  "Returns the set of free seats in a game."
  [game]
  (remove (->> (::users game)
               (map (comp ::seat second))
               (set))
          (range 4)))

(defn broadcast-event!
  "Sends an event to all connected users of a game."
  [game event]
  {:pre [(vector? event)]}
  (doseq [[uid {::keys [send-fn]}] (::users game)]
    (when send-fn
      (send-fn (conj event (client-game game uid))))))

;; TODO: This could be a watcher for a game-atom!
(defn broadcast-game! [game]
  (broadcast-event! game [:game/update]))

;; TODO: Separate join-game! (for guests) and host-game.
;; Mark the host so that clients can refer to them.
(defn join-game!
  "Makes a user join a game.  If they are already playing, does nothing.
   Returns the client-game of the joined player, or nil when there are no
   more free seats in the game."
  [game-atom uid name send-fn]
  (if (some? (get-in @game-atom [::users uid]))
    (client-game @game-atom uid)
    (let [join (fn [game]
                 (if-let [seat (first (free-seats game))]
                   (assoc-in game [::users uid]
                             {::uid uid
                              ::send-fn send-fn
                              ::name name
                              ::seat seat})
                   game))
          game (swap! game-atom join)]
      (when (some? (get-in @game-atom [::users uid]))
        (timbre/info "User" uid "joined game" (::code game))
        (broadcast-game! game)
        (client-game game uid)))))
