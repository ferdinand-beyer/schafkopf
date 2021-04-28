(ns schafkopf.frontend.game.view
  (:require [re-frame.core :as rf]
            ;; TODO require MUI components selectively!
            [mui-bien.core.all :as mui]
            [mui-bien.core.styles :refer [with-styles]]
            [schafkopf.frontend.game.core :as game]))

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
   (fn [{:keys [classes name url elevation children]
         :or {elevation 1}}]
     [mui/card
      {:elevation elevation
       :classes {:root (:root classes)}
       :style {:background-image (str "url('" url "')")}}
      [mui/tooltip
       {:title name
        :arrow true}
       ;; Wrapper since tooltips won't work on disabled buttons.
       [:div {:class (:fill classes)}
        ;; TODO: Pass 'disabled' and 'on-click' down to the button.
        [mui/button-base
         {:class (:fill classes)
          :focus-ripple true}
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
      (let [hand (rf/subscribe [::game/hand])]
        (fn [_]
          [:div {:class (:root classes)}
           (for [card' @hand]
             ^{:key (card-key card')}
             [card {:card card'}])])))))

;; TODO: Avatar, hand, tricks, score, total score
(def peer
  (with-styles
   {:root {}}
   (fn [{:keys [classes seat]}]
     (let [name (rf/subscribe [::game/peer-name seat])
           hand-count (rf/subscribe [::game/peer-hand-count seat])]
       (fn [_]
         [mui/card
          {:classes {:root (:root classes)}}
          (if (some? @name)
            [:<>
             [mui/typography {:variant :h6} @name]
             [mui/typography "Karten: " @hand-count]]
            [mui/typography "(Unbesetzt)"])])))))

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

(defn center []
  (let [started? @(rf/subscribe [::game/started?])
        can-start? @(rf/subscribe [::game/can-start?])]
    (cond
      started?
      [:p "(Active game)"]

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
