(ns todomvc-db-view.datomic.core
  (:require [datomic.api :as d]))

(def db-uri
  "datomic:mem://todomvc-db-view")

(def example-data
  [{:todo/title "Create an example"
    :todo/done true}
   {:todo/title "Write a blog post"}
   {:todo/title "Publish the blog post"}])

(def schema
  [{:db/ident :todo/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Title of the TODO entry."}

   {:db/ident :todo/done
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Marks the TODO entry as done."}

   {:db/ident :command/uuid
    :db/valueType :db.type/uuid
    :db/unique :db.unique/value
    :db/cardinality :db.cardinality/one
    :db/doc "Ensures that the command is transacted at most once."}
   ])

(defn connect!
  []
  (d/create-database db-uri)
  (let [con (d/connect db-uri)]
    @(d/transact con
                 schema)
    @(d/transact con
                 example-data)
    con))

(defonce ^:private connection
  (delay (connect!)))

(defn con
  "Returns the current Datomic connection."
  []
  @connection)

(defn db
  "Returns the latest db value."
  []
  (d/db (con)))

(defn transact!
  "Transacts the `transaction` via the Datomic `connection` and waits
   for the transaction report. Adds the `transaction` to the exception
   info in the case of an error."
  [transaction]
  (try
    @(d/transact (con)
                 transaction)
    (catch Exception e
      (throw (ex-info "Datomic transact failed"
                      {:transaction transaction}
                      e)))))

(defn retract-entity!
  "Retracts an entity."
  [entity-id]
  (transact! [[:db/retractEntity entity-id]]))
