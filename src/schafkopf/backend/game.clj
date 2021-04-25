(ns schafkopf.backend.game
  (:require [clojure.spec.alpha :as s]
            [schafkopf.spec :as spec]))

(def deck (for [suit spec/suit? rank spec/rank?] [rank suit]))
(def points {:deuce 11, 10 10, :king 4, :ober 3, :unter 2})

(s/fdef game
  :ret ::spec/game)

(defn game
  "Creates a new game in initial configuration."
  []
  {::spec/game-number 0
   ::spec/dealer 0
   ::spec/turn 0
   ::spec/current-trick []
   ::spec/players (->> {::spec/score 0 ::spec/hand #{} ::spec/tricks []} (repeat 4) vec)})

(s/fdef player-view
  :args (s/cat :game ::spec/game :idx ::spec/player-index)
  :ret ::spec/player-view)

(defn player-view [game idx]
  (when-let [player (get-in game [::spec/players idx])]
    (merge (select-keys game [::spec/game-number ::spec/dealer ::spec/turn ::spec/current-trick ::spec/pot])
           (select-keys player [::spec/score ::spec/name]))))

(defn next-player [idx]
  (rem (inc idx) 4))

(s/fdef shuffled-deck
  :ret ::spec/deck)

(defn shuffled-deck []
  (shuffle deck))

(s/fdef deal
  :args (s/cat :game ::spec/game :deck (s/? ::spec/deck))
  :ret ::spec/game)

(defn partition-packets
  "Deals the deck to n players, in packets of size k, starting at position start."
  [deck n k start]
  {:pre [(zero? (rem (count deck) (* n k)))
         (<= 0 start (dec n))]}
  (let [packets (partition k deck)]
    (for [i (range n)]
      (->> packets
           (drop (mod (- i start) n))
           (take-nth n)
           (apply concat)))))

(defn deal
  "Deals cards from a the given deck to the players."
  ([game] (deal game (shuffled-deck)))
  ([game deck]
   (let [forehand (next-player (::spec/dealer game))
         players (mapv (fn [player hand]
                         (assoc player ::spec/hand (set hand)))
                       (::spec/players game)
                       (partition-packets deck 4 4 forehand))]
     (assoc game ::spec/turn forehand ::spec/players players))))

(s/fdef play-card
  :args (s/cat :game ::spec/game
               :card ::spec/card)
  :ret ::spec/game)

(defn play-card
  "Updates the game when the current player plays a card from their hand."
  [game card]
  {:pre [(contains? (get-in game [::spec/players (::spec/turn game) ::spec/hand]) card)]}
  (let [player (::spec/turn game)]
    (-> game
        (update ::spec/current-trick conj card)
        (update-in [::spec/players player ::spec/hand] disj card)
        (update ::spec/turn next-player))))

(s/fdef take-trick
  :args (s/cat :game ::spec/game
               :player ::spec/player-index)
  :ret ::spec/game)

(defn take-trick
  "Updates the game when the given player takes the current trick."
  [game player]
  {:pre [(= 4 (count (::spec/current-trick game)))]}
  ;; TODO
  game)

(defn count-points [cards]
  (->> cards flatten (keep points) (reduce +)))