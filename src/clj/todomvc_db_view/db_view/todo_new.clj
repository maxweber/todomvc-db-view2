(ns todomvc-db-view.db-view.todo-new
  (:require [todomvc-db-view.datomic.core :as datomic]
            [todomvc-db-view.db-view.todo-edit :as edit]))

(defn get-view
  "Provides the db-view for the `:todo/new!` command that creates new
   todo items."
  [db db-view-input]
  (when-let [title (get-in db-view-input
                           [:todo/new
                            :todo/title])]
    (when (= (:db-view/command db-view-input)
             [:todo/new :todo/new!])
      (if (edit/valid-title? title)
        {:todo/new {:todo/new! [#'datomic/transact!
                                [{:db/id "new TODO"
                                  :todo/title title
                                  :todo/done false}]]}}
        {:error edit/error-message}))))
