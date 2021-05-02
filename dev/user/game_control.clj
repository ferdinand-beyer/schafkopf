(ns user.game-control
  (:require [clojure.string :as str]
            [schafkopf.game :as g]
            [schafkopf.backend.game :as sg]))

;;;; Game info

(defn game-atom []
  sg/game-atom)

(defn server-game []
  @(game-atom))

(defn game []
  (sg/game (server-game)))

(defn client-game [client-id]
  (sg/client-game (server-game) client-id))

(defn seqno []
  (::sg/seqno (server-game)))

(defn on-seat [seat]
  (->> (server-game)
       ::sg/clients
       vals
       (filter #(= seat (::sg/seat %)))
       first))

(defn active-client []
  (-> (server-game) (sg/game) (:game/active-seat) (on-seat)))

(defn active-client-id []
  (::sg/client-id (active-client)))

(defn rand-client-id []
  (rand-nth (map key (::sg/clients (server-game)))))

(defn expand-fake-client-id [client-id]
  (if (int? client-id)
    (str "fake-" client-id)
    client-id))

(defn rand-fake-client-id []
  (expand-fake-client-id (rand-int 3)))

(defmacro while-max
  [max test & body]
  `(loop [i# 0]
     (when (and (< i# ~max) ~test)
       ~@body
       (recur (inc i#)))))

;;;; Game simulation

(defn receive-event [_])

(defn host!
  []
  (sg/host! "fake-host" "Fake Host" receive-event))

(defn join!
  "Fill the current game with fake clients."
  ([] (join! 3))
  ([n]
   (let [game (game-atom)]
     (when (some? @game)
       (doseq [i (range n)]
         (sg/join! game
                   (str "fake-" i)
                   (str "Fake " i)
                   receive-event))))))

(defn start! []
  (sg/start! (game-atom) (rand-client-id) (seqno)))

(defn play!
  ([] (play! (active-client-id)))
  ([client-id]
   (let [card (first (:player/hand (client-game client-id)))]
     (play! client-id card)))
  ([client-id card]
   (sg/play! (game-atom) (expand-fake-client-id client-id) (seqno) card)))

(defn play-fakes! []
  (let [client-id (active-client-id)]
    (while-max 3
               (str/starts-with? client-id "fake-")
               (play!))))

(defn take!
  ([] (take! (rand-client-id)))
  ([client-id]
   (sg/take! (game-atom) (expand-fake-client-id client-id) (seqno))))

(defn play-trick! []
  (while-max 4
             (not (g/trick-complete? (game)))
             (play!)))

(defn play-game! []
  (while-max 8
             (not (g/all-taken? (game)))
             (play-trick!)
             (take!)))

(defn undo! []
  (sg/undo! (game-atom) (rand-client-id) (seqno)))
