(ns schafkopf.game-test
  (:require [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is testing]]
            [expectations.clojure.test :refer [expect]]
            [schafkopf.game :as game]))

(deftest test-new-game
  (let [game (game/game)]
    (expect :schafkopf/game game)
    (is (= 0 (:game/number game)))))

(deftest test-deal
  (let [game (game/deal (game/start (game/game)) game/deck)]
    
    (expect :schafkopf/game game)
    (is (= (inc (:game/dealer-seat game))
           (:game/active-seat game)))
    
    (testing "Every player has 8 cards"
      (doseq [player (:game/players game)]
        (is (= 8 (count (:player/hand player))))))
    
    (is (= (set game/deck)
           (into #{} (mapcat :player/hand (:game/players game))))
        "Players hold a full deck")
    
    (is (subset? (set (take 4 game/deck))
                 (get-in game [:game/players
                               (:game/active-seat game)
                               :player/hand]))
        "Forehand player has the first 4 cards")))

(defn play-card [game]
  (let [seat (:game/active-seat game)
        card (first (get-in game [:game/players seat :player/hand]))]
    (game/play-card game card)))

(defn play-trick [game]
  (nth (iterate play-card game) 4))

(deftest test-play
  (let [game (-> (game/game) (game/start) (game/deal game/deck))]

    (let [hand-before (get-in game [:game/players 1 :player/hand])
          card (first hand-before)
          game (game/play-card game card)]
      
      (expect :schafkopf/game game)
      (is (= [card] (:game/active-trick game)))
      (is (= 2 (:game/active-seat game)))
      (is (= (disj hand-before card)
             (get-in game [:game/players 1 :player/hand]))
          "Played card is no longer in the player's hand"))

    (testing "clears active seat when trick is complete"
      (let [game (play-trick game)]

        (is (true? (game/trick-complete? game)))
        (is (not (contains? game :game/active-seat))))
      )))

(deftest test-take
  (let [game (-> (game/game) (game/start) (game/deal game/deck) play-trick)
        seat 2
        trick (:game/active-trick game)
        game (game/take-trick game seat)]
    
    (is (= [] (:game/active-trick game)))
    (is (= [trick] (get-in game [:game/players seat :player/tricks])))
    (is (= trick (:game/prev-trick game)))
    (is (= seat (:game/active-seat game)))
    ))