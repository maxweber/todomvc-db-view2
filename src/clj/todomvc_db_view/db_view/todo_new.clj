(ns todomvc-db-view.db-view.todo-new
  (:require [todomvc-db-view.datomic.core :as datomic]
            [todomvc-db-view.db-view.todo-edit :as edit]))

(defn get-view
  "Provides the db-view for the `:todo/new!` command that creates new
   todo items."
  [db db-view-input]
  (when-let [command (:todo/new! db-view-input)]
    (when (#{:editing :error} (:status command))
      (let [title (:todo/title command)]
        (if (edit/valid-title? title)
          {:db-view/command [#'datomic/transact!
                             [{:db/id "new TODO"
                               :todo/title title
                               :todo/done false}]]}
          {:error edit/error-message})))))
