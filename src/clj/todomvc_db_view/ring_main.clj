(ns todomvc-db-view.ring-main
  (:require [todomvc-db-view.db-view.get :as db-view-get]
            [todomvc-db-view.db-view.command :as command]
            [todomvc-db-view.db-view.notify :as notify]
            [todomvc-db-view.datomic.core :as datomic]
            [ring.middleware.file :as middleware-file]))

(defn dispatch
  "Dispatches the Ring request to the Ring handler of the system."
  [request]
  (let [db (datomic/db)]
    (or
     (db-view-get/ring-handler db
                               request)
     (command/ring-handler db
                           request)
     (notify/ring-handler request)
     ;; NOTE: add new Ring handlers here.
     )))

(def app
  ;; The main Ring-handler:
  (-> dispatch
      (middleware-file/wrap-file "public")))
