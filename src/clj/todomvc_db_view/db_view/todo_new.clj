(ns todomvc-db-view.db-view.todo-new
  (:require [todomvc-db-view.datomic.core :as datomic]))

(defn get-view
  "Provides the db-view for the `:todo/new!` command that creates new
   todo items."
  [db db-view-input]
  (when-let [title (get-in db-view-input
                           [:todo/new
                            :todo/title])]
    {:todo/new {:todo/new! [#'datomic/transact!
                            [{:db/id "new TODO"
                              :todo/title title
                              :todo/done false}]]}}))
