(ns schafkopf.frontend.game.views.game-bar
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.app-bar :refer [app-bar]]
            [mui-bien.core.icon-button :refer [icon-button]]
            [mui-bien.core.menu :refer [menu]]
            [mui-bien.core.menu-item :refer [menu-item]]
            [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.toolbar :refer [toolbar]]
            [mui-bien.core.typography :refer [typography]]

            [mui-bien.icons.more-vert :refer [more-vert-icon]]

            [schafkopf.frontend.game.core :as g]))

(def use-styles
  (make-styles
   (fn [{:keys [spacing]}]
     {:root {:margin-bottom (spacing 2)
             :transition "background-color 225ms ease-in-out"}
      :stat {:margin-right (spacing 2)}
      :stretch {:flex-grow 1}})))

(defn- game-info [{:keys [classes]}]
  (with-let [join-code (rf/subscribe [::g/join-code])
             number (rf/subscribe [::g/number])
             round (rf/subscribe [::g/round])
             pot (rf/subscribe [::g/pot])]
    (let [stat-props {:variant :overline
                      :class (classes :stat)}]
      [:<>
       [typography
        stat-props
        "Raumcode: " @join-code]
       [typography
        stat-props
        "Spiel: " @number]
       [typography
        stat-props
        "Runde: " @round]
       (when @pot
         [typography
          stat-props
          "Stock: " @pot])])))

(defn- game-actions [_]
  (with-let [can-undo? (rf/subscribe [::g/can-undo?])
             anchor-el (r/atom nil)]
    [:<>
     [icon-button
      {:color :inherit
       :edge :end
       :on-click #(reset! anchor-el (.-currentTarget %))}
      [more-vert-icon]]
     (let [close-menu #(reset! anchor-el nil)
           close-dispatch #(do (close-menu) (rf/dispatch %))]
       [menu
        {:open (some? @anchor-el)
         :anchor-el @anchor-el
         :on-close close-menu}
        [menu-item
         {:disabled (not @can-undo?)
          :on-click #(close-dispatch [::g/undo])}
         "Rückgängig"
         ]])
     ]))

(defn- game-bar* [_]
  (with-let [active? (rf/subscribe [::g/active?])]
    (let [classes (use-styles)]
      [app-bar
       {:position :static
        :color (if @active? :secondary :primary)
        :class (classes :root)}
       [toolbar
        {:variant :dense}
        [typography {:variant :h6}
         "Schafkopf"]
        [:div {:class (classes :stretch)}]
        [game-info {:classes classes}]
        [:div {:class (classes :stretch)}]
        [game-actions {:classes classes}]]])))

(defn game-bar [_]
  [:f> game-bar*])
