(ns schafkopf.backend.control
  (:require [clojure.set :as set]
            [taoensso.timbre :as timbre]))

(def game-atom (atom nil))

(defn generate-game-code []
  (format "%06d" (rand-int 1000000)))

(defn generate-uid []
  (.toString (java.util.UUID/randomUUID)))

(defn new-game []
  {:code (generate-game-code)
   :clients {}
   :state {}})

(defn ensure-game!
  []
  (swap! game-atom #(or % (new-game)))
  game-atom)

(defn find-game [code]
  (when (= code (:code @game-atom))
    game-atom))

(defn client-view [game uid]
  (select-keys game [:code]))

(defn free-seats [game]
  (set/difference (set (range 4)) (set (map :seat (:clients game)))))

(defn broadcast-event! [game event]
  (doseq [[uid {:keys [send-fn]}] (:clients game)]
    (when send-fn
      (send-fn [event (client-view game uid)]))))

(defn join-game
  "Makes a user join a game."
  [game-atom uid name send-fn]
  (if (some? (get-in @game-atom [:clients uid]))
    (client-view @game-atom uid)
    (let [join (fn [game]
                 (if-let [seat (first (free-seats game))]
                   (assoc-in game [:clients uid]
                             {:uid uid
                              :name name
                              :send-fn send-fn
                              :seat seat})
                   game))
          game (swap! game-atom join)]
      (when (some? (get-in game [:clients uid]))
        (timbre/info "User" uid "joined game" (:code game))
        (broadcast-event! game :player/joined)
        (client-view game uid)))))
