(ns schafkopf.frontend.game.view
  (:require
   [reagent.core :as r :refer [with-let]]
   [re-frame.core :as rf]

   [mui-bien.core.button :refer [button]]
   [mui-bien.core.circular-progress :refer [circular-progress]]
   [mui-bien.core.grid :refer [grid]]
   [mui-bien.core.styles :refer [make-styles lighten]]
   [mui-bien.core.typography :refer [typography]]

   [schafkopf.frontend.game.core :as game]
   [schafkopf.frontend.game.preferences :as prefs]
   [schafkopf.frontend.game.score :as score]

   [schafkopf.frontend.components.hand :refer [hand]]
   [schafkopf.frontend.components.trick :refer [stacked-trick]]

   [schafkopf.frontend.game.views.game-bar :refer [game-bar]]
   [schafkopf.frontend.game.views.peer-info :refer [peer-info]]
   [schafkopf.frontend.game.views.player-bar :refer [player-bar]]
   [schafkopf.frontend.game.views.scoring :refer [score-button]]
   [schafkopf.frontend.game.views.tricks :refer [tricks-backdrop]]))

(defn- active-trick []
  (with-let [trick (rf/subscribe [::game/active-trick])
             lead-seat (rf/subscribe [::game/mapped-lead-seat])]
    [stacked-trick {:cards @trick
                    :lead @lead-seat}]))

(defn- waiting []
  [grid
   {:container true
    :direction :column
    :justify :center
    :align-items :center
    :spacing 2}
   [grid
    {:item true}
    [circular-progress]]
   [grid
    {:item true}
    [typography
     {:color :textSecondary}
     "Warten auf weitere Teilnehmer..."]]])

(defn- center []
  (let [started? @(rf/subscribe [::game/started?])
        can-start? @(rf/subscribe [::game/can-start?])
        can-score? @(rf/subscribe [::score/can-score?])
        can-start-next? @(rf/subscribe [::game/can-start-next?])]
    (cond
      can-start-next?
      [button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [::game/start-next])}
       "Nächstes Spiel"]

      can-score?
      [score-button
       {:variant :contained
        :color :primary}]

      started?
      [active-trick]

      can-start?
      [button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [::game/start])}
       "Spiel starten"]

      :else
      [waiting])))

(defn- peer-info-area []
  (with-let [left-seat (rf/subscribe [::game/left-seat])
             across-seat (rf/subscribe [::game/across-seat])
             right-seat (rf/subscribe [::game/right-seat])]
    [grid
     {:item true
      :container true
      :justify :space-around
      :wrap "nowrap"}
     [grid
      {:item true}
      [peer-info {:seat @left-seat}]]
     [grid
      {:item true}
      [peer-info {:seat @across-seat}]]
     [grid
      {:item true}
      [peer-info {:seat @right-seat}]]]))

(defn- player-hand [{:keys [class]}]
  (with-let [cards (rf/subscribe [::prefs/hand])
             can-play? (rf/subscribe [::game/can-play?])]
    [:div
     {:class class}
     [hand {:cards @cards
            :disabled? (not @can-play?)
            :on-play #(rf/dispatch [::game/play %])}]]))

(defn make-gradient [palette key]
  (str "linear-gradient("
       (get-in palette [:background :paper])
       ", "
       (lighten
        (get-in palette [key :light])
        0.75)
       ")"))

(def use-styles
  (make-styles
   (fn [{:keys [palette]}]
     {:root {:min-height "100vh"
             :overflow :hidden
             :background-image (make-gradient palette :primary)}
      :active {:background-image (make-gradient palette :secondary)}
      :hand {:padding-bottom 32}})
   {:name "game"}))

(defn- game-screen* []
  (with-let [active? (rf/subscribe [::game/active?])]
    (let [classes (use-styles)]
      [:<>
       [grid
        {:class [(when @active? (classes :active))]
         :classes (select-keys classes [:root])
         :container true
         :direction :column
         :justify :space-between}

        [grid
         {:item true}
         [game-bar]]

        [peer-info-area]

        [grid
         {:item true
          :container true
          :justify :center
          :align-items :center
          :xs true}
         [grid
          {:item true}
          [center]]]

        [grid
         {:item true}
         [player-hand
          {:class (classes :hand)}]]]

       [player-bar]
       [tricks-backdrop]])))

(defn game-screen [_]
  [:f> game-screen*])
