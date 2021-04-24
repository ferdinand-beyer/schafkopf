(ns schafkopf.backend.pages
  (:require [hiccup.page :as page]))

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body body})

(defn index [_]
  (response
   (page/html5
    {:lang "de"}
    [:head
     [:title "Schafkopf."]]
    [:body
     [:div#app]
     (page/include-js "/js/main.js")])))