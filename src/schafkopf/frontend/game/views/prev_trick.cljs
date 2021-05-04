(ns schafkopf.frontend.game.views.prev-trick
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.backdrop :refer [backdrop]]
            [mui-bien.core.button :refer [button]]
            [mui-bien.core.styles :refer [make-styles]]

            [schafkopf.frontend.components.trick :refer [stacked-trick]]

            [schafkopf.frontend.game.core :as g]))

(def use-styles
  (make-styles
   (fn [{:keys [z-index]}]
     {:backdrop {:z-index (inc (z-index :drawer))}})))

(defn show-prev-trick-button* [props & children]
  (with-let [prev-trick (rf/subscribe [::g/prev-trick])
             open? (r/atom false)
             toggle-open (fn []
                           (swap! open?
                                  #(and (not %)
                                        (some? @prev-trick))))]
    (let [classes (use-styles)]
      [:<>
       [button
        (assoc props
               :disabled (nil? @prev-trick)
               :on-click toggle-open)
        children]
       [backdrop
        {:class (classes :backdrop)
         :open @open?
         :on-click toggle-open}
        (when @open?
          [:div
           [stacked-trick {:cards @prev-trick}]])]])))

(defn show-prev-trick-button [props & children]
  [:f> show-prev-trick-button* props children])
