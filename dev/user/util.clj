(ns user.util
  (:require [ring.util.codec :refer [base64-encode]])
  (:import java.security.SecureRandom))

(defn rand-bytes [size]
  (let [arr (byte-array size)]
    (.nextBytes (SecureRandom.) arr)
    arr))

(defn random-cookie-store-key
  "Generates 16 random bytes, base64-encoded."
  []
  (base64-encode (rand-bytes 16)))
