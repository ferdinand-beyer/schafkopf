(ns schafkopf.backend.server
  (:require [mount.core :as mount]
            [wrench.core :as config]
            [org.httpkit.server :as http-kit]
            [reitit.ring :as ring]
            [ring.middleware.defaults :as ring-def]
            [schafkopf.backend.pages :as pages]))

(config/def port {:name "HTTP_PORT" :spec int? :default 8000})

(def routes
  [["/" {:get pages/index}]
   ["/js/*" (ring/create-resource-handler)]])

(def ring-handler
  (-> (ring/ring-handler
       (ring/router routes)
       (ring/create-default-handler))
      (ring-def/wrap-defaults ring-def/site-defaults)))

(def server-options
  {:port port})

(mount/defstate server
  :start (http-kit/run-server #'ring-handler server-options)
  :stop (server :timeout 300))