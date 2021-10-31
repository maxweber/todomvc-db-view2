(ns todomvc-db-view.db-view.command
  "The API endpoint '/db-view/command' makes it possible for the client
   to execute a command on the server. The server's responsibility is
   to validate, if the client or rather the current logged-in user is
   allowed to perform this command. For that reason the client must
   also include the current `:db-view/params` in the request. The
   server takes these and runs the db-view queries to receive the
   `:db-view/value`. It then checks if the provided command is included
   in the `:db-view/value`. The client must provide the command under
   the `:db-view/command` in the `:db-view/params`.

   Alternatively the client has the option to provide a
   `:db-view/command-path` in the `:db-view/params`, in the case that
   it already knows the path, where the desired command will reside in
   the resulting `:db-view/value`.

   The API endpoint should not provide any command batching, it only
   receives one command at a time. Batching of multiple commands can be
   implemented by providing a single command that includes multiple
   command maps. Thereby the server has the control how many commands
   are processed at once (in the correct order; probably in single
   database transaction).
  "
  (:require [todomvc-db-view.util.edn :as edn]
            [todomvc-db-view.db-view.get :as get]
            [todomvc-db-view.db-view.notify :as notify]))

(defn contains-elem?
  "Traverse the collection to check if it contains the `element`."
  [coll element]
  (some (fn [x]
          (= x element))
        (tree-seq coll? seq coll)))

(defn try-find-var
  [sym]
  (try
    (find-var sym)
    (catch Exception _e
      nil)))

(defn command!
  [db-view-params db-view-value]
  (when-let [command (update (:db-view/command db-view-params)
                             0
                             try-find-var)]
    ;; Only the server code should be able to add a var
    ;; (clojure.lang.Var) to a db-view value. Transit,
    ;; `clojure.edn/read-string` and even `clojure.core/read-string`
    ;; does not support vars out of the box. Pay attention to not add
    ;; a corresponding value handler, otherwise it might be possible
    ;; to inject a command here:
    (when (and (var? (first command))
               (contains-elem? db-view-value
                               command))
      (let [result (try
                     (apply (first command)
                            (rest command))
                     (notify/notify!)
                     (catch Exception e
                       (throw (ex-info "handle-command failed"
                                       {:command command}
                                       e))))]
        (merge {:status :ok}
               ;; a command handler can return a result:
               (select-keys result
                            [:command/result]))))))

(defn ring-handler
  [db request]
  (when (and (= (:request-method request) :post)
             (= (:uri request) "/db-view/command"))
    (let [db-view-params (edn/read-string (slurp (:body request)))
          db-view-value (get/get-view db
                                      db-view-params)]
      (some-> (command! db-view-params
                        db-view-value)
              (edn/response)))))
