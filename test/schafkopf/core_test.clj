(ns schafkopf.core-test
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :refer [for-all]]
            [expectations.clojure.test :refer [expect]]
            [schafkopf.core :as sk]))


(deftest test-newly-created-game
  (testing "A new game"
    (let [game (sk/game)]
      (expect ::sk/game game)
      (is (= 0 (::sk/game-number game))))))

(deftest test-deal
  (let [game (sk/deal (sk/game) sk/deck)]
    (expect ::sk/game game)
    (is (= (inc (::sk/dealer game)) (::sk/turn game)))
    (testing "Every player has 8 cards"
      (doseq [player (::sk/players game)]
        (is (= 8 (count (::sk/hand player))))))
    (is (= (set sk/deck)
           (into #{} (mapcat ::sk/hand (::sk/players game))))
        "Players hold a full deck")
    (is (subset? (set (take 4 sk/deck))
                 (get-in game [::sk/players (::sk/turn game) ::sk/hand]))
        "Forehand player has the first 4 cards")))

(deftest test-play-cards
  (let [deck sk/deck
        card (first deck)
        game (-> (sk/game) (sk/deal deck))
        hand-before (get-in game [::sk/players 1 ::sk/hand])
        game (sk/play-card game card)]
    (expect ::sk/game game)
    (is (= [card] (::sk/current-trick game)))
    (is (= 2 (::sk/turn game)))
    (is (= (disj hand-before card)
           (get-in game [::sk/players 1 ::sk/hand]))
        "Played card is no longer in the player's hand")))