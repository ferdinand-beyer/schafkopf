(ns schafkopf.backend.api
  (:require [mount.core :as mount]
            [muuntaja.middleware :refer [wrap-format]]
            [wrench.core :as config]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as log]
            [schafkopf.backend.game :as sg]))

(config/def host-name {:default "Host"})
(config/def host-password {:require true :secret true})

;;;; Channel sockets

(defn channel-socket-authorized?
  "Returns true if the user is authorized to establish a channel socket,
   false otherwise."
  [req]
  ;; TODO: Check for valid game ID + if the user is a player
  (some? (get-in req [:session :uid])))

(let [{:keys [ch-recv send-fn
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server!
       (get-sch-adapter)
       {:authorized?-fn channel-socket-authorized?
        :packer (sente-transit/get-transit-packer)})]
  
  (def chsk-ajax-post ajax-post-fn)
  (def chsk-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-recv ch-recv)
  (def chsk-send! send-fn))

(defn send-game-event! [uid event]
  (log/debug "Sending game event to user" uid)
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

(defmethod -handle-event-message :game/start [{:keys [uid ?data]}]
  (when-let [[code seqno] ?data]
    (when-let [game (sg/find-game code)]
      (sg/start! game uid seqno))))

(defmethod -handle-event-message :client/play [{:keys [uid ?data]}]
  (when-let [[code seqno card] ?data]
    (when-let [game (sg/find-game code)]
      (sg/play! game uid seqno card))))

(defmethod -handle-event-message :client/take [{:keys [uid ?data]}]
  (when-let [[code seqno] ?data]
    (when-let [game (sg/find-game code)]
      (sg/take! game uid seqno))))

(defmethod -handle-event-message :client/score [{:keys [uid ?data]}]
  (when-let [[code seqno score] ?data]
    (when-let [game (sg/find-game code)]
      (sg/score! game uid seqno score))))

(defmethod -handle-event-message :client/start-next [{:keys [uid ?data]}]
  (when-let [[code seqno] ?data]
    (when-let [game (sg/find-game code)]
      (sg/start-next! game uid seqno))))

;; TODO :game/reset
;; TODO :game/end

;; TODO :client/undo

(defn handle-event-message [{:as ev-msg :keys [uid event]}]
  (log/trace "Received event:" event " - uid:" uid)
  ;; XXX Dispatch in dedicated thread?
  (-handle-event-message ev-msg))

(mount/defstate event-router
  :start (sente/start-server-chsk-router! ch-recv handle-event-message)
  :stop (event-router))

;;; Ring handlers

;; TODO: Move more of this logic to control.
;; Guests join, hosts _create_ games.
(defn do-join
  "Lets the connected user join a game, and returns a ring response."
  [session role game name]
  (let [uid (or (:uid session) (sg/generate-uid))
        send-fn (partial send-game-event! uid)]
    (if-let [client-game (sg/join! game uid name send-fn)]
      {:status 200
       :body {:role :host
              :game client-game}
       :session (assoc session
                       :role role
                       :uid uid
                       :code (:server/code client-game))}
      {:status 400
       :body {:error :game-full}})))

(defn handle-host
  [{:keys [body-params session]}]
  (if (= host-password (:password body-params))
    (do
      (log/info "Host authentication successful")
      (do-join session :host (sg/ensure-game!) host-name))
    (do
      (log/info "Host authentication failed (invalid password)")
      {:status 403
       :body {:error :invalid-credentials}})))

(defn handle-join
  [{:keys [body-params session]}]
  (let [{:keys [code name]} body-params
        game (sg/find-game code)]
    (cond
      (not (sg/valid-name? name))
      {:status 400
       :body {:error :invalid-name}}

      (nil? game)
      {:status 403
       :body {:error :invalid-code}}

      :else
      (do
        (log/info "Guest authentication successful")
        (do-join session :guest game name)))))

;; TODO /api/game -- :get the client-game (on page refresh)
;; TODO /api/leave -- leave the game (need to update session state)
(def routes
  [["/api" {:middleware [wrap-format]}
    ["/host" {:post handle-host}]
    ["/join" {:post handle-join}]]
   ["/chsk" {:get chsk-ajax-get-or-ws-handshake
             :post chsk-ajax-post}]])
