(ns schafkopf.backend.pages
  (:require [hiccup.page :as page]))

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body body})

(defn index [{:keys [anti-forgery-token]}]
  (response
   (page/html5
    {:lang "de"}
    [:head
     [:meta {:name "token"
             :content anti-forgery-token}]
     [:title "Schafkopf."]]
    [:body
     [:div#app]
     (page/include-js "/js/main.js")])))