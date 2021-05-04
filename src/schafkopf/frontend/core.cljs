(ns schafkopf.frontend.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [schafkopf.frontend.db :as db]
            [schafkopf.frontend.view :as view]))

(enable-console-print!)

(def banner "🅢🅒🅗🅐🅕🅚🅞🅟🅕")

(defn print-banner []
  (.info js/console (str "%c" banner) "font-size:18pt; color:#115293;"))

(defn render []
  (reagent.dom/render [view/root]
                      (.getElementById js/document "app")))

(defn ^:dev/after-load refresh! []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init! []
  (print-banner)
  (rf/dispatch-sync [::db/init])
  (render))
