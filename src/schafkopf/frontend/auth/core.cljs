(ns schafkopf.frontend.auth.core
  (:require [re-frame.core :as rf]
            [schafkopf.frontend.comm :refer [backend-interceptors]]
            [schafkopf.frontend.game.core :as game]))

(rf/reg-event-fx
 ::host
 backend-interceptors
 (fn [{:keys [db]} [_ name password]]
   {:db (assoc db ::busy true)
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
   {:db (assoc db ::busy true)
    :http-xhrio {:method :post
                 :uri "/api/join"
                 :params {:name name
                          :join-code join-code}
                 :on-success [::joined]
                 :on-failure [::join-failed]}}))

(rf/reg-event-fx
 ::joined
 (fn [{:keys [db]} [_ game]]
   {:db (dissoc db ::busy ::error)
    :dispatch [::game/init game]}))

(rf/reg-event-db
 ::host-failed
 (fn [db [_ result]]
   (let [error (get-in result [:response :error])]
     (-> db
         (dissoc ::busy)
         (assoc ::error
                (if (= error :invalid-credentials)
                  "Kennwort ist nicht korrekt"
                  "Unbekannter Fehler"))))))

(rf/reg-event-db
 ::join-failed
 (fn [db [_ result]]
   (let [error (get-in result [:response :error])]
     (-> db
         (dissoc ::busy)
         (assoc ::error
                (if (= error :invalid-credentials)
                  "Unbekannter Zugangscode"
                  "Konnte nicht beitreten"))))))

(rf/reg-sub
 ::loading?
 (fn [db]
   (boolean (::busy db))))

(rf/reg-sub
 ::error
 (fn [db]
   (::error db)))
