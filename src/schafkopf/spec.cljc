(ns schafkopf.spec
  "Specs for the server interface."
  (:require [clojure.spec.alpha :as s]
            [schafkopf.game :as game]))

(s/def :server/code (s/and string? #(<= 4 (count %) 8)))
(s/def :client/name (s/and string? #(<= 2 (count %) 20)))

;; TODO: Use unqualified keys to allow "extension" of specs,
;; e.g. a player/game expects player/peers, but a client/game
;; expects client/peers.
;; Alternative: Conversion functions.
(s/def :client/game
  (s/merge :player/game
           (s/keys :req [:session/code])))
