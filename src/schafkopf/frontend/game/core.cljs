(ns schafkopf.frontend.game.core
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 ::join
 (fn [_ _]
   {:chsk/connect nil}))
