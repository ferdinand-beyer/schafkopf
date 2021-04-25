(ns schafkopf.game-test
  (:require [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is testing]]
            [expectations.clojure.test :refer [expect]]
            [schafkopf.game :as sk]))

(deftest test-newly-created-game
  (testing "A new game"
    (let [game (sk/game)]
      (expect :schafkopf/game game)
      (is (= 0 (:game/number game))))))

(deftest test-deal
  (let [game (sk/deal (sk/game) sk/deck)]
    (expect :schafkopf/game game)
    (is (= (inc (:game/dealer-seat game))
           (:game/active-seat game)))
    (testing "Every player has 8 cards"
      (doseq [player (:game/players game)]
        (is (= 8 (count (:player/hand player))))))
    (is (= (set sk/deck)
           (into #{} (mapcat :player/hand (:game/players game))))
        "Players hold a full deck")
    (is (subset? (set (take 4 sk/deck))
                 (get-in game [:game/players
                               (:game/active-seat game)
                               :player/hand]))
        "Forehand player has the first 4 cards")))

(deftest test-play-cards
  (let [deck sk/deck
        card (first deck)
        game (-> (sk/game) (sk/deal deck))
        hand-before (get-in game [:game/players 1 :player/hand])
        game (sk/play-card game card)]
    (expect :schafkopf/game game)
    (is (= [card] (:game/active-trick game)))
    (is (= 2 (:game/active-seat game)))
    (is (= (disj hand-before card)
           (get-in game [:game/players 1 :player/hand]))
        "Played card is no longer in the player's hand")))