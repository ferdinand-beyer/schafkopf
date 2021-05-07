(ns schafkopf.frontend.game.view
  (:require
   [reagent.core :as r :refer [with-let]]
   [re-frame.core :as rf]

   [mui-bien.core.button :refer [button]]
   [mui-bien.core.circular-progress :refer [circular-progress]]
   [mui-bien.core.grid :refer [grid]]
   [mui-bien.core.styles :refer [make-styles]]
   [mui-bien.core.typography :refer [typography]]

   [schafkopf.frontend.game.core :as game]
   [schafkopf.frontend.game.preferences :as prefs]
   [schafkopf.frontend.game.score :as score]

   [schafkopf.frontend.components.hand :refer [hand]]
   [schafkopf.frontend.components.trick :refer [stacked-trick]]

   [schafkopf.frontend.game.views.game-bar :refer [game-bar]]
   [schafkopf.frontend.game.views.peer-info :refer [peer-info]]
   [schafkopf.frontend.game.views.player-bar :refer [player-bar]]
   [schafkopf.frontend.game.views.prev-trick :refer [prev-trick-view]]
   [schafkopf.frontend.game.views.scoring :refer [score-button]]))

(defn player-hand [_]
  (with-let [cards (rf/subscribe [::prefs/hand])
             can-play? (rf/subscribe [::game/can-play?])]
    [hand {:cards @cards
           :disabled? (not @can-play?)
           :on-play #(rf/dispatch [::game/play %])}]))

(defn active-trick []
  (with-let [trick (rf/subscribe [::game/active-trick])]
    [grid
     {:container true
      :direction :column
      :justify :center
      :align-items :center
      :spacing 2}
     [grid
      {:item true}
      [stacked-trick {:cards @trick}]]]))

(defn center []
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
         "Warten auf weitere Teilnehmer..."]]])))

(defn peer-info-area []
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

(let [use-styles (make-styles {:root {:min-height "100vh"}})]
  (defn game-screen* []
    (let [classes (use-styles)]
      [:<>
       [grid
        {:classes classes
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
         [player-hand]]

        [grid
         {:item true}
         [player-bar]]]

       [prev-trick-view]])))

(defn game-screen [_]
  [:f> game-screen*])
