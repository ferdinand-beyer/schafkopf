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

#_(defn peer [{:keys [seat]}]
  (let [name (rf/subscribe [::game/peer-name seat])]
    (fn [_]
      [mui/card
       [:p "Seat: " seat]
       [:p "Name: " @name]])))

(def peer
  (mui/with-styles
   {:root {}}
   (fn [{:keys [classes seat]}]
     (let [name (rf/subscribe [::game/peer-name seat])]
       (fn [_]
         [mui/card
          {:classes {:root (:root classes)}}
          [:p "Seat: " seat]
          [:p "Name: " @name]])))))

(defn card-deco []
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

(defn center []
  (let [code (rf/subscribe [::game/code])]
    [:div "Code: " @code]))

(def game-screen
  (mui/with-styles
   {:root {:width "100vw"
           :height "100vh"
           :display :flex
           :flex-direction :column
           :justify-content :space-between
           :align-content :space-between}
    :horiz {:display :flex
            :flex-direction :row
            :justify-content :space-between
            :align-content :space-between}
    :north {}
    :east {}
    :south {}
    :west {}}
   (fn [{:keys [classes]}]
     [:div {:class (:root classes)}
      [:div {:class (:north classes)} [across]]
      [:div {:class (:horiz classes)}
       [:div {:class (:west classes)} [left]]
       [:div {:class (:center classes)} [center]]
       [:div {:class (:east classes)} [right]]]
      [:div {:class (:south classes)}]])))
