(ns schafkopf.protocol
  "Utilities for the client-server protocol."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [schafkopf.game :as g]))

;;;; Server API

(def not-blank? (complement str/blank?))

(s/def ::name (s/and string? #(<= 1 (count %) 20)))
(s/def ::password (s/and string? not-blank?))
(s/def ::join-code (s/and string? #(<= 4 (count %) 8)))

(s/def ::error #{:invalid-params
                 :invalid-credentials
                 :join-failed})

(s/def ::error-response (s/keys :req-un [::error]))

(s/def ::host-params (s/keys :req-un [::name ::password]))
(s/def ::join-params (s/keys :req-un [::name ::join-code]))

;;;; Client game

(s/def :server/game-id string?)
(s/def :server/join-code ::join-code)
(s/def :server/seqno int?)

(s/def :server/info (s/keys :req [:server/game-id
                                  :server/join-code
                                  :server/seqno]))

(s/def :client/client-id string?)
(s/def :client/name ::name)

(s/def :client/info (s/keys :req [:client/client-id]))

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
           :server/info
           :client/info
           :client/peer))

(defn all-present?
  "Returns true when all players are present, false otherwise."
  [game]
  (every? present? (g/players game)))

(defn can-start? [game]
  (and (all-present? game)
       (not (g/started? game))))
