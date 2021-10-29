(ns todomvc-db-view.db-view.todo-list
  (:require [datomic.api :as d]
            [todomvc-db-view.datomic.core :as datomic]))

;; Concept:
;;
;; The db-view part for the todo list UI.

(defn q-all
  "Queries all todo-item entities and pulls the attributes which are
   required for todo list UI."
  [db]
  (d/q
   '[:find
     [(pull ?e
            [:db/id :todo/title :todo/done]) ...]
     :where
     [?e :todo/title]]
   db))

(defn commands
  "Prepares the commands for the `todo-item`."
  [todo-item]
  (merge
   {:todo/delete! [#'datomic/retract-entity! (:db/id todo-item)]}
   (if-not (:todo/done todo-item)
     {:todo/done! [#'datomic/transact! [{:db/id (:db/id todo-item)
                                         :todo/done true}]]}
     {:todo/active! [#'datomic/transact! [{:db/id (:db/id todo-item)
                                           :todo/done false}]]})
   ))

(defn q-completed-todo-item-eids
  "Returns the entity ids of completed todo items."
  [db]
  (d/q
   '[:find
     [?e ...]
     :where
     [?e :todo/done true]]
   db))

(defn clear-completed-tx
  "Transaction to remove all todo items which are marked as completed."
  [db]
  (map
   (fn [eid]
     [:db/retractEntity eid])
   (q-completed-todo-item-eids db)))

(defn clear-completed!
  []
  (datomic/transact! (clear-completed-tx (datomic/db))))

(defn q-todo-item-eids
  "Returns the entity ids of all todo items."
  [db]
  (d/q
   '[:find
     [?e ...]
     :where
     [?e :todo/title]]
   db))

(comment
  (q-todo-item-eids (datomic/db))
  )

(defn set-done-tx
  "Transaction to set the `:todo/done` attribute of all entities to
   `done-value`."
  [db done-value]
  (map
   (fn [eid]
     [:db/add eid :todo/done done-value])
   (q-todo-item-eids db)))

(defn set-done!
  "Set done value of all todo items."
  [done-value]
  (datomic/transact! (set-done-tx (datomic/db)
                                  done-value)))

(defn get-view
  "Returns the db-view for the todo list UI."
  [db db-view-input]
  (when-let [params (:todo/list db-view-input)]
    (let [all (q-all db)
          {:keys [active completed]} (group-by (fn [todo-item]
                                                 (if (:todo/done todo-item)
                                                   :completed
                                                   :active))
                                               all)
          todo-items (case (:todo/filter params)
                       :active
                       active
                       :completed
                       completed
                       all)]
      {:todo/list {:todo/list-items (map
                                     (fn [todo-item]
                                       (merge todo-item
                                              (commands todo-item)))
                                     todo-items)
                   :todo/active-count (count active)
                   :todo/completed-count (count completed)
                   :todo/complete-all! [#'set-done! true]
                   :todo/activate-all! [#'set-done! false]
                   :todo/clear-completed! [#'clear-completed!]}})))

(comment
  (require '[clj-http.client :as http])

  (http/request
   {:request-method :post
    :url "http://localhost:8080/db-view/get"
    :body (pr-str {:todo/list
                   {:todo/filter :active}})
    :as :clojure})

  )
