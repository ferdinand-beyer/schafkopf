(ns schafkopf.frontend.components.playing-card
  (:require [mui-bien.core.button-base :refer [button-base]]
            [mui-bien.core.paper :refer [paper]]
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
  (str (if (keyword? rank) (name rank) rank) "-" (name suit)))

(defn card-name [[rank suit]]
  (str (get suit-names suit) " " (get rank-names rank rank)))

(def w 112)
(def h 200)
(def border-radius 12)

(def sprites-url (str "/assets/img/decks/fxs/sprites-" w "x" h ".webp"))

(defn rank-class [rank]
  (if (keyword? rank)
    rank
    (keyword (str "r-" rank))))

(def ranks [:deuce :king :ober :unter :r-10 :r-9 :r-8 :r-7])
(def suits [:acorns :leaves :hearts :bells])

(def styles
  (->
   {:root {:max-width w
           :max-height h
           :width w ; TODO: Allow flex shrinking?
           :height h
           :border-radius (str (* (/ border-radius w) 100) "%/"
                               (* (/ border-radius h) 100) "%")
           :overflow :hidden}
    :face {:width "100%"
           :height 0
           :padding-bottom (str (* (/ h w) 100) "%")
           :background-image (str "url('" sprites-url "')")
           :background-repeat "no-repeat"
           :background-size "800%"}}
   (into (map-indexed
          (fn [i r]
            [(rank-class r)
             {:background-position-x (str (* i (/ 100 7)) "%")}])
          ranks))
   (into (map-indexed
          (fn [i s]
            [s {:background-position-y (str (* i (/ 100 3)) "%")}])
          suits))))

(def use-styles (make-styles styles))

(defn card-sprite*
  [{:keys [card classes]}]
  (let [[rank suit] card]
    [:div
     {:class (:root classes)}
     [:div
      {:class [(:face classes)
               (classes (rank-class rank))
               (classes suit)]}]]))

(defn playing-card*
  [{:keys [card elevation
           button? disabled? on-click]
    :or {elevation 1
         button? false}}]
  (let [classes (use-styles)
        pic [:f> card-sprite* {:card card, :classes classes}]]
    [paper
     {:elevation elevation
      :classes {:root (:root classes)}}
     [tooltip
      {:title (card-name card)
       :arrow true}
      (if button?
        [:div ; tooltips won't work on disabled buttons
         [button-base
          {:focus-ripple true
           :disabled disabled?
           :on-click on-click}
          pic]]
        pic)]]))

(defn playing-card
  [props]
  [:f> playing-card* props])
