(ns schafkopf.frontend.game.core
  (:require [re-frame.core :as rf]
            [schafkopf.game :as game]
            [schafkopf.protocol :as p]))

;;;; Server event handlers

(rf/reg-event-db
 :game/update
 (fn [db [_ game]]
   (assoc db ::game game)))

;;;; UI event handlers

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_ game]]
   {:db (assoc db ::game game)
    :chsk/connect nil}))

(rf/reg-event-fx
 ::start
 (fn [{:keys [db]} _]
   (let [{:server/keys [code seqno]} (::game db)]
     {:chsk/send [:game/start [code seqno]]})))

(rf/reg-event-fx
 ::play
 (fn [{:keys [db]} [_ card]]
   (let [{:server/keys [code seqno]} (::game db)]
     {:chsk/send [:client/play [code seqno card]]})))

(rf/reg-event-fx
 ::take
 (fn [{:keys [db]} _]
   (let [{:server/keys [code seqno]} (::game db)]
     {:chsk/send [:client/take [code seqno]]})))

;;;; Game subscriptions

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
 ::dealer-seat
 :<- [::game]
 (fn [game _]
   (:game/dealer-seat game)))

(rf/reg-sub
 ::active-seat
 :<- [::game]
 (fn [game _]
   (:game/active-seat game)))

(rf/reg-sub
 ::active-trick
 :<- [::game]
 (fn [game _]
   (:game/active-trick game)))

;;;; Player subscriptions

(rf/reg-sub
 ::seat
 :<- [::game]
 (fn [game _]
   (:player/seat game)))

;; TODO: Sorting :)
(rf/reg-sub
 ::hand
 :<- [::game]
 (fn [game _]
   (:player/hand game)))

(rf/reg-sub
 ::tricks-visible?
 :<- [::game]
 (fn [game _]
   (some? (:player/tricks game))))

(rf/reg-sub
 ::tricks
 :<- [::game]
 (fn [game _]
   (:player/tricks game)))

;;;; Seat subscriptions

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

;;;; Peer subscriptions

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

(rf/reg-sub
 ::peer-active?
 :<- [::active-seat]
 (fn [active-seat [_ seat]]
   (= active-seat seat)))

(rf/reg-sub
 ::peer-dealer?
 :<- [::dealer-seat]
 (fn [dealer-seat [_ seat]]
   (= dealer-seat seat)))

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

(rf/reg-sub
 ::peer-trick-count
 subscribe-peer
 (fn [peer _]
   (game/trick-count peer)))

(rf/reg-sub
 ::peer-points
 subscribe-peer
 (fn [peer _]
   (:player/points peer)))

(rf/reg-sub
 ::peer-tricks-visible?
 subscribe-peer
 (fn [peer _]
   (some? (:player/tricks peer))))

;;;; Action subscriptions

(rf/reg-sub
 ::can-play?
 :<- [::game]
 (fn [game _]
   (and (game/player-turn? game (:player/seat game))
        (not (game/trick-complete? game)))))

(rf/reg-sub
 ::can-take?
 :<- [::game]
 (fn [game _]
   (game/trick-complete? game)))
