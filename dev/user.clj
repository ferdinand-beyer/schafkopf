(ns user
  {:clj-kondo/config '{:linters {:unused-namespace {:level :off}
                                 :refer-all {:level :off}}}}
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :as ns-tools]
            
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]

            [mount.core :as mount]
            [wrench.core :as wrench]
            [pjstadig.humane-test-output]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            
            [schafkopf.backend.control :as ctl]
            [schafkopf.backend.server :as backend]
            [schafkopf.game :as game]
            [schafkopf.protocol :as protocol]
            
            [user.cljs-build :as cljs-build]))

(ns-tools/set-refresh-dirs "src" "dev" "test")

(pjstadig.humane-test-output/activate!)

;; Log to std-out (*out* might be re-bound in threads!)
(timbre/merge-config!
 {:appenders {:std-out (appenders/println-appender {:stream :std-out})}})

(defn app-states []
  (filter #(str/starts-with? % "#'schafkopf") (mount/find-all-states)))

(defn start []
  (wrench/reset! :env (wrench/from-file "dev/config.edn"))
  (wrench/validate-and-print)
  (mount/start))

(defn stop []
  (mount/stop (app-states)))

(defn reset []
  (stop)
  (ns-tools/refresh :after `start))

(defn run-tests []
  (run-all-tests #"^schafkopf\..*-test"))

(defn t []
  (ns-tools/refresh :after `run-tests))