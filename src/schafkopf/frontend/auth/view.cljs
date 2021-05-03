(ns schafkopf.frontend.auth.view
  (:require [mui-bien.core.button :refer [button]]
            [mui-bien.core.collapse :refer [collapse]]
            [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.circular-progress :refer [circular-progress]]
            [mui-bien.core.form-control-label :refer [form-control-label]]
            [mui-bien.core.form-helper-text :refer [form-helper-text]]
            [mui-bien.core.paper :refer [paper]]
            [mui-bien.core.styles :refer [emphasize make-styles]]
            [mui-bien.core.switch :refer [switch]]
            [mui-bien.core.text-field :refer [text-field]]
            [mui-bien.core.typography :refer [typography]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [schafkopf.frontend.auth.core :as auth]))

(defn join-form []
  (let [name (r/atom "")
        code (r/atom "")
        password (r/atom "")
        host? (r/atom false)
        loading? (rf/subscribe [::auth/loading?])
        error-text (rf/subscribe [::auth/error])]
    (fn []
      [:form
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch
                     (if @host?
                       [::auth/host @name @password]
                       [::auth/join @name @code])))}
       [grid
        {:container true
         :spacing 1
         :direction :column}

        [grid
         {:item true}
         [text-field
          {:label "Name"
           :full-width true
           :variant :outlined
           :value @name
           :disabled @loading?
           ;:error (some? @error-text)
           :on-change #(reset! name (.. % -target -value))}]]
        
        [grid
         {:item true}
         [collapse
          {:in (not @host?)}
          [text-field
           {:label "Code"
            :full-width true
            :variant :outlined
            :value @code
            :disabled @loading?
            :error (some? @error-text)
            :on-change #(reset! code (.. % -target -value))}]]
         [collapse
          {:in @host?}
          [text-field
           {:label "Kennwort"
            :type :password
            :full-width true
            :variant :outlined
            :value @password
            :disabled @loading?
            :error (some? @error-text)
            :on-change #(reset! password (.. % -target -value))}]]]

        [grid
         {:item true}
         [form-helper-text
          {:error (some? @error-text)}
          (or @error-text "")]]
        
        [grid
         {:item true
          :container true
          :align-items :center
          :wrap "nowrap"}
         [grid
          {:item true
           :xs true}
          [form-control-label
           {:control [switch
                      {:size :small
                       :color :default
                       :checked @host?
                       :disabled @loading?
                       :on-change #(swap! host? not)}]
            :label "Gastgeber"}]]
         [grid
          {:item true}
          (when @loading?
            [circular-progress {:size 12}])]
         [grid
          {:item true}
          [button
           {:type :submit
            :color :primary
            :disabled @loading?}
           "Spielen"]]]]])))

(let [use-styles
      (make-styles
       (fn [{:keys [palette spacing]}]
         {:root {:minHeight "100vh"
                 :backgroundColor
                 (emphasize (get-in palette [:background :paper]))}
          :paper {:width "20rem"
                  :padding (spacing 2)}
          :headline {:margin-bottom (spacing 3)}}))]
  
  (defn screen-layout [_]
    (let [classes (use-styles)]
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
          [typography {:variant :h5
                       :class (:headline classes)}
           "Schafkopf"]
          [join-form]]]]])))

(defn auth-screen [_]
  [:f> screen-layout])
