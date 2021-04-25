(ns schafkopf.backend.api
  (:require [muuntaja.middleware :refer [wrap-format]]
            [wrench.core :as config]
            [taoensso.timbre :as timbre]))

(config/def host-password {:secret true})

(defn handle-authenticate
  [{:keys [body-params session]}]
  (if (= host-password (:password body-params))
    (do
      (timbre/info "Host authentication successful")
      {:status 200
       :body {}
       :session (assoc session :role :host :uid 1)})
    (do
      (timbre/info "Host authentication failed (invalid password)")
      {:status 403
       :body {:error :invalid-credentials}})))

(def routes
  [["/api" {:middleware [wrap-format]}
    ["/authenticate" {:post handle-authenticate}]]])