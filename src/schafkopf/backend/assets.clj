(ns schafkopf.backend.assets
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [ring.util.codec :refer [base64-encode]]))

(defn read-edn [source]
  (with-open [r (io/reader source)]
    (edn/read (java.io.PushbackReader. r))))

(defn read-manifest []
  (some-> (io/resource "public/assets/js/manifest.edn") read-edn))

(def js-assets
  (memoize (fn [] (->> (read-manifest)
                       (map :output-name)
                       (map #(str "/assets/js/" %))))))

(defn emoji-svg
  ([] (emoji-svg "🐑"))
  ([emoji]
   (html
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :viewBox "0 0 100 100"}
     [:text {:y ".9em" :font-size "90"}
      emoji]])))

(defn svg-data-uri [svg]
  (str "data:image/svg+xml;base64," (base64-encode (.getBytes svg))))

(defn emoji-svg-uri [& args]
  (svg-data-uri (apply emoji-svg args)))

(defn suit-svg-uri
  ([] (suit-svg-uri (rand-nth ["acorns" "leaves" "hearts" "bells"])))
  ([suit] (str "/assets/img/suits/" suit ".svg")))
