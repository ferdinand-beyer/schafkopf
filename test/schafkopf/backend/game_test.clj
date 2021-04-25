(ns schafkopf.backend.game-test
  (:require [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is testing]]
            [expectations.clojure.test :refer [expect]]
            [schafkopf.backend.game :as game]
            [schafkopf.spec :as sk]))

(deftest test-newly-created-game
  (testing "A new game"
    (let [game (game/game)]
      (expect ::sk/game game)
      (is (= 0 (::sk/game-number game))))))

(deftest test-deal
  (let [game (game/deal (game/game) game/deck)]
    (expect ::sk/game game)
    (is (= (inc (::sk/dealer game)) (::sk/turn game)))
    (testing "Every player has 8 cards"
      (doseq [player (::sk/players game)]
        (is (= 8 (count (::sk/hand player))))))
    (is (= (set game/deck)
           (into #{} (mapcat ::sk/hand (::sk/players game))))
        "Players hold a full deck")
    (is (subset? (set (take 4 game/deck))
                 (get-in game [::sk/players (::sk/turn game) ::sk/hand]))
        "Forehand player has the first 4 cards")))

(deftest test-play-cards
  (let [deck game/deck
        card (first deck)
        game (-> (game/game) (game/deal deck))
        hand-before (get-in game [::sk/players 1 ::sk/hand])
        game (game/play-card game card)]
    (expect ::sk/game game)
    (is (= [card] (::sk/current-trick game)))
    (is (= 2 (::sk/turn game)))
    (is (= (disj hand-before card)
           (get-in game [::sk/players 1 ::sk/hand]))
        "Played card is no longer in the player's hand")))