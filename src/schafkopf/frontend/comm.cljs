(ns schafkopf.frontend.comm
  (:require [cljs.core.async :as async]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as timbre])
  (:require-macros
   [cljs.core.async.macros :refer (go go-loop)]))

(def anti-forgery-token
  (some-> (.querySelector js/document "meta[name=token]") (.-content)))

(def translate-backend-call
  "Interceptor translating backend call effects to HTTP requests."
  (rf/->interceptor
   :id :secure-comm
   :after (fn [context]
            (if (some? (get-in context [:effects :http-xhrio]))
              (assoc-in context [:effects :http-xhrio :headers "X-CSRF-Token"] anti-forgery-token)
              context))))

(def backend-interceptors [translate-backend-call])

(def channel-socket (atom nil))

(defmulti -handle-event! :id)

(defmethod -handle-event! :default
  [{:keys [event]}]
  (timbre/warn "Unhandled server event" event))

(defmethod -handle-event! :chsk/state
  [{:keys [?data]}]
  (let [[_ new-state-map] ?data]
    (if (:first-open? new-state-map)
      (timbre/info "Channel socket successfully established:" new-state-map)
      (timbre/debug "Channel socket state change:" new-state-map))))

(defmethod -handle-event! :chsk/recv
  [{:keys [?data]}]
  (timbre/info "Push event from server:" ?data))

(defmethod -handle-event! :chsk/handshake
  [{:keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (timbre/debug "Handshake: " ?data)))

(defn handle-event! [ev-msg]
  (-handle-event! ev-msg))

(defn replace-channel-socket! [new-map]
  (swap! channel-socket
         (fn [old-map]
           (when-let [{:keys [chsk stop-router-fn]} old-map]
             (timbre/debug "Disconnecting channel socket")
             (stop-router-fn)
             (sente/chsk-disconnect! chsk))
           new-map)))

(defn connect-channel-socket! []
  (let [{:keys [ch-recv] :as chsk-map}
        (sente/make-channel-socket!
         "/chsk"
         anti-forgery-token
         {:packer (sente-transit/get-transit-packer)
          :wrap-recv-evs? false})
        
        stop-router-fn (sente/start-client-chsk-router! ch-recv handle-event!)]
    (replace-channel-socket! (assoc chsk-map :stop-router-fn stop-router-fn))))

(rf/reg-fx
 :chsk/connect
 (fn [_]
   (timbre/info "Connecting channel socket")
   (connect-channel-socket!)))
