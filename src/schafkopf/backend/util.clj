(ns schafkopf.backend.util
  (:import [clojure.lang
            Counted Seqable IPersistentCollection IPersistentStack IReduceInit]))

(deftype BoundedStack [max-size v]
  Counted
  (count [_] (count v))

  Seqable
  (seq [_] (seq v))

  IReduceInit
  (reduce [_ f start] (reduce f start v))

  IPersistentCollection
  (cons [_ o]
    (let [new-v (conj (if (< (count v) max-size) v (subvec v 1)) o)]
      (BoundedStack. max-size new-v)))
  (empty [_]
    (BoundedStack. max-size []))
  (equiv [_ o]
    (and (= max-size (.max-size o))
         (= v (.data o))))

  IPersistentStack
  (peek [_]
    (peek v))
  (pop [this]
    (if (seq v)
      (BoundedStack. max-size (pop v))
      this)))

(ns-unmap *ns* '->BoundedStack)

(defn bounded-stack
  "Creates an empty persistent stack with a maximum size.  When more than
   max-size elements are added, the oldest one will be dropped."
  [max-size]
  {:pre [(pos? max-size)]}
  (BoundedStack. max-size []))
