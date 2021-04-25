(ns schafkopf.frontend.comm
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]))

(def anti-forgery-token
  (some-> (.querySelector js/document "meta[name=token]") (.-content)))

(def translate-backend-call
  "Interceptor translating backend call effects to HTTP requests."
  (rf/->interceptor
   :id :secure-comm
   :after (fn [context]
            (if (some? (get-in context [:effects :http-xhrio]))
              (assoc-in context [:effects :http-xhrio :headers "X-CSRF-Token"] anti-forgery-token)
              context))))

(def backend-interceptors [translate-backend-call])