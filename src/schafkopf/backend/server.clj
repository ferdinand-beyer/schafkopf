(ns schafkopf.backend.server
  (:require [mount.core :as mount]
            [wrench.core :as config]
            [org.httpkit.server :as http-kit]
            [reitit.ring :as reitit]
            [ring.middleware.defaults :as ring-def]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.util.codec :refer [base64-decode]]
            [taoensso.timbre :as timbre]
            [schafkopf.backend.api :as api]
            [schafkopf.backend.pages :as pages]))

(config/def port {:spec int? :default 8000})
(config/def cookie-store-key {:spec string? :required true :secret true})

(def routes
  [["/" {:get pages/index}]
   ["/assets/js/*" (reitit/create-resource-handler)]])

(defn session-store []
  (cookie-store {:key (base64-decode cookie-store-key)}))

(defn ring-handler []
  (-> (reitit/ring-handler
       (reitit/router [routes api/routes])
       (reitit/create-default-handler))

      (ring-def/wrap-defaults
       (-> ring-def/site-defaults
           (assoc-in [:session :store] (session-store))
           (assoc-in [:session :cookie-name] "session")))))

(def server-options
  {:port port
   :server-header "schafkopf"
   :legacy-return-value? false
   :event-logger #(timbre/trace %)   
   :warn-logger #(timbre/warn %2 %1)
   :error-logger #(timbre/error %2 %1)})

(mount/defstate server
  :start (http-kit/run-server (ring-handler) server-options)
  :stop (http-kit/server-stop! server {:timeout 300}))