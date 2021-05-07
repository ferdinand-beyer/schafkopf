(ns schafkopf.frontend.components.playing-card
  (:require [mui-bien.core.button-base :refer [button-base]]
            [mui-bien.core.paper :refer [paper]]
            [mui-bien.core.styles :refer [make-styles]]))

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

(def w 224)
(def h 400)
(def border-radius 24)
(def scale 0.5)

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
           :width (* w scale)
           :height (* h scale)
           :border-radius (str (* (/ border-radius w) 100) "%/"
                               (* (/ border-radius h) 100) "%")
           :overflow :hidden}
    :sprite {:max-width w
             :max-height h
             :width (* w scale)
             :height (* h scale)}
    :texture {:width "100%"
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

(def use-styles (make-styles styles {:name "card"}))

(defn card-sprite*
  [{:keys [card classes]}]
  (let [[rank suit] card]
    [:div
     {:class (:sprite classes)}
     [:div
      {:class [(:texture classes)
               (classes (rank-class rank))
               (classes suit)]}]]))

(defn playing-card*
  [{:keys [card class elevation
           button? disabled? on-click]
    :or {elevation 1
         button? false}}]
  (let [classes (use-styles)
        sprite [:f> card-sprite* {:card card, :classes classes}]]
    [paper
     {:elevation elevation
      :class class
      :classes {:root (:root classes)}}
     (if button?
       [button-base
        {:focus-ripple true
         :disabled disabled?
         :on-click on-click}
        sprite]
       sprite)]))

(defn playing-card
  [props]
  [:f> playing-card* props])
