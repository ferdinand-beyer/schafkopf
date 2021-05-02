(ns schafkopf.frontend.view
  (:require [re-frame.core :as rf]
            [mui-bien.core.css-baseline :refer [css-baseline]]
            [mui-bien.core.backdrop :refer [backdrop]]
            [mui-bien.core.circular-progress :refer [circular-progress]]

            [schafkopf.frontend.db :as db]
            [schafkopf.frontend.auth.view :as auth-view]
            [schafkopf.frontend.game.core :as game]
            [schafkopf.frontend.game.view :as game-view]))

(defn loading-curtain [{:keys [open?]}]
  [backdrop
   {:open (boolean open?)}
   [circular-progress]])

(defn main []
  (let [game-joined? @(rf/subscribe [::game/joined?])]
    (if game-joined?
      [game-view/game-screen]
      [auth-view/auth-screen])))

(defn root []
  (let [initializing? @(rf/subscribe [::db/initializing?])]
    [:div
     [css-baseline]
     [loading-curtain {:open? initializing?}]
     (when-not initializing?
       [main])]))
