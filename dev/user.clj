(ns user
  {:clj-kondo/config
   '{:linters {:unused-namespace {:level :off}
               :unused-referred-var {:level :off}
               :refer-all {:level :off}}}}
  
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :as ns-tools :refer [refresh]]
            
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]

            [mount.core :as mount]
            [wrench.core :as wrench]
            [pjstadig.humane-test-output]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            
            [schafkopf.backend.control :as ctl]
            [schafkopf.backend.server :as backend]
            [schafkopf.game :as game]
            [schafkopf.protocol :as protocol]
            
            [user.cljs-build :as cljs-build]))

(ns-tools/set-refresh-dirs "src" "dev" "test")

(pjstadig.humane-test-output/activate!)

;; Log to std-out (*out* might be re-bound in threads!)
(timbre/merge-config!
 {:appenders {:println (appenders/println-appender {:stream :std-out})}})

(defn app-states []
  (filter #(str/starts-with? % "#'schafkopf") (mount/find-all-states)))

(defn start []
  (wrench/reset! :env (wrench/from-file "dev/config.edn"))
  (wrench/validate-and-print)
  (mount/start))

(defn stop []
  (mount/stop (app-states)))

(defn reset []
  (stop)
  (refresh :after `start))

(defn run-tests []
  (run-all-tests #"^schafkopf\..*-test"))

(defn t []
  (refresh :after `run-tests))

;;;; Game info

(defn server-game []
  @ctl/game-atom)

(defn client-game [uid]
  (ctl/client-game (server-game) uid))

(defn seqno []
  (::ctl/seqno (server-game)))

(defn on-seat [seat]
  (->> (server-game)
       ::ctl/clients
       vals
       (filter #(= seat (::ctl/seat %)))
       first))

(defn active-client []
  (on-seat (get-in (server-game) [::ctl/game :game/active-seat])))

(defn active-uid []
  (::ctl/uid (active-client)))

;;;; Game simulation

(defn join!
  "Fill the current game with fake clients."
  []
  (let [game (ctl/ensure-game!)]
    (doseq [i (range 3)]
      (ctl/join-game! game
                      (str "fake-" i)
                      (str "Fake " i)
                      (constantly nil)))))

(defn play!
  ([] (play! (active-uid)))
  ([uid]
   (let [card (first (:player/hand (client-game uid)))]
     (play! uid card)))
  ([uid card]
   (ctl/play! ctl/game-atom uid (seqno) card)))

(defn take!
  ([] (take! (active-uid)))
  ([uid]
   (ctl/take! ctl/game-atom uid (seqno))))
