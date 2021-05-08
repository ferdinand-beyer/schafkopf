(ns schafkopf.frontend.components.stat
  (:require [mui-bien.core.colors :refer [color]]
            [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.typography :refer [typography]]))

(def use-styles
  (make-styles
   {:root {:font-size "0.75rem"
           :text-transform :uppercase}
    :profit {:color (color :green :500)}
    :loss {:color (color :red :500)}
    :label {:margin-right "0.25rem"}
    :value {}}))

(defn stat* [{:keys [label value class variant] :as props}]
  (let [classes (use-styles)]
    [typography
     (-> (dissoc props :label :value)
         (assoc :variant :body2
                :class [(classes variant) class]
                :classes (select-keys classes [:root])))
     (when label
       [:span {:class (classes :label)} label])
     [:span {:class (classes :value)} value]]))

(defn stat [props]
  [:f> stat* props])

(defn score-stat [{:keys [value] :as props}]
  (stat (merge {:variant (cond
                           (pos? value) :profit
                           (neg? value) :loss)
                :label "Ergebnis:"}
               props)))
