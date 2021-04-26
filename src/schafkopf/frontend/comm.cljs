(ns schafkopf.frontend.comm
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as timbre]))

(def anti-forgery-token
  (some-> (.querySelector js/document "meta[name=csrf-token]") (.-content)))

(def default-backend-request
  {:method :post
   :headers {"X-CSRF-Token" anti-forgery-token}
   :format (ajax/transit-request-format)
   :response-format (ajax/transit-response-format)})

(defn enrich-http-xhrio [request]
  (merge default-backend-request request))

(def enrich-http-xhrio-interceptor
  "Interceptor enriching :http-xhrio effects with default values."
  (rf/->interceptor
   :id :enrich-http-xhrio
   :after (fn [context]
            (cond-> context
              (some? (get-in context [:effects :http-xhrio]))
              (update-in [:effects :http-xhrio] enrich-http-xhrio)))))

(def backend-interceptors [enrich-http-xhrio-interceptor])

(def channel-socket (atom nil))

(defmulti -handle-event! :id)

(defmethod -handle-event! :default
  [{:keys [id event]}]
  (timbre/info "Push event from server:" id)
  (some-> event (rf/dispatch)))

(defmethod -handle-event! :chsk/handshake
  [{:keys [?data]}]
  (timbre/debug "Handshake: " ?data))

(defmethod -handle-event! :chsk/state
  [{:keys [?data]}]
  (let [[_ new-state-map] ?data]
    (if (:first-open? new-state-map)
      (timbre/info "Channel socket successfully established:" new-state-map)
      (timbre/debug "Channel socket state change:" new-state-map))))

(defmethod -handle-event! :chsk/recv
  [{:keys [?data]}]
  (timbre/warn "Unhandled :chsk/recv event:" ?data))

(defmethod -handle-event! :chsk/ws-ping
  [{:keys [event]}]
  (timbre/info "Received:" event))

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
