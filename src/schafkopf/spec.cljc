(ns schafkopf.spec
  (:require [clojure.spec.alpha :as s]
            [schafkopf.game :as game]))

(s/def :session/code (s/and string? #(<= 4 (count %) 8)))
(s/def :player/name (s/and string? #(<= 2 (count %) 20)))

(s/def :user/game
  (s/merge :player/game
           (s/keys :req [:session/code])))
