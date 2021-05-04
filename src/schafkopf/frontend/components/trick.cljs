(ns schafkopf.frontend.components.trick
  (:require [mui-bien.core.grid :refer [grid]]

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

;; TODO
(def stacked-trick linear-trick)
