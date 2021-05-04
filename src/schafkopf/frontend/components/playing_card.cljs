(ns schafkopf.frontend.components.playing-card
  (:require [mui-bien.core.button-base :refer [button-base]]
            [mui-bien.core.card :refer [card]]
            [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.tooltip :refer [tooltip]]))

(def suit-names {:acorns "Eichel"
                 :leaves "Gras"
                 :hearts "Herz"
                 :bells "Schellen"})

(def rank-names {:unter "Unter"
                 :ober "Ober"
                 :king "König"
                 :deuce "Daus"})

(defn card-key [[rank suit]]
  (str (name suit) "-" (if (keyword? rank) (name rank) rank)))

(defn card-name [[rank suit]]
  (str (get suit-names suit) " " (get rank-names rank rank)))

(defn card-url [card]
  (str "/assets/img/decks/saxonian/" (card-key card) ".jpg"))

(def scale 0.8)

(let [use-styles
      (make-styles {:root {:border-radius 12
                           :width (* 140 scale)
                           :height (* 250 scale)
                           :background-size :cover
                           :position :relative}
                    :fill {:width "100%"
                           :height "100%"}})]
  (defn playing-card*
    [{:keys [name url elevation
             button disabled on-click]
      :or {elevation 1
           button false}}]
    (let [classes (use-styles)]
      [card
       {:elevation elevation
        :classes {:root (:root classes)}
        :style {:background-image (str "url('" url "')")}}
       [tooltip
        {:title name
         :arrow true}
       ;; Wrapper since tooltips won't work on disabled buttons.
        [:div {:class (:fill classes)}
         (when button
           [button-base
            {:class (:fill classes)
             :focus-ripple true
             :disabled disabled
             :on-click on-click}])]]])))

(defn playing-card
  [{:keys [card] :as props}]
  (let [name (card-name card)
        url (card-url card)]
    [:f> playing-card*
     (merge props {:name name
                   :url url})]))
