(ns schafkopf.frontend.start.view
  (:require [mui-bien.core :as mui]
            ["@material-ui/core/styles" :as mui-styles]))

(def actions
  (mui/with-styles
   (fn [theme]
     {:root {:marginTop (.spacing theme 1)
             :display :flex
             :justifyContent :flex-end}})
   (fn [{:keys [classes children]}]
     [:div {:class (:root classes)} children])))

(defn host-form []
  [:form
   [mui/text-field {:label "Kennwort"
                    :type :password
                    :full-width true}]
   [actions
    [mui/button
     {:type :submit
      :color :primary}
     "Anmelden"]]])

(defn guest-form []
  [:form
   [mui/text-field {:label "Dein Name"
                    :full-width true}]
   [mui/text-field {:label "Code"
                    :full-width true}]
   [actions
    [mui/button
     {:type :submit
      :color :primary}
     "Spielen"]]])

(def start-screen
  (mui/with-styles
   (fn [theme]
     {:root {:minHeight "100vh"
             :backgroundColor (mui-styles/emphasize (.. theme -palette -background -paper))
             :display :flex
             :justifyContent :center
             :alignItems :center}
      :paper {:padding (.spacing theme 2)}})
   (fn [{:keys [classes]}]
     [:div {:class (:root classes)}
      [mui/paper
       {:classes {:root (:paper classes)}}
       [mui/typography {:variant :h6} "Schafkopf"]
       [guest-form]
       [mui/typography "Für Gastgeber:"]
       [host-form]]])))