(ns schafkopf.frontend.auth.core
  (:require [re-frame.core :as rf]
            [schafkopf.frontend.comm :refer [backend-interceptors]]
            [schafkopf.frontend.game.core :as game]))

(rf/reg-event-fx
 ::host-login
 backend-interceptors
 (fn [{:keys [db]} [_ password]]
   {:db (assoc db ::authenticating :host)
    :http-xhrio {:method :post
                 :uri "/api/host"
                 :params {:password password}
                 :on-success [::host-login-succeeded]
                 :on-failure [::host-login-failed]}}))

(defn handle-join [db {:keys [role game]}]
  {:db (assoc db ::role role)
   :dispatch [::game/init game]})

(rf/reg-event-fx
 ::host-login-succeeded
 (fn [{:keys [db]} [_ result]]
   (handle-join (dissoc db ::authenticating ::host-error) result)))

(rf/reg-event-db
 ::host-login-failed
 (fn [db [_ result]]
   (let [error (get-in result [:response :error])]
     (-> db
         (dissoc ::authenticating)
         (assoc ::host-error
                (if (= error :invalid-credentials)
                  "Kennwort ist nicht korrekt"
                  "Unbekannter Fehler"))))))

(rf/reg-event-fx
 ::guest-join
 backend-interceptors
 (fn [{:keys [db]} [_ code name]]
   {:db (assoc db ::authenticating :guest)
    :http-xhrio {:method :post
                 :uri "/api/join"
                 :params {:code code
                          :name name}
                 :on-success [::guest-join-succeeded]
                 :on-failure [::guest-join-failed]}}))

(rf/reg-event-fx
 ::guest-join-succeeded
 (fn [{:keys [db]} [_ result]]
   (handle-join (dissoc db ::authenticating ::guest-error) result)))

(rf/reg-event-db
 ::guest-join-failed
 (fn [db _]
   (-> db
       (dissoc ::authenticating)
       (assoc ::guest-error "Konnte nicht beitreten"))))

(rf/reg-sub
 ::host-loading?
 (fn [db]
   (= :host (::authenticating db))))

(rf/reg-sub
 ::host-login-error
 (fn [db]
   (::host-error db)))

(rf/reg-sub
 ::guest-loading?
 (fn [db]
   (= :guest (::authenticating db))))

(rf/reg-sub
 ::guest-join-error
 (fn [db]
   (::guest-error db)))

(rf/reg-sub
 ::role
 (fn [db]
   (::role db)))
