(ns schafkopf.frontend.game.views.peer-info
  (:require [reagent.core :as r :refer [with-let]]
            [re-frame.core :as rf]

            [mui-bien.core.avatar :refer [avatar]]
            [mui-bien.core.card :refer [card]]
            [mui-bien.core.card-header :refer [card-header]]
            [mui-bien.core.card-actions :refer [card-actions]]
            [mui-bien.core.styles :refer [make-styles]]

            [schafkopf.frontend.components.player-badge :refer [player-badge]]
            [schafkopf.frontend.components.stat :refer [stat score-stat]]

            [schafkopf.frontend.game.views.tricks :refer [show-tricks-button]]

            [schafkopf.frontend.game.core :as g]))

(def use-styles
  (make-styles
   (fn [{:keys [palette spacing]}]
     {:root {:position :relative
             :z-index 100}
      :active {:border "1px solid"
               :border-color (get-in palette [:secondary :main])}
      :container {:display :flex
                  :padding (spacing 2)
                  :align-items :center}
      :badge {:margin-right (spacing 2)
              :align-self :flex-start}
      :stats {:display :flex
              :flex-direction :column}
      :stat {}})))

(defn- absent-card [{:keys [classes]}]
  [card
   {:class (classes :root)}
   [card-header
    {:avatar [avatar "?"]
     :title "Niemand"
     :subheader "Warte auf Spieler..."}]])

(defn- present-card [{:keys [seat classes]}]
  (with-let [name (rf/subscribe [::g/peer-name seat])
             active? (rf/subscribe [::g/peer-active? seat])
             balance (rf/subscribe [::g/peer-balance seat])
             hand-count (rf/subscribe [::g/peer-hand-count seat])
             trick-count (rf/subscribe [::g/peer-trick-count seat])
             tricks-visible? (rf/subscribe [::g/peer-tricks-visible? seat])
             points (rf/subscribe [::g/peer-points seat])
             score (rf/subscribe [::g/peer-score seat])]
    [card
     {:class [(classes :root)
              (when @active? (classes :active))]}
     [:div {:class (classes :container)}
      [player-badge
       {:class (classes :badge)
        :name @name
        :balance @balance}]

      [:div
       {:class (classes :stats)}
       [stat {:class (classes :stat)
              :label "Karten:"
              :value @hand-count}]
       [stat {:class (classes :stat)
              :label "Stiche:"
              :value @trick-count}]
       (when @points
         [stat {:class (classes :stat)
                :label "Punkte:"
                :value @points}])
       (when @score
         [score-stat {:class (classes :stat)
                      :value @score}])]]

     (when @tricks-visible?
       [card-actions
        [show-tricks-button
         {:seat seat
          :size :small
          :color :primary}
         "Stiche anzeigen"]])]))

(defn- peer-info* [{:keys [seat] :as props}]
  (with-let [present? (rf/subscribe [::g/peer-present? seat])]
    (let [classes (use-styles)
          props (assoc props :classes classes)]
      (if @present?
        [present-card props]
        [absent-card props]))))

(defn peer-info [props]
  [:f> peer-info* props])
