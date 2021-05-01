(ns schafkopf.main
  (:gen-class)
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [wrench.core :as config]
            [schafkopf.backend.server]))

(defn setup-logging! []
  (log/merge-config!
   {:appenders {:println (appenders/println-appender {:stream :std-out})}
    :min-level :info}))

(defn -main [& _args]
  (setup-logging!)
  (log/info "Starting application")

  ;; TODO validate-and-log to timbre?
  (when-not (config/validate-and-print)
    (System/exit 1))

  (mount/start))
