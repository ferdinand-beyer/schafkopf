(ns user
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :as ns-tools]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [pjstadig.humane-test-output]
            [schafkopf.core :as sk]))

(ns-tools/set-refresh-dirs "src" "dev" "test")

(pjstadig.humane-test-output/activate!)

(defn refresh []
  (ns-tools/refresh))

(defn run-tests []
  (run-all-tests #"^schafkopf\..*-test"))

(defn t []
  (ns-tools/refresh :after `run-tests))