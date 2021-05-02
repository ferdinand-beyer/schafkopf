(ns schafkopf.frontend.auth.core
  (:require [re-frame.core :as rf]
            [schafkopf.frontend.comm :refer [backend-interceptors]]
            [schafkopf.frontend.game.core :as game]))

(rf/reg-event-fx
 ::host
 backend-interceptors
 (fn [{:keys [db]} [_ name password]]
   {:db (assoc db ::busy :host)
    :http-xhrio {:method :post
                 :uri "/api/host"
                 :params {:name name
                          :password password}
                 :on-success [::joined]
                 :on-failure [::host-failed]}}))

(rf/reg-event-fx
 ::join
 backend-interceptors
 (fn [{:keys [db]} [_ name join-code]]
   {:db (assoc db ::busy :join)
    :http-xhrio {:method :post
                 :uri "/api/join"
                 :params {:name name
                          :join-code join-code}
                 :on-success [::joined]
                 :on-failure [::join-failed]}}))

(rf/reg-event-fx
 ::joined
 (fn [{:keys [db]} [_ game]]
   {:db (dissoc db ::busy ::host-error ::join-error)
    :dispatch [::game/init game]}))

(rf/reg-event-db
 ::host-failed
 (fn [db [_ result]]
   (let [error (get-in result [:response :error])]
     (-> db
         (dissoc ::busy)
         (assoc ::host-error
                (if (= error :invalid-credentials)
                  "Kennwort ist nicht korrekt"
                  "Unbekannter Fehler"))))))

(rf/reg-event-db
 ::join-failed
 (fn [db _]
   (-> db
       (dissoc ::busy)
       (assoc ::join-error "Konnte nicht beitreten"))))

(rf/reg-sub
 ::host-loading?
 (fn [db]
   (= :host (::busy db))))

(rf/reg-sub
 ::join-loading?
 (fn [db]
   (= :join (::busy db))))

(rf/reg-sub
 ::host-error
 (fn [db]
   (::host-error db)))

(rf/reg-sub
 ::join-error
 (fn [db]
   (::join-error db)))
