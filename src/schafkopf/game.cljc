;; IDEA - split this into per-entity namespaces?
;; schafkopf.deck
;; schafkopf.game
;; schafkopf.player
(ns schafkopf.game
  "Minimalist Schafkopf game implementation."
  (:require [clojure.spec.alpha :as s]))

(def suit? #{:acorns :leaves :hearts :bells})
(def rank? (into #{:deuce :king :ober :unter} (range 7 11)))

(s/def :game/card (s/tuple rank? suit?))
(s/def :game/deck (s/coll-of :game/card :count 32 :distinct true))

(s/def :player/hand (s/coll-of :game/card :max-count 8 :distinct true :into #{}))

;; XXX do we need to record who played the first card?
(s/def :player/trick (s/coll-of :game/card :kind vector? :count 4 :distinct true))

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

(s/def :player/points nat-int?)
(s/def :player/score int?)
(s/def :player/total :player/score)
(s/def :game/pot :player/score)

(s/def :game/player-public
  (s/keys :req [:player/total]
          :opt [:player/points
                :player/score]))

(s/def :game/player
  (s/merge :game/player-public
           (s/keys :req [:player/hand :player/tricks])))

(s/def :game/players (s/coll-of :game/player :kind vector? :count 4))

(s/def :game/prev-trick :player/trick)

(s/def :schafkopf/game-public
  (s/keys :req [:game/number]
          :opt [:game/dealer-seat
                :game/active-seat
                :game/active-trick
                :game/prev-trick
                :game/pot]))

(s/def :schafkopf/game
  (s/merge :schafkopf/game-public
           (s/keys :req [:game/players])))

(s/def :player/peer
  (s/merge :game/player-public
           (s/keys :req [:player/hand-count :player/trick-count])))

(s/def :player/self #{:player/self})
(s/def :player/slot (s/or :peer :player/peer
                          :self :player/self))

(s/def :player/peers (s/coll-of :player/peer :kind vector? :count 4))

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

;;;; Logic

(defn started? [game]
  (some? (:game/dealer-seat game)))

(defn tricks-taken [game]
  (->> (:game/players game)
       (map (comp count :player/tricks))
       (reduce +)))

(defn all-taken? [game]
  (= 8 (tricks-taken game)))

(defn scored? [game]
  (->> (:game/players game)
       (some :player/score)
       boolean))

(s/fdef game
  :ret :schafkopf/game)

(defn game
  "Creates a new game in initial configuration."
  []
  {:game/number 0
   :game/players (->> {:player/total 0
                       :player/hand #{}
                       :player/tricks []}
                       (repeat 4)
                       vec)})

(s/fdef player-game
  :args (s/cat :game :schafkopf/game :seat :player/seat)
  :ret :player/game)

(defn player-peer [{:player/keys [total hand tricks points score]}
                   scored?]
  (cond-> #:player{:total total
                   :hand-count (count hand)
                   :trick-count (count tricks)}
    (some? points) (assoc :player/points points)
    (some? score) (assoc :player/score score)
    scored? (assoc :player/tricks tricks)))

(defn player-game
  [{:game/keys [players] :as game} seat]
  (when-let [player (get players seat)]
    (let [scored? (scored? game)

          ;; When we have points we can see our own tricks.
          player-keys (cond-> [:player/hand]
                        (some? (:player/points player-game))
                        (conj :player/tricks))]

      (merge
       (select-keys game [:game/number
                          :game/dealer-seat
                          :game/active-seat
                          :game/active-trick
                          :game/pot])
       (select-keys player player-keys)
       ;; TODO: Swap our entry in the peers vector with :player/self,
       ;; can merge the precomputed peer data here.
       (player-peer player scored?)
       {:player/seat seat
        :player/peers (mapv #(player-peer % scored?) players)}))))

;; TODO: Spec alternatives?
;; TODO: Name player-publics?
(s/fdef players
  :args (s/cat :game (s/alt :game :schafkopf/game-public
                            :player-game :player/game))
  :ret (s/coll-of :game/player-public))

(defn players
  "Takes a game or player-game and returns players or peers."
  [game]
  (or (:game/players game) (:player/peers game)))

(defn hand-count [player]
  (or (:player/hand-count player)
      (count (:player/hand player))))

(defn trick-count [player]
  (or (:player/trick-count player)
      (count (:player/trick player))))

(defn round
  "Returns the current round in the game."
  [game]
  (quot (:game/number game) 4))

(defn round-games-left
  "Returns how many games are left in this round."
  [game]
  (rem (:game/number game) 4))

(defn rand-seat []
  (rand-int 4))

(defn next-seat [seat]
  (rem (inc seat) 4))

;; TODO: Have a shared (reset)?
(defn start
  ([game] (start game 0))
  ([game dealer-seat]
   {:pre [(not (started? game))]}
   (assoc game
          :game/dealer-seat dealer-seat
          :game/active-trick [])))

(s/fdef shuffled-deck
  :ret :game/deck)

(defn shuffled-deck []
  (shuffle deck))

(defn partition-packets
  "Deals the deck to n players, in packets of size k, starting at start."
  ([deck start] (partition-packets deck 4 4 start))
  ([deck n k start]
   {:pre [(zero? (rem (count deck) (* n k)))
          (<= 0 start (dec n))]}
   (let [packets (partition k deck)]
     (for [i (range n)]
       (->> packets
            (drop (mod (- i start) n))
            (take-nth n)
            (apply concat))))))

(s/fdef deal
  :args (s/cat :game :schafkopf/game :deck (s/? :game/deck))
  :ret :schafkopf/game)

(defn deal
  "Deals cards from a the given deck to the players."
  ([game] (deal game (shuffled-deck)))
  ([game deck]
   {:pre [(started? game)]}
   (let [forehand (next-seat (:game/dealer-seat game))
         players (mapv (fn [player hand]
                         (assoc player :player/hand (set hand)))
                       (:game/players game)
                       (partition-packets deck forehand))]
     (assoc game
            :game/active-seat forehand
            :game/players players))))

(defn trick-complete?
  [game]
  (= (count (:game/active-trick game)) 4))

(defn player-turn?
  [game seat]
  (= seat (:game/active-seat game)))

(defn has-card?
  [game seat card]
  (-> (get-in game [:game/players seat :player/hand])
      (contains? card)))

(s/fdef play-card
  :args (s/cat :game :schafkopf/game
               :card :game/card)
  :ret :schafkopf/game)

(defn play-card
  "Updates the game when the current player plays a card from their hand."
  [game card]
  {:pre [(not (trick-complete? game))
         (has-card? game (:game/active-seat game) card)]}
  (let [seat (:game/active-seat game)
        game (-> game
                 (update :game/active-trick conj card)
                 (update-in [:game/players seat :player/hand] disj card))]
    (if (trick-complete? game)
      (dissoc game :game/active-seat)
      (update game :game/active-seat next-seat))))

(s/fdef take-trick
  :args (s/cat :game :schafkopf/game
               :seat :player/seat)
  :ret :schafkopf/game)

(defn take-trick
  "Updates the game when the given player takes the current trick."
  [game seat]
  {:pre [(trick-complete? game)
         (nil? (:game/active-seat game))]}
  (let [trick (:game/active-trick game)]
    (-> game
        (assoc :game/active-trick []
               :game/prev-trick trick
               :game/active-seat seat)
        (update-in [:game/players seat :player/tricks] conj trick))))

(defn count-points
  "Takes any nested structure of ranks, e.g. a collection of cards,
   and sums up the points."
  [nested-ranks]
  (->> nested-ranks flatten (keep points) (reduce +)))

(defn- update-points [player]
  (->> (:player/tricks player)
       count-points
       (assoc player :player/points)))

(defn summarize
  "Once all tricks have been taken, counts all players' points."
  [game]
  {:pre [(all-taken? game)]}
  (update game :game/players #(mapv update-points %)))

(defn score [game scores])

(defn next-game [game])