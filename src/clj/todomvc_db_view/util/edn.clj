(ns todomvc-db-view.util.edn
  (:require [clojure.edn :as edn])
  (:refer-clojure :exclude [read read-string pr-str]))

(defn read-string
  "Same as `clojure.edn/read-string` except that it sets
   `clojure.core/tagged-literal` as `:default` tag reader. Thereby this
   function can also read unknown tags like `#object` for example."
  ;; `tagged-literal` recommendation was found here:
  ;; http://insideclojure.org/2018/06/21/tagged-literal/
  ([s] (read-string {:eof nil} s))
  ([opts s] (edn/read-string (merge
                               {:default tagged-literal}
                               opts)
                             s)))

(defn pr-str
  "Just like `clojure.core/pr-str` but takes care that print related
   global vars are bound correctly, so that no data is truncated."
  [x]
  (binding [*print-length* nil
            *print-level* nil]
    (clojure.core/pr-str x)))