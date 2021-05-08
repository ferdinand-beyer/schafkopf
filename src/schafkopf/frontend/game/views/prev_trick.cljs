(ns schafkopf.frontend.game.views.prev-trick
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.backdrop :refer [backdrop]]
            [mui-bien.core.button :refer [button]]
            [mui-bien.core.styles :refer [make-styles]]

            [schafkopf.frontend.components.trick :refer [linear-trick]]

            [schafkopf.frontend.game.core :as g]))

(rf/reg-event-db
 ::toggle-open
 (fn [db _]
   (update db ::open? not)))

(rf/reg-sub
 ::open?
 (fn [db _]
   (boolean (::open? db))))

(def use-styles
  (make-styles
   (fn [{:keys [z-index]}]
     {:backdrop {:z-index (inc (z-index :drawer))}})))

(defn prev-trick-view* []
  (with-let [prev-trick (rf/subscribe [::g/prev-trick])
             open? (rf/subscribe [::open?])]
    (let [classes (use-styles)]
      [backdrop
       {:class (classes :backdrop)
        :open @open?
        :on-click #(rf/dispatch [::toggle-open])}
       (when @open?
         [:div
          [linear-trick {:cards @prev-trick}]])])))

(defn prev-trick-view []
  [:f> prev-trick-view*])

(defn show-prev-trick-button [props & children]
  (with-let [prev-trick (rf/subscribe [::g/prev-trick])]
    [:<>
     [button
      (assoc props
             :disabled (nil? @prev-trick)
             :on-click #(rf/dispatch [::toggle-open]))
      children]]))
