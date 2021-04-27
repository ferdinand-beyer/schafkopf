(ns schafkopf.backend.api
  (:require [mount.core :as mount]
            [muuntaja.middleware :refer [wrap-format]]
            [wrench.core :as config]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as timbre]
            [schafkopf.backend.control :as ctl]))

(config/def host-name {:default "Host"})
(config/def host-password {:secret true})

;;;; Channel sockets

(defn channel-socket-authorized?
  "Returns true if the user is authorized to establish a channel socket,
   false otherwise."
  [req]
  (some? (get-in req [:session :uid])))

;;; TODO: Make this a mount state?
(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket!
       (get-sch-adapter)
       {:authorized?-fn channel-socket-authorized?
        :packer (sente-transit/get-transit-packer)})]

  (def chsk-ajax-post ajax-post-fn)
  (def chsk-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-recv ch-recv)
  (def chsk-send! send-fn)
  ;; TODO: Notify control when a uid disconnects
  (def connected-uids connected-uids))

;;;; Message handlers

(defmulti -handle-event-message :id)

(defmethod -handle-event-message :default [ev-msg]
  (timbre/debug "Unhandled message: " (:id ev-msg)))

(defn handle-event-message [ev-msg]
  (timbre/debug "Received event message:" (:id ev-msg))
  (-handle-event-message ev-msg))

(mount/defstate event-router
  :start (sente/start-server-chsk-router! ch-recv handle-event-message)
  :stop (event-router))

(defn send-game-event! [uid event]
  (timbre/debug "Sending game event to user" uid)
  (chsk-send! uid event))

;;; Ring handlers

;; TODO: Move more of this logic to control.
;; Guests 
(defn do-join
  "Lets the connected user join a game, and returns a ring response."
  [session role game name]
  (let [uid (or (:uid session) (ctl/generate-uid))
        send-fn (partial send-game-event! uid)]
    (if-let [user-game (ctl/join-game! game uid name send-fn)]
      {:status 200
       :body {:role :host
              :game user-game}
       :session (assoc session
                       :role role
                       :uid uid
                       :code (:session/code user-game))}
      {:status 400
       :body {:error :game-full}})))

(defn handle-host
  [{:keys [body-params session]}]
  (if (= host-password (:password body-params))
    (do
      (timbre/info "Host authentication successful")
      (do-join session :host (ctl/ensure-game!) host-name))
    (do
      (timbre/info "Host authentication failed (invalid password)")
      {:status 403
       :body {:error :invalid-credentials}})))

(defn handle-join
  [{:keys [body-params session]}]
  (let [{:keys [code name]} body-params
        game (ctl/find-game code)]
    (cond
      (not (ctl/valid-name? name))
      {:status 400
       :body {:error :invalid-name}}

      (nil? game)
      {:status 403
       :body {:error :invalid-code}}

      :else
      (do
        (timbre/info "Guest authentication successful")
        (do-join session :guest game name)))))

(def routes
  [["/api" {:middleware [wrap-format]}
    ["/host" {:post handle-host}]
    ["/join" {:post handle-join}]]
   ["/chsk" {:get chsk-ajax-get-or-ws-handshake
             :post chsk-ajax-post}]])
