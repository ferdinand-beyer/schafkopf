(ns schafkopf.frontend.view
  (:require [re-frame.core :as rf]
            ;; TODO require MUI components selectively!
            [mui-bien.core.all :as mui]
            [schafkopf.frontend.auth.view :as auth-view]
            [schafkopf.frontend.game.core :as game]
            [schafkopf.frontend.game.view :as game-view]))

(defn root []
  (let [active? @(rf/subscribe [::game/active?])]
    [:<>
     [mui/css-baseline]
     (if active?
       [game-view/game-screen]
       [auth-view/auth-screen])]))
