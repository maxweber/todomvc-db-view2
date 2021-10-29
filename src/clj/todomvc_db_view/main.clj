(ns todomvc-db-view.main
  (:require [todomvc-db-view.ring-main :as ring-main]
            [org.httpkit.server :as server])
  (:gen-class))

;; Concept:
;;
;; Starts and stops the system (production mode).

(defonce server
  (delay
    (server/run-server #'ring-main/app
                       {:port 8080})))

(def shutdown-hook
  ;; stops the system on a JVM shutdown:
  (Thread.
   (fn []
     (@server))))

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime)
                    shutdown-hook)
  @server
  )
