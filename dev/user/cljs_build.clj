(ns user.cljs-build
  (:require [mount.core :as mount]
            [shadow.cljs.devtools.api :as cljs]
            [shadow.cljs.devtools.server :as shadow]))

(mount/defstate ^{:on-reload :noop} shadow-cljs-server
  :start (shadow/start!)
  :stop (shadow/stop!))

(mount/defstate ^{:on-reload :noop} cljs-app-watcher
  :start (cljs/watch :app)
  :stop (cljs/stop-worker :app))

(mount/defstate ^{:on-reload :noop} cljs-test-watcher
  :start (cljs/watch :test)
  :stop (cljs/stop-worker :test))

(def states [#'shadow-cljs-server
             #'cljs-app-watcher
             #'cljs-test-watcher])

(defn start []
  (mount/start states))

(defn stop []
  (mount/stop states))