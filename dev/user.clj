(ns user
  {:clj-kondo/config '{:linters {:unused-namespace {:level :off}
                                 :refer-all {:level :off}}}}
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :as ns-tools]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [mount.core :as mount]
            [wrench.core :as wrench]
            [pjstadig.humane-test-output]
            [schafkopf.core :as sk]
            [schafkopf.backend.server :as backend]
            [user.cljs-build :as cljs-build]))

(ns-tools/set-refresh-dirs "src" "dev" "test")

(pjstadig.humane-test-output/activate!)

(def app-states [#'backend/server])

(defn start []
  (wrench/reset! :env (wrench/from-file "dev/config.edn"))
  (wrench/validate-and-print)
  (mount/start))

(defn stop []
  (mount/stop app-states))

(defn reset []
  (stop)
  (ns-tools/refresh :after `start))

(defn run-tests []
  (run-all-tests #"^schafkopf\..*-test"))

(defn t []
  (ns-tools/refresh :after `run-tests))