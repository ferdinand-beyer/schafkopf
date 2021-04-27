(ns schafkopf.protocol
  "Utilities for the client-server protocol.
   
   Actions are allowed based on predicates:

     join! when free-seats?
     start! when all-present? and not started?
     play! when my-turn?
     take! when trick-complete?
     score! when game-done?
     next! when game-scored?

   "
  (:require [clojure.spec.alpha :as s]
            [schafkopf.game :as game]))

(s/def :server/code (s/and string? #(<= 4 (count %) 8)))
(s/def :client/name (s/and string? #(<= 2 (count %) 20)))

(s/def :client/peer
  (s/merge :player/peer
           (s/keys :req [:client/name])))

(defn present? [peer]
  (or (= :player/self peer)
      (some? (:client/name peer))))

(s/def :client/peer-slot
       (s/or :peer :client/peer
             :self :player/self))

(s/def :client/peers (s/coll-of :client/peer-slot :kind vector? :count 4))

;; TODO: :player/peers is actually :client/peers
;; TODO: Use unqualified keys? Can be done with s/and?
(s/def :client/game
  (s/merge :player/game
           :client/peer
           (s/keys :req [:server/code])))

(defn all-present?
  "Returns true when all players are present, false otherwise."
  [game]
  (every? present? (game/players game)))

(defn can-start? [game]
  (and (all-present? game)
       (not (game/started? game))))
