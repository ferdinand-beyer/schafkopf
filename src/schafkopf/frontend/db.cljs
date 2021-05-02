(ns schafkopf.frontend.db
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [schafkopf.frontend.game.core :as game]
            [schafkopf.frontend.comm :refer [backend-interceptors]]))

(def initial-db {::initializing? true})

;; Initializes the DB and checks the backend for any running game we're in.
(rf/reg-event-fx
 ::init
 backend-interceptors
 (fn [_ _]
   {:db initial-db
    :http-xhrio {:method :get
                 :uri "/api/game"
                 :on-success [::game-received]
                 :on-failure [::init-failed]}}))

;; Game data received (might be nil).
(rf/reg-event-fx
 ::game-received
 (fn [_ [_ game]]
   {:fx [(when (some? game)
           [:dispatch [::game/init game]])
         [:dispatch [::init-completed]]]}))

(rf/reg-event-db
 ::init-failed
 (fn [db [_ result]]
   ;; TODO: Global error screen?
   (log/error "Failed to receive game state:" result)
   db))

(rf/reg-event-db
 ::init-completed
 (fn [db _]
   (dissoc db ::initializing?)))

;;;; Subscriptions

(rf/reg-sub
 ::initializing?
 (fn [db _]
   (db ::initializing?)))
