(ns schafkopf.frontend.auth.core
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [schafkopf.frontend.comm :refer [backend-interceptors]]
            [schafkopf.frontend.game.core :as game]))

(rf/reg-event-fx
 ::host-login
 backend-interceptors
 (fn [{:keys [db]} [_ password]]
   {:db (assoc db ::authenticating :host)
    :http-xhrio {:method :post
                 :uri "/api/authenticate"
                 :params {:password password}
                 :format (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success [::host-login-succeeded]
                 :on-failure [::host-login-failed]}}))

(rf/reg-event-fx
 ::host-login-succeeded
 (fn [{:keys [db]} [_ result]]
   {:db (-> db
            (dissoc ::authenticating ::host-error)
            (assoc ::role :host))
    :dispatch [::game/join]}))

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

(rf/reg-sub
 ::host-loading?
 (fn [db]
   (= :host (::authenticating db))))

(rf/reg-sub
 ::host-login-error
 (fn [db]
   (::host-error db)))

(rf/reg-sub
 ::role
 (fn [db]
   (::role db)))
