(ns schafkopf.frontend.components.hand
  (:require [mui-bien.core.styles :refer [make-styles]]
            [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]))

(def width 112)
(def height 200)

(def angle 5)
(def radius 800)

(def active-dist 24)

(defn slot-transform [i n active?]
  (let [pos (- i (int (/ n 2)))
        dist (if active? (+ radius active-dist) radius)]
    (str "translate(0px, " radius "px) "
         "rotate(" (* pos angle) "deg) "
         "translate(0px, " (- dist) "px)")))

(let [use-styles
      (make-styles
       {:root
        (fn [{:keys [i n]}]
          {:position :absolute
           :width width
           :height height
           :z-index (+ 100 i)
           :transform (slot-transform i n false)
           :transition "transform 225ms ease-out"})

        :enabled
        (fn [{:keys [i n]}]
          {"&:hover" {:transform (slot-transform i n true)}})}
       {:name "hand"})]

  (defn slot* [{:keys [card disabled? on-click]
                :as props}]
    (let [classes (use-styles props)]
      [:div
       {:class [(:root classes)
                (when-not disabled?
                  (:enabled classes))]}
       [playing-card
        {:card card
         :elevation 6
         :button? true
         :disabled? disabled?
         :on-click on-click}]])))

(def use-styles
  (make-styles
   {:root {:min-width width
           :min-height height
           :display :flex
           :justify-content :center}
    :slots {:width width
            :height height
            :position :relative}}
   {:name "hand"}))

(defn hand*
  [{:keys [cards disabled? on-play]
    :or {cards []}}]
  (let [classes (use-styles)]
    [:div
     {:class (classes :root)}
     [:div
      {:class (classes :slots)}
      (doall
       (for [[i card] (map vector (range) cards)]
         ^{:key (card-key card)}
         [:f> slot*
          {:i i
           :n (count cards)
           :card card
           :disabled? disabled?
           :on-click #(when on-play (on-play card))}]))]]))

(defn hand [props]
  [:f> hand* props])
