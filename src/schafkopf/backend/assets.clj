(ns schafkopf.backend.assets
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn read-edn [source]
  (with-open [r (io/reader source)]
    (edn/read (java.io.PushbackReader. r))))

(defn read-manifest []
  (some-> (io/resource "public/assets/js/manifest.edn") read-edn))

(def js-assets
  (memoize (fn [] (->> (read-manifest)
                       (map :output-name)
                       (map #(str "/assets/js/" %))))))
