(ns schafkopf.frontend.game.core
  (:require [re-frame.core :as rf]
            [schafkopf.game :as game]
            [schafkopf.protocol :as p]))

;;;; Server events

(rf/reg-event-db
 :game/update
 (fn [db [_ game]]
   (assoc db ::game game)))

;;;; UI events

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_ game]]
   {:db (assoc db ::game game)
    :chsk/connect nil}))

(rf/reg-event-fx
 ::start
 (fn [{:keys [db]} _]
   (let [code (get-in db [::game :server/code])]
     {:chsk/send [:game/start code]})))

;;;; Subscriptions

(rf/reg-sub
 ::game
 (fn [db _]
   (::game db)))

(rf/reg-sub
 ::active?
 :<- [::game]
 (fn [game _]
   (some? game)))

(rf/reg-sub
 ::started?
 :<- [::game]
 (fn [game _]
   (game/started? game)))

(rf/reg-sub
 ::can-start?
 :<- [::game]
 (fn [game _]
   (p/can-start? game)))

(rf/reg-sub
 ::code
 :<- [::game]
 (fn [game _]
   (:server/code game)))

(rf/reg-sub
 ::seat
 :<- [::game]
 (fn [game _]
   (:player/seat game)))

(defn rotate-seat [seat offset]
  (rem (+ seat offset) 4))

(defn seat-fn [offset]
  (fn [seat _]
    (rotate-seat seat offset)))

(rf/reg-sub
 ::left-seat
 :<- [::seat]
 (seat-fn 1))

(rf/reg-sub
 ::across-seat
 :<- [::seat]
 (seat-fn 2))

(rf/reg-sub
 ::right-seat
 :<- [::seat]
 (seat-fn 3))

(rf/reg-sub
 ::peers
 :<- [::game]
 (fn [game _]
   (:player/peers game)))

(rf/reg-sub
 ::peer
 :<- [::peers]
 (fn [peers [_ seat]]
   (get peers seat)))

(defn subscribe-peer [[_ seat]]
  (rf/subscribe [::peer seat]))

(rf/reg-sub
 ::peer-name
 subscribe-peer
 (fn [peer _]
   (:client/name peer)))

(rf/reg-sub
 ::peer-hand-count
 subscribe-peer
 (fn [peer _]
   (game/hand-count peer)))
