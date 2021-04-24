(ns schafkopf.backend.server
  (:require [mount.core :as mount]
            [org.httpkit.server :as http-kit]
            [reitit.ring :as ring]
            [ring.middleware.defaults :as ring-def]
            [schafkopf.backend.pages :as pages]))

(def router
  (ring/router
   [["/" {:get pages/index}]
    ["/js/*" (ring/create-resource-handler)]]))

(def ring-handler
  (-> (ring/ring-handler
       router
       (ring/create-default-handler))
      (ring-def/wrap-defaults ring-def/site-defaults)))

(def server-options
  {:port 8000})

(mount/defstate server
  :start (http-kit/run-server #'ring-handler server-options)
  :stop (server :timeout 300))