(ns schafkopf.frontend.game.views.player-bar
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.app-bar :refer [app-bar]]
            [mui-bien.core.button :refer [button]]
            [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.toolbar :refer [toolbar]]

            [schafkopf.frontend.components.player-badge :refer [player-badge]]
            [schafkopf.frontend.components.stat :refer [stat score-stat]]

            [schafkopf.frontend.game.views.tricks :refer [show-tricks-button
                                                          show-prev-trick-button]]
            [schafkopf.frontend.game.views.sort :refer [sort-button]]

            [schafkopf.frontend.game.core :as g]))

(def use-styles
  (make-styles
   (fn [{:keys [spacing z-index]}]
     {:root {:top :auto
             :bottom 0
             :z-index (inc (:app-bar z-index))
             :background-color "rgba(255, 255, 255, 75%)"
             :backdrop-filter "blur(5px)"}
      :active {}
      :badge {:margin-right (spacing 4)}
      :stat {:margin-right (spacing 2)}
      :stretch {:flex-grow 1}})
   {:name "playerBar"}))

(defn- player-info [{:keys [classes]}]
  (with-let [name (rf/subscribe [::g/name])
             balance (rf/subscribe [::g/balance])
             trick-count (rf/subscribe [::g/trick-count])
             points (rf/subscribe [::g/points])
             score (rf/subscribe [::g/score])]
    [:<>
     [player-badge
      {:class (classes :badge)
       :name @name
       :balance @balance}]
     [stat
      {:class (classes :stat)
       :label "Stiche:"
       :value @trick-count}]
     (when @points
       [stat {:class (classes :stat)
              :label "Punkte:"
              :value @points}])
     (when @score
       [score-stat {:class (classes :stat)
                    :value @score}])]))

(defn- player-action-buttons [_]
  (with-let [can-take? (rf/subscribe [::g/can-take?])
             can-skip? (rf/subscribe [::g/can-skip?])
             can-see-tricks? (rf/subscribe [::g/can-see-tricks?])
             button-props {:color :inherit}]
    [:<>
     (when @can-skip?
       [button
        (assoc button-props
               :on-click #(rf/dispatch [::g/skip]))
        "Zusammenwerfen"])
     (when @can-take?
       [button
        (assoc button-props
               :color :primary
               :on-click #(rf/dispatch [::g/take]))
        "Stich nehmen"])
     (when @can-see-tricks?
       [show-tricks-button
        (assoc button-props :color :primary)])

     [sort-button]
     [show-prev-trick-button
      button-props]]))

(defn- player-bar*
  []
  (with-let [active? (rf/subscribe [::g/active?])]
    (let [classes (use-styles)]
      [app-bar
       {:component :footer
        :position :fixed
        :color :transparent
        :class [(classes :root)
                (when @active? (classes :active))]}
       [toolbar
        [player-info {:classes classes}]
        [:div {:class (classes :stretch)}]
        [player-action-buttons {:classes classes}]]])))

(defn player-bar [_]
  [:f> player-bar*])
