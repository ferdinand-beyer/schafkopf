(ns schafkopf.frontend.game.view
  (:require [re-frame.core :as rf]
            [mui-bien.core :as mui]
            [schafkopf.frontend.game.core :as game]))

(defn card-url [[suit rank]]
  (str "/assets/img/decks/saxonian/" (name suit) "-"
       (if (keyword? rank) (name rank) rank)
       ".jpg"))

(def card
  (mui/with-styles
    {:root {:width 140
            :height 250
            :display :flex
            :border-radius 12
            :background-size :cover}}
   (fn [{:keys [classes card elevation children]
         :or {card [:hearts :king]
              elevation 1}}]
     [mui/card
      {:elevation elevation
       :classes {:root (:root classes)}
       :style {:background-image (str "url('" (card-url card) "')")}}
      children])))

(def peer
  (mui/with-styles
   {:root {}}
   (fn [{:keys [classes seat]}]
     (let [name (rf/subscribe [::game/peer-name seat])]
       (fn [_]
         [mui/card
          {:classes {:root (:root classes)}}
          (if (some? @name)
            [mui/typography {:variant :h6} @name]
            [mui/typography "(Unbesetzt)"])])))))

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
    [peer {:seat @seat}]))

(defn center []
  [:<>
   [mui/circular-progress]
   [:p "Warten auf weitere Teilnehmer..."]])

(defn game-info []
  (let [code (rf/subscribe [::game/code])]
    [:<>
     [mui/typography
      {:variant :h5}
      "Schafkopf"]
     [mui/typography "Raumcode: " [:strong @code]]]))

(def game-screen
  (mui/with-styles
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
         [card-deco]]
        [mui/grid
         {:item true
          :xs 2}
         [mui/paper "se"]]]])))