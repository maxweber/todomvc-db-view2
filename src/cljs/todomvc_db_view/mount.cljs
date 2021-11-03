(ns todomvc-db-view.mount
  (:require [todomvc-db-view.db-view.get :as db-view]
            [todomvc-db-view.core :as core]
            [todomvc-db-view.db-view.notify :as notify]
            [todomvc-db-view.state.core :as state]
            [reagent.core :as r]
            [cljs.core.async :as a]
            [clojure.pprint :as pprint]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; Concept:
;;
;; Initializes and mounts the ClojureScript app.

(defn inspector
  []
  [:pre {:style {:position "fixed"
                 :width "30%"
                 :height "100%"
                 :top "0px"
                 :left "0px"
                 :overflow "scroll"
                 :font-size "12px"
                 :padding-left "5px"
                 }}
   "app state:\n"
   (with-out-str
     (pprint/pprint @state/state))])

(defn start []
  (r/render-component [:<>
                       [inspector]
                       [core/todo-app]]
                      (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads, it is called in the
  ;; index.html and must be exported so it is available even in
  ;; :advanced release builds.
  (go
    (a/<! (db-view/refresh!))
    (notify/start-listening)
    (start)))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
