(ns schafkopf.frontend.game.views.tricks
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]
            
            [mui-bien.core.backdrop :refer [backdrop]]
            [mui-bien.core.button :refer [button]]
            [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.styles :refer [make-styles]]
            
            [schafkopf.frontend.components.trick :refer [linear-trick]]
            
            [schafkopf.frontend.game.core :as g]))

(rf/reg-event-db
 ::show-tricks
 (fn [db [_ tricks]]
   (assoc db ::tricks tricks)))

(rf/reg-event-db
 ::hide-tricks
 (fn [db _]
   (dissoc db ::tricks)))

(rf/reg-sub
 ::tricks
 (fn [db _]
   (::tricks db)))

(rf/reg-sub
 ::open?
 :<- [::tricks]
 (fn [tricks _]
   (some? tricks)))

(def use-styles
  (make-styles
   (fn [{:keys [spacing z-index]}]
     {:backdrop {:z-index (inc (z-index :drawer))}
      :container {:padding (spacing 2)
                  :max-height "100vh"
                  :overflow :scroll}})))

(defn- tricks-panel [{:keys [classes]}]
  (with-let [tricks (rf/subscribe [::tricks])]
    [:div
     {:class (classes :container)}
     [grid
      {:container true
       :spacing 2
       :direction :column
       :justify :center
       :align-items :center
       :wrap :nowrap}
      (for [[i trick] (map vector (range) @tricks)]
        ^{:key i}
        [grid
         {:item true}
         [linear-trick {:cards trick}]])]]))

(defn- tricks-backdrop* []
  (with-let [open? (rf/subscribe [::open?])]
    (let [classes (use-styles)]
      [backdrop
       {:class (classes :backdrop)
        :open @open?
        :on-click #(rf/dispatch [::hide-tricks])}
       [tricks-panel {:classes classes}]])))

(defn tricks-backdrop []
  [:f> tricks-backdrop*])

(defn- show-button [{:keys [tricks] :as props} & children]
  [button
   (-> (dissoc props :tricks)
       (assoc :disabled (empty? tricks)
              :on-click #(rf/dispatch [::show-tricks tricks])))
   children])

(defn show-tricks-button [{:keys [seat] :as props} & children]
  (with-let [tricks (if (some? seat)
                      (rf/subscribe [::g/peer-tricks seat])
                      (rf/subscribe [::g/tricks]))]
    [show-button
     (-> props
         (dissoc :seat)
         (assoc :tricks @tricks))
     (or children "Stiche anzeigen")]))

(defn show-prev-trick-button [props & children]
  (with-let [prev-trick (rf/subscribe [::g/prev-trick])]
    [show-button
     (assoc props :tricks (when-let [trick @prev-trick]
                            [trick]))
     (or children "Letzter Stich")]))
