(ns schafkopf.frontend.game.core
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 ::join
 (fn [{:keys [db]} [_ code]]
   {:db (assoc db ::code code)
    :chsk/connect nil}))

(rf/reg-sub
 ::code
 (fn [db]
   (::code db)))
