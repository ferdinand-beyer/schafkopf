(ns user.game-control
  (:require [clojure.string :as str]
            [schafkopf.backend.control :as sg]
            [schafkopf.game :as g]))

;;;; Game info

(defn server-game []
  @sg/game-atom)

(defn game []
  (::sg/game (server-game)))

(defn client-game [uid]
  (sg/client-game (server-game) uid))

(defn seqno []
  (::sg/seqno (server-game)))

(defn on-seat [seat]
  (->> (server-game)
       ::sg/clients
       vals
       (filter #(= seat (::sg/seat %)))
       first))

(defn active-client []
  (on-seat (get-in (server-game) [::sg/game :game/active-seat])))

(defn active-uid []
  (::sg/uid (active-client)))

(defn rand-uid []
  (rand-nth (map key (::sg/clients (server-game)))))

(defn expand-fake-uid [uid]
  (if (int? uid)
    (str "fake-" uid)
    uid))

(defn rand-fake-uid []
  (expand-fake-uid (rand-int 3)))

(defmacro while-max
  [max test & body]
  `(loop [i# 0]
     (when (and (< i# ~max) ~test)
       ~@body
       (recur (inc i#)))))

;;;; Game simulation

(defn join!
  "Fill the current game with fake clients."
  []
  (let [game (sg/ensure-game!)]
    (doseq [i (range 3)]
      (sg/join! game
                (str "fake-" i)
                (str "Fake " i)
                (constantly nil)))))

(defn start! []
  (sg/start! sg/game-atom (rand-uid) (seqno)))

(defn play!
  ([] (play! (active-uid)))
  ([uid]
   (let [card (first (:player/hand (client-game uid)))]
     (play! uid card)))
  ([uid card]
   (sg/play! sg/game-atom (expand-fake-uid uid) (seqno) card)))

(defn play-fakes! []
  (let [uid (active-uid)]
    (while-max 3
               (str/starts-with? uid "fake-")
               (play!))))

(defn take!
  ([] (take! (rand-uid)))
  ([uid]
   (sg/take! sg/game-atom (expand-fake-uid uid) (seqno))))

(defn play-trick! []
  (while-max 4
             (not (g/trick-complete? (game)))
             (play!)))

(defn play-game! []
  (while-max 8
             (not (g/all-taken? (game)))
             (play-trick!)
             (take!)))
