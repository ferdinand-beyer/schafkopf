(ns schafkopf.backend.pages
  (:require [hiccup.page :as page]
            [schafkopf.backend.assets :as assets]))

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

;; TODO: Get the user's active game code, if any, and place it in the
;; page so that they re-join automatically when they reload the page.
(defn index [{:keys [anti-forgery-token]}]
  (response
   (page/html5
    {:lang "de"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "minimum-scale=1, initial-scale=1, width=device-width"}]
     [:meta {:name "csrf-token" :content anti-forgery-token}]
     [:title "Schafkopf"]
     [:link {:rel "stylesheet"
             :href "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"}]]
    [:body
     [:div#app]
     (for [url (assets/js-assets)]
       (page/include-js url))])))
