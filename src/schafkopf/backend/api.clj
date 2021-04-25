(ns schafkopf.backend.api
  (:require [muuntaja.middleware :refer [wrap-format]]
            [wrench.core :as config]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.timbre :as timbre]
            [schafkopf.backend.control :as ctl]))

(config/def host-password {:secret true})

(defn channel-socket-authorized? [req]
  (some? (get-in req [:session :uid])))

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
  (def connected-uids connected-uids))

(defn send-game-event! [uid event]
  (timbre/debug "Sending game event to user" uid)
  (chsk-send! uid event))

(defn do-join [session role game name]
  (let [uid (or (:uid session) (ctl/generate-uid))
        send-fn (partial send-game-event! uid)]
    (if-let [state (ctl/join-game! game uid name send-fn)]
      (let [code (:code state)]
        {:status 200
         :body {:role :host
                :code code}
         :session (assoc session
                         :role role
                         :uid uid
                         :code code)})
      {:status 400
       :body {:error :game-full}})))

(defn handle-authenticate
  [{:keys [body-params session]}]
  (if (= host-password (:password body-params))
    (do
      (timbre/info "Host authentication successful")
      (do-join session :host (ctl/ensure-game!) "Host"))
    (do
      (timbre/info "Host authentication failed (invalid password)")
      {:status 403
       :body {:error :invalid-credentials}})))

(defn handle-guest-join
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
    ["/authenticate" {:post handle-authenticate}]
    ["/join" {:post handle-guest-join}]]
   ["/chsk" {:get chsk-ajax-get-or-ws-handshake
             :post chsk-ajax-post}]])
