(ns schafkopf.frontend.game.view
  (:require [re-frame.core :as rf]
            [schafkopf.frontend.game.core :as game]))

(defn game-screen []
  (let [code @(rf/subscribe [::game/code])]
    [:<>
     [:h1 "Spiel."]
     [:p "Code: " code]]))