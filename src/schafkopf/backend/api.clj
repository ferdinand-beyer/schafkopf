(ns schafkopf.backend.api
  (:require [clojure.spec.alpha :as s]

            [mount.core :as mount]
            [wrench.core :as config]
            [taoensso.timbre :as log]

            [ring.util.response :as resp]

            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.sente.packers.transit :as sente-transit]

            [schafkopf.protocol :as protocol]
            [schafkopf.backend.game :as sg])
  (:import (org.apache.commons.codec.digest DigestUtils)))

;;;; Config etc.

(config/def ticket-key {:secret true})
(config/def host-password {:require true :secret true})

(defn active-player? [request]
  (let [{:keys [game-id uid]} (request :session)]
    (some-> game-id
            (sg/find-game-by-id)
            (sg/client? uid))))

;;;; Channel sockets

(let [{:keys [ch-recv send-fn
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server!
       (get-sch-adapter)
       {:authorized?-fn active-player?
        :packer (sente-transit/get-transit-packer)})]

  (def chsk-ajax-post ajax-post-fn)
  (def chsk-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-recv ch-recv)
  (def chsk-send! send-fn))

;; TODO: We can disconnect a client by sending :chsk/close.  We can either
;; look at the event for a :server/stop or switch to using async channels,
;; where a closed channel can be interpreted as disconnect request.
(defn send-game-event! [uid event]
  (log/debug "Sending" (first event) "to user" uid)
  (chsk-send! uid event))

;;;; Message handlers

(defmulti -handle-event-message :id)

(defmethod -handle-event-message :default [ev-msg]
  (log/debug "Unhandled message:" (:id ev-msg)))

(defmethod -handle-event-message :chsk/uidport-open [{:keys [uid]}]
  (log/info "Client connected:" uid))

(defmethod -handle-event-message :chsk/uidport-close [{:keys [uid]}]
  (log/info "Client disconnected:" uid))

(defmethod -handle-event-message :chsk/ws-ping [_])

;; TODO: Conform ?data of each custom event!

(defmethod -handle-event-message :client/start [{:keys [uid ?data]}]
  (when-let [[game-id seqno] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/start! game uid seqno))))

(defmethod -handle-event-message :client/skip [{:keys [uid ?data]}]
  (when-let [[game-id seqno] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/skip! game uid seqno))))

;; TODO :client/reset
;; TODO :client/stop

(defmethod -handle-event-message :client/play [{:keys [uid ?data]}]
  (when-let [[game-id seqno card] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/play! game uid seqno card))))

(defmethod -handle-event-message :client/take [{:keys [uid ?data]}]
  (when-let [[game-id seqno] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/take! game uid seqno))))

(defmethod -handle-event-message :client/score [{:keys [uid ?data]}]
  (when-let [[game-id seqno score] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/score! game uid seqno score))))

(defmethod -handle-event-message :client/next [{:keys [uid ?data]}]
  (when-let [[game-id seqno] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/start-next! game uid seqno))))

(defmethod -handle-event-message :client/undo [{:keys [uid ?data]}]
  (when-let [[game-id seqno] ?data]
    (when-let [game (sg/find-game-by-id game-id)]
      (sg/undo! game uid seqno))))

(defn handle-event-message [{:as ev-msg :keys [uid event]}]
  (log/trace "Received event:" event " - uid:" uid)
  ;; XXX Dispatch in dedicated thread?
  (-handle-event-message ev-msg))

(mount/defstate event-router
  :start (sente/start-server-chsk-router! ch-recv handle-event-message)
  :stop (event-router))

;;;; Ring handlers

(defn- make-uid [session]
  (or (session :uid) (sg/generate-id)))

(defn- error-response [status error]
  {:status status
   :headers {}
   :body {:error error}})

(defn- invalid-params []
  (error-response 400 :invalid-params))

(defn- invalid-credentials []
  (error-response 403 :invalid-credentials))

(defn- join-response
  [{:keys [session]}
   {:client/keys [client-id]
    :server/keys [game-id]
    :as client-game}]
  (assoc (resp/response client-game)
         :session (assoc session :game-id game-id :uid client-id)))

;; XXX: Have a middleware factory to conform body-params with a spec?
;; Supported by reitit using coercion:
;; https://cljdoc.org/d/metosin/reitit/0.5.13/doc/coercion/clojure-spec

(defn handle-host
  [{:keys [body-params session]
    :as request}]
  (let [{:keys [name password] :as params}
        (s/conform ::protocol/host-params body-params)]
    (cond
      (s/invalid? params)
      (invalid-params)

      (not= password host-password)
      (invalid-credentials)

      :else
      (let [uid (make-uid session)
            send-fn (partial send-game-event! uid)
            client-game (sg/host! uid name send-fn)]
        (join-response request client-game)))))

(defn handle-join
  [{:keys [body-params session]
    :as request}]
  (let [{:keys [name join-code] :as params}
        (s/conform ::protocol/join-params body-params)]
    (if (s/invalid? params)
      ;; We could look in s/explain-data [::s/problems ::s/path]
      ;; to report back which param was wrong, to display to the user.
      (invalid-params)
      (if-let [game (sg/find-game-by-join-code join-code)]
        (let [uid (make-uid session)
              send-fn (partial send-game-event! uid)
              client-game (sg/join! game uid name send-fn)]
          (if (some? client-game)
            (join-response request client-game)
            (error-response 409 :join-failed)))
        (invalid-credentials)))))

(defn handle-get-game [{:keys [session]}]
  (let [game (some-> (session :game-id) (sg/find-game-by-id))
        uid (session :uid)]
    (if (and (some? game)
             (some? uid)
             (sg/client? game uid))
      (resp/response (sg/client-game @game uid))
      (resp/status 204))))

(defn- ticket-response [response request]
  (if (and (some? ticket-key)
           (= 200 (:status response)))
    (let [ticket (DigestUtils/sha1Hex (str ticket-key " " (:remote-addr request)))]
      (if (not= ticket (get-in request [:cookies "ticket" :value]))
        (assoc-in response [:cookies "ticket"]
                  {:value ticket
                   :path "/"
                   :http-only true
                   :same-site :strict})
        response))
    response))

(defn- wrap-ticket
  [handler]
  (fn
    ([request]
     (-> (handler request)
         (ticket-response request)))
    ([request respond raise]
     (handler request
              (fn [response]
                (respond (ticket-response response request)))
              raise))))

;; TODO /api/leave -- leave the game (need to update session state)
(def routes
  [["/api" {:middleware [wrap-ticket]}
    ["/game" {:get handle-get-game}]
    ["/host" {:post handle-host}]
    ["/join" {:post handle-join}]]
   ["/chsk" {:get chsk-ajax-get-or-ws-handshake
             :post chsk-ajax-post}]])
