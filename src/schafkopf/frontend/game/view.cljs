;; TODO: Split into multiple namespaces!
(ns schafkopf.frontend.game.view
  (:require
   [reagent.core :as r :refer [with-let]]
   [re-frame.core :as rf]

   ;; TODO require MUI components selectively!
   [mui-bien.core.all :as mui]
   [mui-bien.core.styles :refer [make-styles]]

   [schafkopf.frontend.game.core :as game]
   [schafkopf.frontend.game.score :as score]

   [schafkopf.frontend.components.hand :refer [hand]]
   [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]

   [schafkopf.frontend.game.views.game-bar :refer [game-bar]]
   [schafkopf.frontend.game.views.peer-info :refer [peer-info]]
   [schafkopf.frontend.game.views.player-bar :refer [player-bar]]
   [schafkopf.frontend.game.views.scoring :refer [score-button]]))

(defn player-hand [_]
  (let [cards (rf/subscribe [::game/hand])
        can-play? (rf/subscribe [::game/can-play?])]
    (fn [_]
      [hand {:cards @cards
             :disabled? (not @can-play?)
             :on-play #(rf/dispatch [::game/play %])}])))

;; TODO just for demo :)
(comment
  (defn card-deco []
    [mui/grid
     {:container true}
     [mui/grid
      {:item true
       :style {:transform "translate(20px, -10px) rotate(-7deg)"
               :z-index 0}}
      [playing-card {:card [:bells :deuce]}]]
     [mui/grid
      {:item true
       :style {:transform "translate(0px, -20px)"
               :z-index 10}}
      [playing-card {:card [:acorns :ober]
                     :elevation 5}]]
     [mui/grid
      {:item true
       :style {:transform "translate(-20px, -10px) rotate(7deg)"
               :z-index 3}}
      [playing-card {:card [:acorns 7]}]]]))

;; TODO: Use "component names" for these, to avoid confusing
;; with related data (card' trick')!
(defn trick [{:keys [trick]}]
  [mui/grid
   {:container true
    :direction :row
    :spacing 1}
   (for [card trick]
     ^{:key (card-key card)}
     [mui/grid
      {:item true}
      [playing-card {:card card}]])])

(defn prev-trick-button []
  (let [prev-trick (rf/subscribe [::game/prev-trick])
        open? (r/atom false)
        toggle-open (fn [] (swap! open? #(and (not %)
                                              (some? @prev-trick))))]
    (fn [_]
      [:<>
       [mui/button
        {:disabled (nil? @prev-trick)
         :on-click toggle-open}
        "Letzter Stich"]
       [mui/backdrop
        {:open @open?
         :style {:z-index 100} ; TODO - calculate from theme
         :on-click toggle-open}
        (when @open?
          [:div
           [trick {:trick @prev-trick}]])]])))

(defn player-tricks-detail []
  (let [tricks (rf/subscribe [::game/tricks])]
    (fn [_]
      [:div
       {:style {:height "100vh"
                :overflow :scroll}}
       (for [[i trick'] (map vector (range) @tricks)]
         ^{:key i}
         [trick {:trick trick'}])])))

(defn player-tricks []
  (let [open? (r/atom false)
        toggle-open #(swap! open? not)]
    (fn [_]
      [:<>
       [mui/button
        {:disabled @open?
         :on-click toggle-open}
        "Meine Stiche ansehen"]
       [mui/backdrop
        {:open @open?
         :style {:z-index 100} ; TODO - calculate from theme
         :on-click toggle-open}
        (when @open?
          [player-tricks-detail])]])))

(defn active-trick []
  (let [trick' (rf/subscribe [::game/active-trick])]
    [mui/grid
     {:container true
      :direction :column
      :justify :center
      :align-items :center
      :spacing 2}
     [mui/grid
      {:item true}
      [trick {:trick @trick'}]]]))

(defn center []
  (let [started? @(rf/subscribe [::game/started?])
        can-start? @(rf/subscribe [::game/can-start?])
        tricks-visible? @(rf/subscribe [::game/can-see-tricks?])
        can-score? @(rf/subscribe [::score/can-score?])
        can-start-next? @(rf/subscribe [::game/can-start-next?])]
    (cond
      can-start-next?
      [mui/button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [::game/start-next])}
       "Nächstes Spiel"]

      tricks-visible?
      [:div
       [player-tricks]
       (when can-score?
         [score-button
          {:variant :contained
           :color :primary}])]

      started?
      [active-trick]

      can-start?
      [mui/button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [::game/start])}
       "Spiel starten"]

      :else
      [:<>
       [mui/circular-progress]
       [:p "Warten auf weitere Teilnehmer..."]])))

(defn peer-info-area []
  (with-let [left-seat (rf/subscribe [::game/left-seat])
             across-seat (rf/subscribe [::game/across-seat])
             right-seat (rf/subscribe [::game/right-seat])]
    [mui/grid
     {:item true
      :container true
      :justify :space-evenly
      :wrap "nowrap"}
     [mui/grid
      {:item true}
      [peer-info {:seat @left-seat}]]
     [mui/grid
      {:item true}
      [peer-info {:seat @across-seat}]]
     [mui/grid
      {:item true}
      [peer-info {:seat @right-seat}]]]))

(let [use-styles (make-styles {:root {:min-height "100vh"}})]
  (defn game-screen* []
    (let [classes (use-styles)]
      [mui/grid
       {:classes classes
        :container true
        :direction :column
        :justify :space-between}
       
       [mui/grid
        {:item true}
        [game-bar]]
       
       [peer-info-area]

       [mui/grid
        {:item true
         :container true
         :justify :center
         :align-items :center
         :xs true}
        [mui/grid
         {:item true}
         [center]]]

       [mui/grid
        {:item true}
        [player-hand]]

       [mui/grid
        {:item true}
        [player-bar]]])))

(defn game-screen [_]
  [:f> game-screen*])
