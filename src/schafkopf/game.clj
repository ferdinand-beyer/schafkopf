;; IDEA - split this into per-entity namespaces?
;; schafkopf.deck
;; schafkopf.game
;; schafkopf.player
(ns schafkopf.game
  (:require [clojure.spec.alpha :as s]))

(def suit? #{:acorns :leaves :hearts :bells})
(def rank? (into #{:deuce :king :ober :unter} (range 7 11)))

(s/def :game/card (s/tuple rank? suit?))
(s/def :game/deck (s/coll-of :game/card :count 32 :distinct true))

(s/def :player/hand (s/coll-of :game/card :max-count 8 :distinct true :into #{}))
(s/def :player/trick (s/coll-of :game/card ::kind vector? :count 4 :distinct true))

;; Uncomplete trick
(s/def :game/active-trick (s/coll-of :game/card :kind vector? :max-count 4 :distinct true))

;; A player's won/taken tricks.
(s/def :player/tricks (s/coll-of :player/trick :max-count 8))

(s/def :player/seat (s/and int? #(<= 0 % 3)))
(s/def :player/trick-count (s/and int? #(<= 0 % 8)))
(s/def :player/hand-count :player/trick-count)

(s/def :game/number nat-int?)
(s/def :game/dealer-seat :player/seat)
(s/def :game/active-seat :player/seat)

(s/def :player/score int?)
(s/def :player/points nat-int?)
(s/def :game/pot :player/score)

(s/def :game/player-public
  (s/keys :req [:player/score]))

(s/def :game/player
  (s/merge :game/player-public
           (s/keys :req [:player/hand :player/tricks]
                   :opt [:player/points])))

(s/def :game/players (s/coll-of :game/player :kind vector? :count 4))

(s/def :game/prev-trick :player/trick)

(s/def :schafkopf/game-public
  (s/keys :req [:game/number
                :game/dealer-seat
                :game/active-seat
                :game/active-trick]
          :opt [:game/prev-trick
                :game/pot]))

(s/def :schafkopf/game
  (s/merge :schafkopf/game-public
           (s/keys :req [:game/players])))

(s/def :player/peer
  (s/merge :game/player-public
           (s/keys :req [:player/hand-count :player/trick-count])))
(s/def :player/peers (s/coll-of :player/peer :kind vector? :count 3))

(s/def :player/game
  (s/merge :schafkopf/game-public
           :game/player-public
           (s/keys :req [:player/seat
                         :player/hand
                         :player/trick-count
                         :player/peers]
                   :opt [:player/tricks])))

(def deck (for [suit suit? rank rank?] [rank suit]))
(def points {:deuce 11, 10 10, :king 4, :ober 3, :unter 2})

(s/fdef game
  :ret :schafkopf/game)

(defn game
  "Creates a new game in initial configuration."
  []
  {:game/number 0
   :game/dealer-seat 0
   :game/active-seat 0
   :game/active-trick []
   :game/players (->> {:player/score 0
                       :player/hand #{}
                       :player/tricks []}
                       (repeat 4)
                       vec)})

(s/fdef player-game
  :args (s/cat :game :schafkopf/game :seat :player/seat)
  :ret :player/game)

(defn player-peer [{::keys [score hand tricks]}]
  (cond-> #:player{:hand-count (count hand)
                   :trick-count (count tricks)}
    (some? score) (assoc :player/score score)))

;; TODO: Reveal tricks when game is over.
(defn player-game
  [{:game/keys [players] :as game} seat]
  (when-let [player (get players seat)]
    (merge (select-keys game [:game/number
                              :game/dealer-seat
                              :game/active-seat
                              :game/active-trick
                              :game/pot])
           (select-keys player [:player/score
                                :player/hand])
           (player-peer player)
           {:player/seat seat
            :player/peers (mapv player-peer players)})))

(defn next-seat [seat]
  (rem (inc seat) 4))

(s/fdef shuffled-deck
  :ret :game/deck)

(defn shuffled-deck []
  (shuffle deck))

(s/fdef deal
  :args (s/cat :game :schafkopf/game :deck (s/? :game/deck))
  :ret :schafkopf/game)

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
   (let [forehand (next-seat (:game/dealer-seat game))
         players (mapv (fn [player hand]
                         (assoc player :player/hand (set hand)))
                       (:game/players game)
                       (partition-packets deck 4 4 forehand))]
     (assoc game
            :game/active-seat forehand
            :game/players players))))

(s/fdef play-card
  :args (s/cat :game :schafkopf/game
               :card :game/card)
  :ret :schafkopf/game)

(defn play-card
  "Updates the game when the current player plays a card from their hand."
  [game card]
  {:pre [(-> (get-in game [:game/players
                           (:game/active-seat game)
                           :player/hand])
             (contains? card))]}
  (let [seat (:game/active-seat game)]
    (-> game
        (update :game/active-trick conj card)
        (update-in [:game/players seat :player/hand] disj card)
        (update :game/active-seat next-seat))))

(s/fdef take-trick
  :args (s/cat :game :schafkopf/game
               :seat :player/seat)
  :ret :schafkopf/game)

(defn take-trick
  "Updates the game when the given player takes the current trick."
  [game seat]
  {:pre [(= 4 (count (:game/active-trick game)))]}
  ;; TODO
  game)

(defn count-points [cards]
  (->> cards flatten (keep points) (reduce +)))