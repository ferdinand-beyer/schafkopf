(ns schafkopf.game-test
  (:require [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is are testing]]
            [expectations.clojure.test :refer [expect]]
            [schafkopf.game :as g])
  (:import [java.lang AssertionError]))

(defn prepare-game []
  (-> (g/game) (g/start) (g/deal g/deck)))

(defn play-card [game]
  (let [seat (:game/active-seat game)
        card (first (get-in game [:game/players seat :player/hand]))]
    (g/play-card game card)))

(defn play-trick [game]
  (nth (iterate play-card game) 4))

(defn take-trick [game]
  (let [n (count (get-in game [:game/players 0 :game/hand]))
        seat (rem n 4)]
    (g/take-trick game seat)))

(defn play-game [game]
  (nth (iterate (comp take-trick play-trick) game) 8))

(defn make-score [p0 p1 p2 p3 pot]
  #:game.score {:players [p0 p1 p2 p3]
                :pot pot})

(defn unscore
  "Removes scores for testing purposes."
  [game]
  (-> game
      (dissoc :game/pot-score)
      (update :game/players (partial mapv #(dissoc % :player/score)))))

;;;; Tests

(deftest test-new-game
  (let [game (g/game)]
    (expect :schafkopf/game game)
    (is (= 0 (:game/number game)))
    (is (not (g/started? game)))
    (is (= 0 (:game/pot game)))))

(deftest test-deal
  (let [game (prepare-game)]
    
    (expect :schafkopf/game game)
    (is (= (inc (:game/dealer-seat game))
           (:game/active-seat game)))
    (is (= (:game/active-seat game)
           (:game/lead-seat game)))
    
    (testing "Every player has 8 cards"
      (doseq [player (:game/players game)]
        (is (= 8 (count (:player/hand player))))))
    
    (is (= (set g/deck)
           (into #{} (mapcat :player/hand (:game/players game))))
        "Players hold a full deck")
    
    (is (subset? (set (take 4 g/deck))
                 (get-in game [:game/players
                               (:game/active-seat game)
                               :player/hand]))
        "Forehand player has the first 4 cards")))

(deftest test-play
  (let [game (prepare-game)]

    (let [hand-before (get-in game [:game/players 1 :player/hand])
          card (first hand-before)
          game (g/play-card game card)]
      
      (expect :schafkopf/game game)
      (is (= [card] (:game/active-trick game)))
      (is (= 2 (:game/active-seat game)))
      (is (= (disj hand-before card)
             (get-in game [:game/players 1 :player/hand]))
          "Played card is no longer in the player's hand"))

    (testing "clears active seat when trick is complete"
      (let [game (play-trick game)]

        (expect :schafkopf/game game)
        (is (true? (g/trick-complete? game)))
        (is (not (contains? game :game/active-seat)))
        (is (= 1 (:game/lead-seat game)))))))

(deftest test-take
  (let [game (-> (prepare-game) (play-trick))
        taker 2
        trick (:game/active-trick game)
        game (g/take-trick game taker)]

    (expect :schafkopf/game game)
    (is (= [] (:game/active-trick game)))
    (is (= [trick] (get-in game [:game/players taker :player/tricks])))
    (is (= trick (:game/prev-trick game)))

    ;; TODO Expect no active seat when the last trick has been taken!
    (is (= taker (:game/lead-seat game)))
    (is (= taker (:game/active-seat game)))))

(deftest test-summarize
  (let [game (-> (prepare-game) (play-game) (g/summarize))
        players (:game/players game)]

    (expect :schafkopf/game game)
    (is (every? (comp some? :player/points) players))
    (is (= 120 (reduce + (map :player/points players))))

    (is (not (contains? game :game/lead-seat)))
    (is (not (contains? game :game/active-seat)))
    (is (not (contains? game :game/active-trick)))
    (is (not (contains? game :game/prev-trick)))))

(deftest test-valid-score
  (are [scores expected] (= expected (g/valid-score? (apply make-score scores)))
    [1 2 3 4 5] false
    [-1 -2 -3 -4 -5] false
    [10 10 -10 -10 0] true
    [-10 -10 -10 -10 40] true
    [0 0 0 0 0] true))

(deftest test-score
  (let [game (-> (prepare-game) (play-game) (g/summarize))]

    (expect :schafkopf/game (g/score game (make-score 10 -10 10 -10 0)))

    (testing "rejects invalid scores"
      (is (thrown? AssertionError (g/score game (make-score 10 10 10 10 0))))
      (is (thrown? AssertionError (g/score game (make-score 10 -10 10 -10 10)))))

    (testing "rejects negative pot balance"
      (is (thrown? AssertionError (g/score game (make-score 10 10 10 10 -40)))))

    (testing "associates scores correctly"
      (let [game (g/score game (make-score 10 10 -10 -10 0))]
        (is (= [10 10 -10 -10] (mapv :player/score (:game/players game))))
        (is (zero? (:game/pot-score game))))

      (let [game (g/score game (make-score -10 -10 -10 -10 40))]
        (is (= [-10 -10 -10 -10] (mapv :player/score (:game/players game))))
        (is (= 40 (:game/pot-score game)))))
    
    (testing "allows zero scores"
      (let [game (g/score game (make-score 0 0 0 0 0))]
        (is (= [0 0 0 0] (mapv :player/score (:game/players game))))
        (is (= 0 (:game/pot-score game)))))

    (testing "adds scores to balances"
      (let [game1 (g/score game (make-score -50 -50 50 50 0))
            game2 (g/score (unscore game1) (make-score -10 -10 -10 -10 40))
            game3 (g/score (unscore game2) (make-score 60 -20 20 -20 -40))]

        (is (= [-50 -50 50 50] (mapv :player/balance (:game/players game1))))
        (is (= 0 (:game/pot game1)))

        (is (= [-60 -60 40 40] (mapv :player/balance (:game/players game2))))
        (is (= 40 (:game/pot game2)))

        (is (= [0 -80 60 20] (mapv :player/balance (:game/players game3))))
        (is (= 0 (:game/pot game3)))))))

(deftest test-start-next
  (let [game (-> (prepare-game) (play-game) (g/summarize))]
    
    (is (thrown? AssertionError (g/start-next game)))

    (let [game (g/score game (make-score 10 -10 10 -10 0))
          next-game (g/start-next game)]

      (expect :schafkopf/game next-game)

      (is (= 1 (:game/number next-game)))
      (is (= 1 (:game/dealer-seat next-game)))
      (is (= [] (:game/active-trick next-game)))
      (is (not (contains? next-game :game/pot-score)))
      (is (not (contains? next-game :game/lead-seat)))
      (is (not (contains? next-game :game/active-seat)))
      (is (not (contains? next-game :game/prev-trick)))

      (doseq [player (:game/players next-game)]
        (is (empty? (:player/hand player)))
        (is (= [] (:player/tricks player)))
        (is (not (contains? player :player/points)))
        (is (not (contains? player :player/score)))))))

(deftest test-skip
  (doseq [game
          [(-> (prepare-game) (g/skip))
           (-> (prepare-game) (play-trick) (g/skip))]]

    (expect :schafkopf/game game)

    (let [players (:game/players game)]
      (is (every? (comp #(= 0 %) :player/points) players))
      (is (every? (comp empty? :player/hand) players))
      (is (every? (comp empty? :player/tricks) players)))

    (is (not (contains? game :game/lead-seat)))
    (is (not (contains? game :game/active-seat)))
    (is (not (contains? game :game/active-trick)))
    (is (not (contains? game :game/prev-trick)))

    (testing "Score the skipped game"
      (let [game (g/score game (make-score -10 -10 -10 -10 40))]
        (expect :schafkopf/game game)
        (is (= 40 (:game/pot game)))))))
