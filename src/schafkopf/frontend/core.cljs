(ns schafkopf.frontend.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [schafkopf.frontend.db :as db]
            [schafkopf.frontend.view :as view]))

(enable-console-print!)

(defn render []
  (reagent.dom/render [view/root]
                      (.getElementById js/document "app")))

(defn ^:dev/after-load refresh! []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init! []
  (rf/dispatch-sync [::db/init])
  (render))
