(ns schafkopf.frontend.auth.view
  (:require [mui-bien.core.button :refer [button]]
            [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.circular-progress :refer [circular-progress]]
            [mui-bien.core.paper :refer [paper]]
            [mui-bien.core.styles :as mui-styles :refer [with-styles]]
            [mui-bien.core.text-field :refer [text-field]]
            [mui-bien.core.typography :refer [typography]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [schafkopf.frontend.auth.core :as auth]))

(def actions
  (with-styles
   (fn [theme]
     {:root {:marginTop (.spacing theme 1)
             :display :flex
             :justifyContent :flex-end}})
   (fn [{:keys [classes children]}]
     [:div {:class (:root classes)} children])))

(defn host-form []
  (let [password (r/atom "")
        loading? (rf/subscribe [::auth/host-loading?])
        error-text (rf/subscribe [::auth/host-error])]
    (fn []
      [:form
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::auth/host "Unnamed Host" @password]))}
       [text-field
        {:label "Kennwort"
         :type :password
         :full-width true
         :value @password
         :disabled @loading?
         :error (some? @error-text)
         :helper-text @error-text
         :on-change #(reset! password (.. % -target -value))}]
       [actions
        (when @loading?
          [circular-progress {:size 24}])
        [button
         {:type :submit
          :color :primary
          :disabled @loading?}
         "Anmelden"]]])))

(defn join-form []
  (let [name (r/atom "")
        code (r/atom "")
        loading? (rf/subscribe [::auth/join-loading?])
        error-text (rf/subscribe [::auth/join-error])]
    (fn []
      [:form
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::auth/join @name @code]))}
       [text-field
        {:label "Name"
         :full-width true
         :value @name
         :disabled @loading?
         :on-change #(reset! name (.. % -target -value))}]
       [text-field
        {:label "Code"
         :full-width true
         :value @code
         :disabled @loading?
         :error (some? @error-text)
         :helper-text @error-text
         :on-change #(reset! code (.. % -target -value))}]
       [actions
        (when @loading?
          [circular-progress {:size 24}])
        ;; TODO Merge forms, allow hosts to enter their name.
        #_[mui/form-control-label
         {:control [mui/switch]
          :label "Gastgeber"}]
        [button
         {:type :submit
          :color :primary}
         "Spielen"]]])))

(def auth-screen
  (with-styles
   (fn [theme]
     {:root {:minHeight "100vh"
             :backgroundColor (mui-styles/emphasize (.. theme -palette -background -paper))}
      :paper {:padding (.spacing theme 2)}})
   (fn [{:keys [classes]}]
     [:div {:class (:root classes)}
      [grid
       {:classes {:root (:root classes)}
        :container true
        :justify :center
        :align-items :center}
       [grid
        {:item true}
        [paper
         {:classes {:root (:paper classes)}}
         [typography {:variant :h6} "Schafkopf"]
         [join-form]
         [typography "Für Gastgeber:"]
         [host-form]]]]])))
