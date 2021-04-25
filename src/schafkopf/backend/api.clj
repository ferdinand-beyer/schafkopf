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

(defn handle-authenticate
  [{:keys [body-params session]}]
  (if (= host-password (:password body-params))
    (do
      (timbre/info "Host authentication successful")
      (let [game (ctl/ensure-game!)
            uid (or (:uid session) (ctl/generate-uid))
            send-fn (partial send-game-event! uid)
            state (ctl/join-game game uid "Host" send-fn)
            code (:code state)]
        {:status 200
         :body {:role :host
                :code code}
         :session (assoc session
                         :role :host
                         :uid uid
                         :game code)}))
    (do
      (timbre/info "Host authentication failed (invalid password)")
      {:status 403
       :body {:error :invalid-credentials}})))

(def routes
  [["/api" {:middleware [wrap-format]}
    ["/authenticate" {:post handle-authenticate}]]
   ["/chsk" {:get chsk-ajax-get-or-ws-handshake
             :post chsk-ajax-post}]])