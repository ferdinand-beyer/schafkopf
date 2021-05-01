(ns schafkopf.frontend.game.views.scoring
  (:require [re-frame.core :as rf]

            [mui-bien.core.button :refer [button]]
            [mui-bien.core.dialog :refer [dialog]]
            [mui-bien.core.dialog-actions :refer [dialog-actions]]
            [mui-bien.core.dialog-title :refer [dialog-title]]
            [mui-bien.core.dialog-content :refer [dialog-content]]
            [mui-bien.core.form-helper-text :refer [form-helper-text]]
            [mui-bien.core.grid :refer [grid]]
            [mui-bien.core.text-field :refer [text-field]]

            [schafkopf.frontend.game.score :as score]))

(defn score-field [{:keys [seat] :as props}]
  (let [name @(rf/subscribe [::score/name seat])
        score @(rf/subscribe [::score/score seat])]
    [text-field
     (assoc props
            :type :number
            :variant :outlined
            :label name
            :value score
            :input-props {:step 10}
            :on-change #(rf/dispatch [::score/change seat (.. % -target -value)]))]))

(defn score-dialog-form [_]
  (let [sum @(rf/subscribe [::score/sum])
        error @(rf/subscribe [::score/error])
        has-error? (some? error)]
    [:form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (rf/dispatch [::score/submit]))}

     [dialog-title "Spiel bewerten"]
     [dialog-content
      [grid
       {:container true
        :spacing 1
        :direction :row
        :wrap :nowrap}
       (doall
        (for [seat (range 5)]
          ^{:key seat}
          [grid
           {:item true
            :xs true}
           [score-field {:seat seat
                         :error has-error?}]]))]
      
      [:div
       "Summe: " sum]
      
      [form-helper-text
       {:error has-error?}
       (or error " ")]
      
      ]

     [dialog-actions
      #_[player-tricks]
      [button
       {:type :submit
        :color :primary}
       "Speichern"]]]))

(defn score-dialog [{:keys [open? on-close]}]
  [dialog
   {:open open?
    :on-close on-close}
   [score-dialog-form]])

(defn score-button []
  (let [open? (rf/subscribe [::score/scoring?])
        toggle-open #(rf/dispatch [::score/toggle])]
    (fn [_]
      [:<>
       [button
        {:disabled @open?
         :on-click toggle-open}
        "Spiel bewerten"]
       [score-dialog {:open? @open?
                      :on-close toggle-open}]])))
