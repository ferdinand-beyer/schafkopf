(ns schafkopf.frontend.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [schafkopf.frontend.view :as view]))

(enable-console-print!)

(def anti-forgery-token
  (some-> (.querySelector js/document "meta[name=token]") (.-content)))

(defn render []
  (reagent.dom/render [view/root]
                      (.getElementById js/document "app")))

(defn ^:dev/after-load refresh! []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init! []
  (render))