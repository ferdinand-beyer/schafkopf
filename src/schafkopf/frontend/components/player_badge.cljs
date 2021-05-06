(ns schafkopf.frontend.components.player-badge
  (:require [mui-bien.core.avatar :refer [avatar]]
            [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.typography :refer [typography]]))

;; TODO: Move to subs?
(defn avatar-color [name]
  (str "hsl(" (-> name hash (mod 360)) ", 60%, 40%"))

(def use-styles
  (make-styles
   (fn [{:keys [spacing]}]
     {:root {:display :flex
             :flex-wrap :no-wrap}
      :avatar {:margin-right (spacing 2)}})))

(defn- player-badge* [{:keys [class name balance]}]
  (let [classes (use-styles)
        ;; FIXME :)
        color (avatar-color name)]
    [:div
     {:class [(classes :root) class]}
     [avatar
      (cond-> {:class (classes :avatar)}
        (some? color)
        (assoc-in [:style :background-color] color))
      (if name (first name) "?")]
     [:div
      [typography
       {:variant :body2
        :component :div}
       (or name "Niemand")]
      (when balance
        [typography
         {:variant :body2
          :component :div
          :color :textSecondary}
         balance])]]))

(defn player-badge [props]
  [:f> player-badge* props])
