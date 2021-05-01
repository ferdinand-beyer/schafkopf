(ns user
  {:clj-kondo/config
   '{:linters {:unused-namespace {:level :off}
               :unused-referred-var {:level :off}
               :refer-all {:level :off}}}}
  
  (:require [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :as ns-tools :refer [refresh]]
            
            [clojure.core.async :as async]
            [clojure.string :as str]

            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]

            [kaocha.repl :as k]
            [pjstadig.humane-test-output]

            [mount.core :as mount]
            [wrench.core :as config]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]

            [schafkopf.backend.control :as sg]
            [schafkopf.game :as g]
            [schafkopf.protocol :as protocol]

            [user.cljs-build :as cljs-build]
            [user.game-control :as ctl]
            [user.util :as util]))

(ns-tools/set-refresh-dirs "src" "dev" "test")

(pjstadig.humane-test-output/activate!)

;; Log to std-out (*out* might be re-bound in threads!)
(log/merge-config!
 {:appenders {:println (appenders/println-appender {:stream :std-out})}})

(stest/instrument)

(defn app-states []
  (filter #(or (str/starts-with? % "#'schafkopf")
               (str/starts-with? % "#'user/"))
          (mount/find-all-states)))

(defn start []
  (k/run-all)
  (config/reset! :env (config/from-file "dev/config.edn"))
  (config/validate-and-print)
  (mount/start))

(defn stop []
  (mount/stop (app-states)))

(defn reset []
  (stop)
  (refresh :after `start))
