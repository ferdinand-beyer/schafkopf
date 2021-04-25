(ns schafkopf.frontend.view
  (:require [mui-bien.core :as mui]
            [schafkopf.frontend.start.view :as start]))

(defn root []
  [:<>
   [mui/css-baseline]
   [start/start-screen]])