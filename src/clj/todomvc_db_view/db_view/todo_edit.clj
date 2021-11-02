(ns todomvc-db-view.db-view.todo-edit
  (:require [todomvc-db-view.datomic.core :as datomic]
            [datomic.api :as d]))

(defn valid-title?
  [title]
  ;; Example for a validation which ensures that the `:todo/title` is
  ;; longer than 2 characters after it has been edited in the
  ;; client:
  (and (string? title)
       (> (count title)
          2)))

(def error-message
  "Title must be longer than 2 characters!")

(defn get-view
  "Provides the db-view to validate the input for a `:todo/edit!`
   command."
  [db db-view-input]
  (let [{:keys [db/id todo/title]} (:todo/edit! db-view-input)]
    (when (and (string? title)
               (integer? id)
               ;; is it a todo item entity?
               (:todo/title (d/entity db
                                      id)))

      (if (valid-title? title)
        {:db-view/command [#'datomic/transact!
                           [{:db/id id
                             :todo/title title}]]}
        {:error error-message}))))
