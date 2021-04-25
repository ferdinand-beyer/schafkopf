(ns schafkopf.backend.control
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as timbre]
            [schafkopf.game :as game]
            [schafkopf.spec :as spec]))

(s/def ::uid string?)

(s/def ::client (s/keys :req-un [::uid]))

(def game-atom (atom nil))

(defn generate-code []
  (format "%04d" (rand-int 10000)))

(defn generate-uid []
  (.toString (java.util.UUID/randomUUID)))

(defn valid-name? [name]
  (s/valid? :player/name name))

(defn new-game []
  (assoc (game/game)
         ::code (generate-code)
         ::users {}))

(defn ensure-game!
  "Creates a game unless it exists already, then returns the
   game singleton."
  []
  (swap! game-atom #(or % (new-game)))
  game-atom)

(defn find-game
  "Finds a game by its code, nil otherwise."
  [code]
  (when (= code (::code @game-atom))
    game-atom))

(defn user-game
  "Returns the view of the game for a user identified by their uid."
  [game uid]
  ;; TODO: Merge names
  (when-let [seat (get-in game [::users uid ::seat])]
    (->
     (game/player-game game seat)
     (assoc :session/code (::code game)))))

(defn free-seats
  "Returns the set of free seats in a game."
  [game]
  (set/difference (set (range 4)) (set (map ::seat (::users game)))))

(defn broadcast-event!
  "Sends an event to all connected users of a game."
  [game event]
  (doseq [[uid {::keys [send-fn]}] (::users game)]
    (when send-fn
      (send-fn [event (user-game game uid)]))))

(defn join-game!
  "Makes a user join a game.  If they are already playing, does nothing.
   Returns the user-game of the joined player, or nil when there are no
   more free seats in the game."
  [game-atom uid name send-fn]
  (if (some? (get-in @game-atom [::users uid]))
    (user-game @game-atom uid)
    (let [join (fn [game]
                 (if-let [seat (first (free-seats game))]
                   (assoc-in game [::users uid]
                             {::uid uid
                              ::name name
                              ::send-fn send-fn
                              ::seat seat})
                   game))
          game (swap! game-atom join)]
      (when (some? (get-in game [::users uid]))
        (timbre/info "User" uid "joined game" (::code game))
        (broadcast-event! game :player/joined)
        (user-game game uid)))))
