(ns schafkopf.spec
  (:require [clojure.spec.alpha :as s]))

(def suit? #{:acorns :leaves :hearts :bells})
(def rank? (into #{:deuce :king :ober :unter} (range 7 11)))

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