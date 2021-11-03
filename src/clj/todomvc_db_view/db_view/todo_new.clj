(ns todomvc-db-view.db-view.todo-new
  (:require [todomvc-db-view.datomic.core :as datomic]
            [todomvc-db-view.db-view.todo-edit :as edit]))

(defn get-view
  "Provides the db-view for the `:todo/new!` command that creates new
   todo items."
  [db db-view-input]
  (when (= (:db-view/command db-view-input)
           [:todo/new!])
    (let [title (get-in db-view-input
                        [:todo/new!
                         :todo/title])]
      (if (edit/valid-title? title)
        {:todo/new! [#'datomic/transact!
                     [{:db/id "new TODO"
                       :todo/title title
                       :todo/done false}]]}
        {:error edit/error-message}))))
