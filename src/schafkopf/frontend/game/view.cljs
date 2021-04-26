(ns schafkopf.frontend.game.view
  (:require [re-frame.core :as rf]
            [mui-bien.core :as mui]
            [schafkopf.frontend.game.core :as game]))

(defn card-url [[suit rank]]
  (str "/img/decks/saxonian/" (name suit) "-"
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

(defn game-screen []
  (let [code @(rf/subscribe [::game/code])]
    [:<>
     [:h1 "Spiel."]
     [:p "Code: " code]
     [mui/grid
      {:container true}
      [mui/grid
       {:item true
        :style {:transform "translate(20px, 10px) rotate(-7deg)"
                :z-index 0}}
       [card {:card [:bells :deuce]}]]
      [mui/grid
       {:item true
        :style {:z-index 10}}
       [card {:card [:acorns :ober]
              :elevation 5}]]
      [mui/grid
       {:item true
        :style {:transform "translate(-20px, 10px) rotate(7deg)"
                :z-index 3}}
       [card {:card [:acorns 7]}]]]]))
