(ns schafkopf.core
  (:require [clojure.spec.alpha :as s]))

(def suit? #{:acorns :leaves :hearts :bells})
(def rank? (into #{:deuce :king :ober :unter} (range 7 11)))

(def deck (for [suit suit? rank rank?] [rank suit]))
(def points {:deuce 11, 10 10, :king 4, :ober 3, :unter 2})

(s/def ::card (s/tuple rank? suit?))
(s/def ::deck (s/coll-of ::card :count 32 :distinct true))
(s/def ::hand (s/coll-of ::card :max-count 8 :distinct true :into #{}))
(s/def ::trick (s/coll-of ::card ::kind vector? :count 4 :distinct true))
(s/def ::prev-trick ::trick)
(s/def ::current-trick (s/coll-of ::card ::kind vector? :max-count 4 :distinct true))

;; A player's won/taken tricks.
(s/def ::tricks (s/coll-of ::trick :max-count 8))

(s/def ::player-index (s/and int? #(<= 0 % 3)))
(s/def ::trick-count (s/and int? #(<= 0 % 8)))
(s/def ::hand-count ::trick-count)

(s/def ::game-number nat-int?)
(s/def ::dealer ::player-index)
(s/def ::turn ::player-index)
(s/def ::pot int?)

(s/def ::name string?)
(s/def ::score int?)
(s/def ::points int?)

(s/def ::player-public
  (s/keys :req [::score]
          :opt [::name]))

(s/def ::player
  (s/merge ::player-public
           (s/keys :req [::hand ::tricks]
                   :opt [::points])))

(s/def ::players (s/coll-of ::player :kind vector? :count 4))

(s/def ::game-public
  (s/keys :req [::game-number ::dealer ::turn ::current-trick]
          :opt [::prev-trick ::pot]))

(s/def ::game
  (s/merge ::game-public
           (s/keys :req [::players])))

(s/def ::peer
  (s/merge ::player-public
           (s/keys :req [::hand-count ::trick-count])))
(s/def ::peers (s/coll-of ::peer :kind vector? :count 3))

;; The game from a player's point of view.  A player can only see their
;; own hand, and maybe the tricks they took at the end of a game.
(s/def ::player-view
  (s/merge ::game-public
           ::player-public
           (s/keys :req [::hand ::trick-count ::peers]
                   :opt [::tricks])))

(s/fdef game
  :ret ::game)

(defn game
  "Creates a new game in initial configuration."
  []
  {::game-number 0
   ::dealer 0
   ::turn 0
   ::current-trick []
   ::players (->> {::score 0 ::hand #{} ::tricks []} (repeat 4) vec)})

(s/fdef player-view
  :args (s/cat :game ::game :idx ::player-index)
  :ret ::player-view)

(defn player-view [game idx]
  (when-let [player (get-in game [::players idx])]
    (merge (select-keys game [::game-number ::dealer ::turn ::current-trick ::pot])
           (select-keys player [::score ::name]))))

(defn next-player [idx]
  (rem (inc idx) 4))

(s/fdef shuffled-deck
  :ret ::deck)

(defn shuffled-deck []
  (shuffle deck))

(s/fdef deal
  :args (s/cat :game ::game :deck (s/? ::deck))
  :ret ::game)

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
   (let [forehand (next-player (::dealer game))
         players (mapv (fn [player hand]
                         (assoc player ::hand (set hand)))
                       (::players game)
                       (partition-packets deck 4 4 forehand))]
     (assoc game ::turn forehand ::players players))))

(s/fdef play-card
  :args (s/cat :game ::game
               :card ::card)
  :ret ::game)

(defn play-card
  "Updates the game when the current player plays a card from their hand."
  [game card]
  {:pre [(contains? (get-in game [::players (::turn game) ::hand]) card)]}
  (let [player (::turn game)]
    (-> game
        (update ::current-trick conj card)
        (update-in [::players player ::hand] disj card)
        (update ::turn next-player))))

(s/fdef take-trick
  :args (s/cat :game ::game
               :player ::player-index)
  :ret ::game)

(defn take-trick
  "Updates the game when the given player takes the current trick."
  [game player]
  {:pre [(= 4 (count (::current-trick game)))]}
  ;; TODO
  game)

(defn count-points [cards]
  (->> cards flatten (keep points) (reduce +)))
