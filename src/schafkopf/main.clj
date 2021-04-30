(ns schafkopf.main
  (:gen-class)
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [wrench.core :as config]
            [schafkopf.backend.server]))

(defn -main [& _args]
  (log/set-level! :info)
  (log/info "Starting application")

  (when-not (config/validate-and-print)
    (System/exit 1))

  (mount/start))