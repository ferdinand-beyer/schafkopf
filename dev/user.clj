(ns user
  {:clj-kondo/config
   '{:linters {:unused-namespace {:level :off}
               :unused-referred-var {:level :off}
               :refer-all {:level :off}}}}
  
  (:require [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :as ns-tools :refer [refresh]]
            
            [clojure.core.async :as async]
            [clojure.string :as str]

            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            
            [kaocha.repl :as k]

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

;;(pjstadig.humane-test-output/activate!)

;; Log to std-out (*out* might be re-bound in threads!)
(timbre/merge-config!
 {:appenders {:println (appenders/println-appender {:stream :std-out})}})

(stest/instrument)

(mount/defstate kaocha-runner
  :start (k/run-all))

(defn app-states []
  (filter #(or (str/starts-with? % "#'schafkopf")
               (str/starts-with? % "#'user/"))
          (mount/find-all-states)))

(defn start []
  (wrench/reset! :env (wrench/from-file "dev/config.edn"))
  (wrench/validate-and-print)
  (mount/start))

(defn stop []
  (mount/stop (app-states)))

(defn reset []
  (stop)
  (refresh :after `start))

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

(defn expand-fake-uid [uid]
  (if (int? uid)
    (str "fake-" uid)
    uid))

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
   (ctl/play! ctl/game-atom (expand-fake-uid uid) (seqno) card)))

(defn play-fakes! []
  (loop []
    (let [uid (active-uid)]
      (when (str/starts-with? uid "fake-")
        (play!)
        (recur)))))

(defn take!
  ([] (take! (active-uid)))
  ([uid]
   (ctl/take! ctl/game-atom (expand-fake-uid uid) (seqno))))

