(ns schafkopf.frontend.util)

(defn parse-int
  ([s] (parse-int s nil))
  ([s default]
   (let [n (js/parseInt s)]
     (if (js/isNaN n)
       default
       n))))
