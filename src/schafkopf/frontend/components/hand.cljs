(ns schafkopf.frontend.components.hand
  (:require [mui-bien.core.styles :refer [make-styles]]
            [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]))

(def width 112)
(def height 200)

(def angle 5)
(def radius 800)

(defn slot-transform [i n delta]
  (let [pos (- i (int (/ n 2)))]
    (str "translate(0px, " radius "px) "
         "rotate(" (* pos angle) "deg) "
         "translate(0px, " (- 0 radius delta) "px)")))

(let [use-styles
      (make-styles
       {:root
        (fn [{:keys [i n]}]
          {:z-index (inc i)
           :transform (slot-transform i n 0)})

        :enabled
        (fn [{:keys [i n]}]
          {"&:hover" {:transform (slot-transform i n 20)}})}
       {:name "slot"})]

  (defn slot* [{:keys [class card disabled? on-click]
                :as props}]
    (let [classes (use-styles props)]
      [:div
       {:class [class
                (:root classes)
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
           :padding-top 30
           ;; Unless/until we lay the player bar on top!
           :overflow :hidden
           :display :flex
           :justify-content :center}
    :slot {:width 0
           :transform-origin (str (/ width 2.0) "px "
                                  (/ height 2.0) "px")
           :transition "transform ease-out 250ms"}}
   {:name "hand"}))

(defn hand*
  [{:keys [cards disabled? on-play]
    :or {cards []}}]
  (let [classes (use-styles)]
    [:div
     {:class (classes :root)}
     (doall
      (for [[i card] (map vector (range) cards)]
        ^{:key (card-key card)}
        [:f> slot*
         {:i i
          :n (count cards)
          :class (classes :slot)
          :card card
          :disabled? disabled?
          :on-click #(when on-play (on-play card))}]))]))

(defn hand [props]
  [:f> hand* props])
