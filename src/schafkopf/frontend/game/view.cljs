;; TODO: Split into multiple namespaces!
(ns schafkopf.frontend.game.view
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]

   ;; TODO require MUI components selectively!
   [mui-bien.core.all :as mui]
   [mui-bien.core.styles :refer [make-styles]]

   [schafkopf.frontend.game.core :as game]
   [schafkopf.frontend.game.score :as score]

   [schafkopf.frontend.components.hand :refer [hand]]
   [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]

   [schafkopf.frontend.game.views.scoring :refer [score-button]]))

(defn player-hand [_]
  (let [cards (rf/subscribe [::game/hand])
        can-play? (rf/subscribe [::game/can-play?])]
    (fn [_]
      [hand {:cards @cards
             :disabled? (not @can-play?)
             :on-play #(rf/dispatch [::game/play %])}])))

;; TODO: Avatar
(defn peer [{:keys [classes seat]}]
  (let [name (rf/subscribe [::game/peer-name seat])
        dealer? (rf/subscribe [::game/peer-dealer? seat])
        active? (rf/subscribe [::game/peer-active? seat])
        balance (rf/subscribe [::game/peer-balance seat])
        hand-count (rf/subscribe [::game/peer-hand-count seat])
        trick-count (rf/subscribe [::game/peer-trick-count seat])
        tricks-visible? (rf/subscribe [::game/peer-tricks-visible? seat])
        points (rf/subscribe [::game/peer-points seat])
        score (rf/subscribe [::game/peer-score seat])]
    (fn [_]
      [mui/card
       {:classes {:root (:root classes)}}
       (if (some? @name)
         [:<>
          [mui/typography {:variant :h6} @name]
          (when @dealer?
            [mui/typography "Geber"])
          (when @active?
            [mui/typography "An der Reihe"])
          
          [mui/typography "Saldo: " @balance]
          [mui/typography "Karten: " @hand-count]
          [mui/typography "Stiche: " @trick-count]
          (when @tricks-visible?
            [mui/button "Stiche ansehen"])
          (when @points
            [mui/typography "Punkte: " @points])
          (when @score
            [mui/typography "Bewertung: " @score])]
         [mui/typography "(Unbesetzt)"])])))

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

(defn left []
  (let [seat (rf/subscribe [::game/left-seat])]
    [peer {:seat @seat}]))

(defn right []
  (let [seat (rf/subscribe [::game/right-seat])]
    [peer {:seat @seat}]))

(defn across []
  (let [seat (rf/subscribe [::game/across-seat])]
    [peer {:seat @seat}]))

(defn self []
  (let [seat (rf/subscribe [::game/seat])]
    ;; TODO: This will not work once we have :player/self
    ;; need to get this info from the game itself, or using
    ;; a smart subscription.
    [peer {:seat @seat}]))

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
  (let [trick' (rf/subscribe [::game/active-trick])
        can-take? (rf/subscribe [::game/can-take?])
        can-skip? (rf/subscribe [::game/can-skip?])]
    [mui/grid
     {:container true
      :direction :column
      :justify :center
      :align-items :center
      :spacing 2}
     [mui/grid
      {:item true}
      [trick {:trick @trick'}]]
     (when @can-take?
       [mui/grid
        {:item true}
        [mui/button
         {:variant :contained
          :color :primary
          :on-click #(rf/dispatch [::game/take])}
         "Stich nehmen"]])
     (when @can-skip?
       [mui/grid
        {:item true}
        [mui/button
         {:variant :contained
          :color :primary
          :on-click #(rf/dispatch [::game/skip])}
         "Zusammenwerfen"]])]))

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

(defn game-info []
  (let [join-code (rf/subscribe [::game/join-code])
        number (rf/subscribe [::game/number])
        round (rf/subscribe [::game/round])]
    [:<>
     [mui/typography
      {:variant :h5}
      "Schafkopf"]
     [mui/typography "Raumcode: " [:strong @join-code]]
     [mui/typography "Spiel: " @number]
     [mui/typography "Runde: " @round]
     [prev-trick-button]]))

(let [use-styles (make-styles {:root {:min-height "100vh"}})]
  (defn game-screen* []
    (let [classes (use-styles)]
      [mui/grid
       {:classes classes
        :container true
        :direction :column
        :justify :space-between}

       ;; Top Row
       [mui/grid
        {:item true
         :container true
         :justify :space-between}
        [mui/grid
         {:item true
          :xs 2}
         [game-info]]
        [mui/grid
         {:item true
          :xs 2}
         [across]]
        [mui/grid
         {:item true}
         [mui/button
          {:disabled (not @(rf/subscribe [::game/can-undo?]))
           :on-click #(rf/dispatch [::game/undo])}
          "Rückgängig"]]]

       ;; Middle Row
       [mui/grid
        {:item true
         :container true
         :justify :space-between
         :align-items :center}
        [mui/grid
         {:item true
          :xs 2}
         [left]]
        [mui/grid
         {:item true}
         [center]]
        [mui/grid
         {:item true
          :xs 2}
         [right]]]

       ;; Bottom Row
       [mui/grid
        {:item true
         :container true
         :justify :space-between
         :align-items :flex-end}
        [mui/grid
         {:item true
          :xs 2}
         [self]]
        [mui/grid
         {:item true
          :xs 10}
         [player-hand]]]])))

(defn game-screen [_]
  [:f> game-screen*])
