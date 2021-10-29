(ns todomvc-db-view.db-view.get
  (:require [todomvc-db-view.util.edn :as edn]
            [todomvc-db-view.db-view.todo-list :as todo-list]
            [todomvc-db-view.db-view.todo-edit :as todo-edit]
            [todomvc-db-view.db-view.todo-new :as todo-new]
            [clojure.walk :as walk]
            ))

;; Concept:
;;
;; Provides the API endpoint to get the db-view. The request body
;; contains the parameters to query the database to assemble the
;; `:db-view/output` map, that contains the required data for the
;; current active UI parts. This value is returned in the response
;; body and the client stores it in the Reagent app state atom, where
;; the UI components can access it.

(defn get-view
  "Main entry point to gather the `:db-view/output` map for the
   client. Based on the given Datomic database value `db` and the
   `:db-view/input` map from the client."
  [db db-view-input]
  (merge
   (todo-list/get-view db
                       db-view-input)
   (todo-edit/get-view db
                       db-view-input)
   (todo-new/get-view db
                      db-view-input)
   ;; NOTE: add other db-view parts here.
   ))

(defn prepare-commands
  "Converts each command var into a symbol, so that it can be
   serialized."
  [x]
  (walk/postwalk (fn [x]
                   (if (var? x)
                     (symbol x)
                     x))
                 x))

(defn ring-handler
  "Ring handler to get the `:db-view/output` map for the given
   `:db-view/input` map in the `request` body."
  [db request]
  (when (and (= (:request-method request) :post)
             (= (:uri request) "/db-view/get"))
    ;; NOTE: for a production app rather use
    ;;       [Transit](https://github.com/cognitect/transit-format)
    ;;       here instead of EDN:
    (let [db-view-input (edn/read-string (slurp (:body request)))]
      ;; NOTE: for a production app do the appropriate authorization
      ;;       checks:
      (-> (get-view db
                    db-view-input)
          (prepare-commands)
          (edn/response)
          ))))
