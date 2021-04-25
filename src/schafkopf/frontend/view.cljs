(ns schafkopf.frontend.view
  (:require [re-frame.core :as rf]
            [mui-bien.core :as mui]
            [schafkopf.frontend.auth.view :as auth]))

(defn root []
  (let [role @(rf/subscribe [:schafkopf.frontend.auth.core/role])]
    [:<>
     [mui/css-baseline]
     (case role
       :host [:h1 "Host"]
       :guest [:h1 "Guest"]
       [auth/auth-screen])]))
