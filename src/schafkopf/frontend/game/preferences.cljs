(ns schafkopf.frontend.game.preferences
  (:require [re-frame.core :as rf]
            [schafkopf.game]
            [schafkopf.frontend.game.core :as g]))

(def weights
  (-> {}
      (into (map-indexed #(vector %2 %1)
                         [7 8 9 :unter :ober :king 10 :deuce]))
      (into (map-indexed #(vector %2 (* 8 %1))
                         [:bells :hearts :leaves :acorns]))))

(def default-sort-opts {:suit :hearts
                        :unter? true
                        :ober? true})

(defn base-weight [[rank suit]]
  (+ (weights rank) (weights suit)))

(defn extra-weight [[rank suit] opts]
  (cond
    (and (:ober? opts) (= :ober rank)) 96
    (and (:unter? opts) (= :unter rank)) 64
    (= suit (:suit opts)) 32
    :else 0))

(defn card-weight [card opts]
  (+ (base-weight card)
     (extra-weight card opts)))

(defn card-priority [card opts]
  (- (card-weight card opts)))

;;;; Sorting event handlers

(rf/reg-event-db
 ::sort-as-given
 (fn [db _]
   (dissoc db ::sort-opts)))

(rf/reg-event-db
 ::sort-default
 (fn [db _]
   (assoc db ::sort-opts default-sort-opts)))

(rf/reg-event-db
 ::sort-by-suit
 (fn [db [_ suit]]
   (assoc-in db [::sort-opts :suit] suit)))

(rf/reg-event-db
 ::toggle-sort-by-unter
 (fn [db _]
   (update-in db [::sort-opts :unter?] not)))

(rf/reg-event-db
 ::toggle-sort-by-ober
 (fn [db _]
   (update-in db [::sort-opts :ober?] not)))

;;;; Sorting subscriptions

(rf/reg-sub
 ::sort-opts
 (fn [db _]
   (::sort-opts db)))

(rf/reg-sub
 ::hand
 :<- [::g/hand]
 :<- [::sort-opts]
 (fn [[hand opts] _]
   (if (some? opts)
     (sort-by #(card-priority % opts) hand)
     hand)))
