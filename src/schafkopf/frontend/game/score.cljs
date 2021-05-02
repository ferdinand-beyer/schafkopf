(ns schafkopf.frontend.game.score
  (:require [re-frame.core :as rf]
            [schafkopf.game :as g]
            [schafkopf.frontend.util :refer [parse-int]]
            [schafkopf.frontend.game.core :as gdb]))

(def initial-scores (vec (repeat 5 nil)))

(def interceptors [(rf/path ::data)])

(defn scores [raw-scores]
  (mapv #(parse-int % 0) raw-scores))

;;;; Event handlers

(rf/reg-event-db
 ::toggle
 interceptors
 (fn [db _]
   (-> db
       (update :scoring? not)
       (assoc :raw-scores initial-scores)
       (dissoc :error))))

(rf/reg-event-db
 ::change
 interceptors
 (fn [db [_ seat score]]
   (-> db
       (update :raw-scores assoc seat score)
       (dissoc :error))))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   (let [scores (scores (get-in db [::data :raw-scores]))
         game-score {:game.score/players (vec (butlast scores))
                     :game.score/pot (last scores)}
         game (::gdb/game db)]
     (if (g/valid-score? game game-score)
       (let [{:server/keys [game-id seqno]} game]
         {:db (update db ::data assoc
                      :error nil
                      :scoring? false)
          :chsk-send [:client/score [game-id seqno game-score]]})
       {:db (update db ::data assoc
                    :error "Ungültige Bewertung!")}))))

;;;; Subscriptions

(rf/reg-sub
 ::can-score?
 :<- [::gdb/game]
 (fn [game _]
   (g/can-score? game)))

(rf/reg-sub
 ::-data
 (fn [db]
   (::data db)))

(rf/reg-sub
 ::scoring?
 :<- [::-data]
 (fn [db _]
   (boolean (:scoring? db))))

(rf/reg-sub
 ::error
 :<- [::-data]
 (fn [db _]
   (:error db)))

(rf/reg-sub
 ::raw-scores
 :<- [::-data]
 (fn [db _]
   (:raw-scores db)))

(rf/reg-sub
 ::sum
 :<- [::raw-scores]
 (fn [raw-scores _]
   (reduce + (scores raw-scores))))

(rf/reg-sub
 ::pot-name
 (fn [_]
   "Stock"))

(rf/reg-sub
 ::name
 (fn [[_ seat]]
   (rf/subscribe (if (< seat 4)
                   [::gdb/peer-name seat]
                   [::pot-name])))
 identity)

(rf/reg-sub
 ::score
 :<- [::raw-scores]
 (fn [scores [_ seat]]
   (nth scores seat)))
