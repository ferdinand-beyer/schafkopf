(ns schafkopf.frontend.auth.view
  (:require [mui-bien.core :as mui]
            ["@material-ui/core/styles" :as mui-styles]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [schafkopf.frontend.auth.core :as auth]))

(def actions
  (mui/with-styles
   (fn [theme]
     {:root {:marginTop (.spacing theme 1)
             :display :flex
             :justifyContent :flex-end}})
   (fn [{:keys [classes children]}]
     [:div {:class (:root classes)} children])))

(defn host-form []
  (let [password (r/atom "")
        loading? (rf/subscribe [::auth/host-loading?])
        error-text (rf/subscribe [::auth/host-login-error])]
    (fn []
      [:form
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::auth/host-login @password]))}
       [mui/text-field
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
          [mui/circular-progress {:size 24}])
        [mui/button
         {:type :submit
          :color :primary
          :disabled @loading?}
         "Anmelden"]]])))

(defn guest-form []
  (let [name (r/atom "")
        code (r/atom "")
        loading? (rf/subscribe [::auth/guest-loading?])
        error-text (rf/subscribe [::auth/guest-join-error])]
    (fn []
      [:form
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::auth/guest-join @code @name]))}
       [mui/text-field {:label "Code"
                        :full-width true
                        :value @code
                        :disabled @loading?
                        :error (some? @error-text)
                        :helper-text @error-text
                        :on-change #(reset! code (.. % -target -value))}]
       [mui/text-field {:label "Name"
                        :full-width true
                        :value @name
                        :disabled @loading?
                        :on-change #(reset! name (.. % -target -value))}]
       [actions
        (when @loading?
          [mui/circular-progress {:size 24}])
        ;; TODO Merge forms, allow hosts to enter their name.
        #_[mui/form-control-label
         {:control [mui/switch]
          :label "Gastgeber"}]
        [mui/button
         {:type :submit
          :color :primary}
         "Spielen"]]])))

(def auth-screen
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
