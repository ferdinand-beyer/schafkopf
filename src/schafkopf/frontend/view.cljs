(ns schafkopf.frontend.view
  (:require [re-frame.core :as rf]
            [mui-bien.core :as mui]
            [schafkopf.frontend.auth.view :as auth-view]
            [schafkopf.frontend.game.core :as game]
            [schafkopf.frontend.game.view :as game-view]))

(defn root []
  (let [code @(rf/subscribe [::game/code])]
    [:<>
     [mui/css-baseline]
     (if (some? code)
       [game-view/game-screen]
       [auth-view/auth-screen])]))
