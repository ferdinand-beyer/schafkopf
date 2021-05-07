(ns schafkopf.frontend.game.views.sort
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.button :refer [button]]
            [mui-bien.core.divider :refer [divider]]
            [mui-bien.core.list-item-icon :refer [list-item-icon]]
            [mui-bien.core.list-item-text :refer [list-item-text]]
            [mui-bien.core.menu :refer [menu]]
            [mui-bien.core.menu-item :refer [menu-item]]

            [mui-bien.icons.check :refer [check-icon]]

            [schafkopf.frontend.game.preferences :as prefs]
            
            [schafkopf.frontend.components.playing-card :refer [suit-names]]))

(defn sort-menu [props]
  (with-let [opts (rf/subscribe [::prefs/sort-opts])
             checked [check-icon {:font-size :small}]]
    [menu
     props

     [menu-item
      {:on-click #(rf/dispatch [::prefs/toggle-sort-by-ober])}
      [list-item-icon (when (:ober? @opts) checked)]
      [list-item-text "Ober"]]
     [menu-item
      {:on-click #(rf/dispatch [::prefs/toggle-sort-by-unter])}
      [list-item-icon (when (:unter? @opts) checked)]
      [list-item-text "Unter"]]

     [divider]
     (doall
      (for [suit [:acorns :leaves :hearts :bells]]
        ^{:key suit}
        [menu-item
         {:on-click #(rf/dispatch [::prefs/sort-by-suit suit])}
         [list-item-icon (when (= suit (:suit @opts)) checked)]
         [list-item-text (suit-names suit)]]))

     [divider]
     [menu-item
      {:on-click #(rf/dispatch [::prefs/sort-as-given])}
      [list-item-icon (when (nil? @opts) checked)]
      [list-item-text "Wie ausgeteilt"]]
     
     [divider]
     [menu-item
      {:on-click #(rf/dispatch [::prefs/sort-default])}
      [list-item-icon]
      [list-item-text "Standard"]]]))

(defn sort-button [props]
  (with-let [anchor-el (r/atom nil)]
    [:<>
     [button
      (assoc props :on-click #(reset! anchor-el (.-currentTarget %)))
      "Sortieren"]
     (let [close-menu #(reset! anchor-el nil)]
       [sort-menu
        {:open (some? @anchor-el)
         :anchor-el @anchor-el
         :on-close close-menu}])]))
