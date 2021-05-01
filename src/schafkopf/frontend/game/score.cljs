(ns schafkopf.frontend.game.score
  (:require [re-frame.core :as rf]
            [schafkopf.game :as g]
            [schafkopf.frontend.util :refer [parse-int]]
            [schafkopf.frontend.game.core :as gdb]))

(def initial-score (vec (repeat 5 nil)))

(def interceptors [(rf/path ::data)])

;;;; Event handlers

(rf/reg-event-db
 ::toggle
 interceptors
 (fn [db _]
   (-> db
       (update :scoring? not)
       (dissoc :scores :error))))

(rf/reg-event-db
 ::change
 interceptors
 (fn [db [_ seat score]]
   (update db :scores (fnil assoc initial-score) seat score)))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   (let [scores (mapv #(parse-int % 0) (get-in db [::data :scores]))
         game-score {:game.score/players (vec (butlast scores))
                     :game.score/pot (last scores)}]
     (if (g/valid-score? game-score)
       (let [{:server/keys [code seqno]} (::gdb/game db)]
         {:db (update db ::data assoc
                      :error nil
                      :scoring? false)
          :chsk/send [:client/score [code seqno game-score]]})
       {:db (update db ::data assoc
                    :error "Ungültige Bewertung: Die Summe der Beträge muss null ergeben!")}))))

;;;; Subscriptions

(rf/reg-sub
 ::can-score?
 :<- [::gdb/game]
 (fn [game _]
   (g/all-taken? game)))

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
 ::scores
 :<- [::-data]
 (fn [db _]
   (:scores db)))

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
 :<- [::scores]
 (fn [scores [_ seat]]
   (nth scores seat)))
