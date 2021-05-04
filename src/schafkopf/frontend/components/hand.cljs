(ns schafkopf.frontend.components.hand
  (:require [mui-bien.core.styles :refer [make-styles]]
            [schafkopf.frontend.components.playing-card :refer [card-key playing-card]]))

(let [use-styles (make-styles {:root {:display :flex
                                      :flex-direction :row
                                      :justify-content :center}})]
  (defn hand*
    [{:keys [cards disabled? on-play]
      :or {cards []}}]
    (let [classes (use-styles)]
      [:div
       {:class (classes :root)}
       (doall
        (for [card cards]
          ^{:key (card-key card)}
          [playing-card
           {:card card
            :button true
            :disabled disabled?
            :on-click #(when on-play (on-play card))}]))])))

(defn hand [props]
  [:f> hand* props])
