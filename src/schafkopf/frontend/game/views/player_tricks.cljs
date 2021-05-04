(ns schafkopf.frontend.game.views.player-tricks
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]
            
            [mui-bien.core.backdrop :refer [backdrop]]
            [mui-bien.core.button :refer [button]]
            [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.styles :refer [make-styles]]
            
            [schafkopf.frontend.components.trick :refer [linear-trick]]
            
            [schafkopf.frontend.game.core :as g]))

(def use-styles
  (make-styles
   (fn [{:keys [spacing z-index]}]
     ; Hack when our backdrop is included in card-actions.
     ; A cleaner solution would be to use re-frame styles and
     ; a "global" trick view panel.
     {:backdrop {:margin-left "0 !important"
                 :z-index (inc (z-index :drawer))}
      :container {:padding (spacing 2)
                  :max-height "100vh"
                  :overflow :scroll}})))

(defn tricks-view [{:keys [classes seat]}]
  (with-let [tricks (if (some? seat)
                      (rf/subscribe [::g/peer-tricks seat])
                      (rf/subscribe [::g/tricks]))]
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

(defn show-tricks-button* [{:keys [seat] :as props} & children]
  (with-let [open? (r/atom false)
             toggle-open #(swap! open? not)]
    (let [classes (use-styles)]
      [:<>
       [button
        (-> (dissoc props :seat)
            (assoc :disabled @open?
                   :on-click toggle-open))
        children]
       [backdrop
        {:class (classes :backdrop)
         :open @open?
         :on-click toggle-open}
        (when @open?
          [tricks-view {:classes classes
                        :seat seat}])]])))

(defn show-tricks-button [props & children]
  [:f> show-tricks-button* props children])
