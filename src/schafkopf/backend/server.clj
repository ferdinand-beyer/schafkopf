(ns schafkopf.backend.server
  (:require [mount.core :as mount]
            [wrench.core :as config]
            [taoensso.timbre :as log]

            [org.httpkit.server :as http-kit]

            [muuntaja.core :as m]
            [muuntaja.middleware :refer [wrap-format]]
            [reitit.ring :as reitit]
            [reitit.ring.middleware.muuntaja :as muuntaja]

            [ring.middleware.defaults :as ring-def]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.util.codec :refer [base64-decode]]

            [schafkopf.backend.api :as api]
            [schafkopf.backend.pages :as pages]))

(config/def port {:spec int? :default 8000})
(config/def cookie-store-key {:spec string? :require true :secret true})

;; Configure content negotiation and Transit support.
(def muuntaja m/instance)

(def routes
  [["/" {:get pages/index}]
   ["/assets/js/*" (reitit/create-resource-handler)]])

(defn session-store []
  (cookie-store {:key (base64-decode cookie-store-key)}))

(def anti-forgery-error-handler
  "Custom error handler that produces a machine-readable body and
   content negotiation"
  (-> (fn [_]
        {:status 403
         :headers {}
         :body {:error :invalid-csrf-token}})
      (wrap-format muuntaja)))

(defn ring-handler []
  (-> (reitit/ring-handler
       (reitit/router
        [routes api/routes]
        {:data {:muuntaja muuntaja
                :middleware [muuntaja/format-middleware]}})
       (reitit/create-default-handler))

      (ring-def/wrap-defaults
       (-> ring-def/site-defaults
           (assoc-in [:session :store] (session-store))
           (assoc-in [:session :cookie-name] "session")
           (assoc-in [:session :cookie-attrs :max-age] (* 12 60 60))
           (assoc-in [:security :anti-forgery]
                     {:error-handler anti-forgery-error-handler})))))

(def server-options
  {:port port
   :server-header "schafkopf"
   :legacy-return-value? false
   :event-logger #(log/trace %)   
   :warn-logger #(log/warn %2 %1)
   :error-logger #(log/error %2 %1)})

(mount/defstate server
  :start (http-kit/run-server (ring-handler) server-options)
  :stop (http-kit/server-stop! server {:timeout 300}))
