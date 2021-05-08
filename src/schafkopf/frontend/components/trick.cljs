(ns schafkopf.frontend.components.trick
  (:require [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.styles :refer [make-styles]]

            [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]))

(defn linear-trick [{:keys [cards]}]
  [grid
   {:container true
    :direction :row
    :spacing 1}
   (for [card cards]
     ^{:key (card-key card)}
     [grid
      {:item true}
      [playing-card {:card card}]])])

(def use-styles
  (make-styles
   (-> {:root {:position :relative
               :width 112
               :height 200}
        :card {:position :absolute
               :top 0
               :left 0}
        :pos-0 {:transform "translate(0px, 50px)"}
        :pos-1 {:transform "translate(-84px, 0px)"}
        :pos-2 {:transform "translate(0px, -50px)"}
        :pos-3 {:transform "translate(84px, 0)"}}
       (into (map #(vector (keyword (str "index-" %))
                           {:z-index (inc %)})
                  (range 4))))
   {:name "trick"}))

(defn stacked-trick*
  "Displays a trick as a stack of cards.  Seats are relative to
   the player, i.e. seat 0 is on the bottom, seat 1 on the left,
   and so on."
  [{:keys [cards lead]}]
  (let [classes (use-styles)]
    [:div
     {:class (:root classes)}
     (doall
      (for [[i card] (map-indexed vector cards)]
        (let [pos (rem (+ i lead) 4)
              index-class (keyword (str "index-" i))
              pos-class (keyword (str "pos-" pos))]
          ^{:key (card-key card)}
          [:div
           {:class (mapv classes [:card index-class pos-class])}
           [playing-card {:card card
                          :elevation (inc i)}]])))]))

(defn stacked-trick [props]
  [:f> stacked-trick* props])
