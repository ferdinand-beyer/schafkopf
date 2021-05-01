;; TODO: Split into multiple namespaces!
(ns schafkopf.frontend.game.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]

            ;; TODO require MUI components selectively!
            [mui-bien.core.all :as mui]
            [mui-bien.core.styles :refer [with-styles]]

            [schafkopf.frontend.game.core :as game]
            [schafkopf.frontend.game.score :as score]

            [schafkopf.frontend.game.views.scoring :refer [score-button]]))

(def suit-names {:acorns "Eichel"
                 :leaves "Gras"
                 :hearts "Herz"
                 :bells "Schellen"})

(def rank-names {:unter "Unter"
                 :ober "Ober"
                 :king "König"
                 :deuce "Daus"})

(defn card-key [[rank suit]]
  (str (name suit) "-" (if (keyword? rank) (name rank) rank)))

(defn card-name [[rank suit]]
  (str (get suit-names suit) " " (get rank-names rank rank)))

(defn card-url [card]
  (str "/assets/img/decks/saxonian/" (card-key card) ".jpg"))

;; Separate component since with-styles screws up clojure types,
;; such as the 'card' prop which is a vector of keywords.
(def -card
  (with-styles
    {:root {:border-radius 12
            :width 140
            :height 250
            :background-size :cover
            :position :relative}
     :fill {:width "100%"
            :height "100%"}}
   (fn [{:keys [classes name url elevation
                disabled onClick
                children]
         :or {elevation 1
              disabled true}}]
     [mui/card
      {:elevation elevation
       :classes {:root (:root classes)}
       :style {:background-image (str "url('" url "')")}}
      [mui/tooltip
       {:title name
        :arrow true}
       ;; Wrapper since tooltips won't work on disabled buttons.
       [:div {:class (:fill classes)}
        [mui/button-base
         {:class (:fill classes)
          :focus-ripple true
          :disabled disabled
          :on-click onClick}
         children]]]])))

(defn card [{:keys [card] :as props}]
  (let [name (card-name card)
        url (card-url card)]
    [-card (merge props {:name name
                         :url url})]))

(def hand
  (with-styles
    {:root {:display :flex
            :flex-direction :row}}
    (fn [{:keys [classes]}]
      (let [hand (rf/subscribe [::game/hand])
            can-play? (rf/subscribe [::game/can-play?])]
        (fn [_]
          [:div
           {:class (:root classes)}
           (doall
            (for [card' @hand]
              ^{:key (card-key card')}
              [card {:card card'
                     :disabled (not @can-play?)
                     :on-click #(rf/dispatch [::game/play card'])}]))])))))

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
          
          [mui/typography "Saldo:" @balance]
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
(defn card-deco []
  [mui/grid
   {:container true}
   [mui/grid
    {:item true
     :style {:transform "translate(20px, -10px) rotate(-7deg)"
             :z-index 0}}
    [card {:card [:bells :deuce]}]]
   [mui/grid
    {:item true
     :style {:transform "translate(0px, -20px)"
             :z-index 10}}
    [card {:card [:acorns :ober]
           :elevation 5}]]
   [mui/grid
    {:item true
     :style {:transform "translate(-20px, -10px) rotate(7deg)"
             :z-index 3}}
    [card {:card [:acorns 7]}]]])

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

(def -trick
  (with-styles
    {}
    (fn [{:keys [classes children]}]
      [mui/grid
       {:container true
        :direction :row}
       (for [[i card] (map vector (range) children)]
         ^{:key i}
         [mui/grid {:item true} card])])))

;; TODO: Use "component names" for these, to avoid confusing
;; with related data (card' trick')!
(defn trick [{:keys [trick]}]
  [-trick
   (for [card' trick]
     ^{:key (card-key card')}
     [card {:card card'}])])

(defn player-tricks-detail []
  (let [tricks (rf/subscribe [::game/tricks])]
    (fn [_]
      [:div
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
        can-take? (rf/subscribe [::game/can-take?])]
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
         "Stich nehmen"]])]))

(defn center []
  (let [started? @(rf/subscribe [::game/started?])
        can-start? @(rf/subscribe [::game/can-start?])
        tricks-visible? @(rf/subscribe [::game/can-see-tricks?])
        can-score? @(rf/subscribe [::score/can-score?])
        can-start-next? @(rf/subscribe [::game/can-start-next?])]
    (cond
      can-start-next?
      [mui/button "Nächstes Spiel"]

      tricks-visible?
      [:div
       [player-tricks]
       (when can-score?
         [score-button])]

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
  (let [code (rf/subscribe [::game/code])]
    [:<>
     [mui/typography
      {:variant :h5}
      "Schafkopf"]
     [mui/typography "Raumcode: " [:strong @code]]]))

(def game-screen
  (with-styles
    {:root {:min-height "100vh"}}

    (fn [{:keys [classes]}]
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
          :xs 4}
         [across]]
        [mui/grid
         {:item true
          :xs 2}
         [mui/paper "nw"]]]
       
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
         {:item true}
         [hand]]
        [mui/grid
         {:item true
          :xs 2}
         [mui/paper "se"]]]])))
