(ns schafkopf.frontend.components.stat
  (:require [mui-bien.core.styles :refer [make-styles]]
            [mui-bien.core.typography :refer [typography]]))

(def use-styles (make-styles
                 {:root {:font-size "0.75rem"
                         ;:letter-spacing "0.08333em"
                         :text-transform :uppercase}
                  :label {:margin-right "0.25rem"}
                  :value {}}))

(defn stat* [{:keys [label value] :as props}]
  (let [classes (use-styles)]
    [typography
     (-> (dissoc props :label :value)
         (assoc :variant :body2
                :classes (select-keys classes [:root])
                :no-wrap true))
     (when label
       [:span {:class (classes :label)} label])
     [:span {:class (classes :value)} value]]))

(defn stat [props]
  [:f> stat* props])
