(ns todomvc-db-view.command.send
  (:require [todomvc-db-view.state.core :as state]
            [cljs-http.client :as http]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; Concept:
;;
;; Helper namespace to send an encrypted command map to the server.

(defn send!
  "Sends the `command` map to the server, returns the response body or
   `false` if the request failed."
  [command]
  (let [params (assoc (:db-view/input @state/state)
                      :db-view/command
                      command)]
    (go-loop []
      (let [response (a/<! (http/request
                            {:request-method :post
                             :url "/db-view/command"
                             :edn-params params
                             ;;:transit-opts {:encoding :json-verbose}
                             }))]
        (case (:status response)
          200
          (let [db-view-value (:body response)]
            (swap! state/state
                   assoc
                   :db-view/output
                   db-view-value))

          429
          (do (a/<! (a/timeout (+ 500
                                  (rand-int 500))))
              (recur))
          ;; TODO: consider how to inform the user about errors.
          false)))))
