(ns schafkopf.frontend.game.core
  (:require [re-frame.core :as rf]
            [schafkopf.game :as g]
            [schafkopf.protocol :as p]))

;;;; Re-frame helpers

(defn reg-event-chsk
  "Registers a re-frame event handler that returns a chsk-send effect.
   The event to send is automatically enriched by game-id and seqno."
  [id handler-fn]
  (rf/reg-event-fx
   id
   (fn [{{{:server/keys [game-id seqno]} ::game} :db
         :as coeffects}
        event]
     (let [[ev-id ?args] (handler-fn coeffects event)]
       {:chsk-send [ev-id (into [game-id seqno] ?args)]}))))

;;;; Server event handlers

(rf/reg-event-db
 :server/update
 (fn [db [_ game]]
   (assoc db ::game game)))

(rf/reg-event-fx
 :server/stop
 (fn [db _]
   ;; TODO: Display message to the user before removing the game state!
   {:db (dissoc db ::game)
    :chsk-disconnect nil}))

;;;; UI event handlers

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_ game]]
   {:db (assoc db ::game game)
    :chsk-connect nil}))

(reg-event-chsk
 ::start
 (fn [_ _]
   [:client/start]))

(reg-event-chsk
 ::skip
 (fn [_ _]
   [:client/skip]))

(reg-event-chsk
 ::play
 (fn [_ [_ card]]
   [:client/play [card]]))

(reg-event-chsk
 ::take
 (fn [_ _]
   [:client/take]))

(reg-event-chsk
 ::start-next
 (fn [_ _]
   [:client/next]))

(reg-event-chsk
 ::undo
 (fn [_ _]
   [:client/undo]))

;;;; Game subscriptions

(rf/reg-sub
 ::game
 (fn [db _]
   (::game db)))

(rf/reg-sub
 ::joined?
 :<- [::game]
 (fn [game _]
   (some? game)))

(rf/reg-sub
 ::started?
 :<- [::game]
 (fn [game _]
   (g/started? game)))

(rf/reg-sub
 ::join-code
 :<- [::game]
 (fn [game _]
   (:server/join-code game)))

(rf/reg-sub
 ::number
 :<- [::game]
 (fn [{:game/keys [number]} _]
   (inc number)))

(rf/reg-sub
 ::round
 :<- [::game]
 (fn [game _]
   (inc (g/round game))))

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

(rf/reg-sub
 ::prev-trick
 :<- [::game]
 (fn [game _]
   (:game/prev-trick game)))

(rf/reg-sub
 ::pot
 :<- [::game]
 (fn [game _]
   (:game/pot game)))

(rf/reg-sub
 ::pot-score
 :<- [::game]
 (fn [game _]
   (:game/pot-score game)))

;;;; Player subscriptions

(rf/reg-sub
 ::seat
 :<- [::game]
 (fn [game _]
   (:player/seat game)))

(rf/reg-sub
 ::active?
 :<- [::seat]
 :<- [::active-seat]
 (fn [[seat active-seat] _]
   (= seat active-seat)))

(rf/reg-sub
 ::name
 :<- [::game]
 (fn [game _]
   (:client/name game)))

(rf/reg-sub
 ::balance
 :<- [::game]
 (fn [game _]
   (:player/balance game)))

(rf/reg-sub
 ::trick-count
 :<- [::game]
 (fn [game _]
   (g/trick-count game)))

;; TODO: Sorting :)
(rf/reg-sub
 ::hand
 :<- [::game]
 (fn [game _]
   (:player/hand game)))

(rf/reg-sub
 ::points
 :<- [::game]
 (fn [game _]
   (:player/points game)))

(rf/reg-sub
 ::score
 :<- [::game]
 (fn [game _]
   (:player/score game)))

(rf/reg-sub
 ::can-see-tricks?
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
 ::peer-present?
 subscribe-peer
 (fn [{:client/keys [name]} _]
   (some? name)))

(rf/reg-sub
 ::peer-name
 subscribe-peer
 (fn [peer _]
   (:client/name peer)))

(rf/reg-sub
 ::peer-balance
 subscribe-peer
 (fn [peer _]
   (:player/balance peer)))

(rf/reg-sub
 ::peer-hand-count
 subscribe-peer
 (fn [peer _]
   (g/hand-count peer)))

(rf/reg-sub
 ::peer-trick-count
 subscribe-peer
 (fn [peer _]
   (g/trick-count peer)))

(rf/reg-sub
 ::peer-points
 subscribe-peer
 (fn [peer _]
   (:player/points peer)))

(rf/reg-sub
 ::peer-score
 subscribe-peer
 (fn [peer _]
   (:player/score peer)))

(rf/reg-sub
 ::peer-tricks
 subscribe-peer
 (fn [peer _]
   (:player/tricks peer)))

(rf/reg-sub
 ::peer-tricks-visible?
 subscribe-peer
 (fn [peer _]
   (some? (:player/tricks peer))))

;;;; Action subscriptions

(rf/reg-sub
 ::can-start?
 :<- [::game]
 (fn [game _]
   (p/can-start? game)))

;; TODO: Better logic?
(rf/reg-sub
 ::can-skip?
 :<- [::game]
 (fn [game _]
   (and (g/started? game)
        (empty? (:game/active-trick game))
        (nil? (:player/tricks game))
        (zero? (g/tricks-taken game)))))

(rf/reg-sub
 ::can-play?
 :<- [::game]
 (fn [game _]
   (and (g/player-turn? game (:player/seat game))
        (not (g/trick-complete? game)))))

(rf/reg-sub
 ::can-take?
 :<- [::game]
 (fn [game _]
   (g/trick-complete? game)))

(rf/reg-sub
 ::can-start-next?
 :<- [::game]
 (fn [game]
   (g/scored? game)))

(rf/reg-sub
 ::can-undo?
 :<- [::game]
 (fn [game]
   (:server/undo? game)))
