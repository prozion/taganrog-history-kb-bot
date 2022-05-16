(ns taganrog-history-bot.test-rdf
  (:require [clojure.test :refer :all]
            [jena-clj.triplestore :as ts :refer [with-transaction]]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            [org.clojars.prozion.clj-tabtree.aux :as aux :refer [---]]
            [org.clojars.prozion.clj-tabtree.rdf :as rdf])
  (:import (org.apache.jena.query ReadWrite)))

; (import '[org.apache.jena.query ReadWrite])
           ; [org.apache.jena.rdf.model ModelFactory]

(defonce db (ts/init-database "/var/db/jena/tgn-history"))

(def FILE "blocks")

(def rdf-file-path (format "test/taganrog_history_bot/output/%s.ttl" FILE))

(defn test-file-write []
  (let [tabtree (tabtree/parse-tab-tree (format "test/taganrog_history_bot/fixtures/%s.tree" FILE))]
    (aux/write-to-file
      rdf-file-path
      (rdf/to-rdf
        tabtree
        :namespaces {"" "https://purl.org/denis-shirshov#"
                    "rdf" "https://www.w3.org/1999/02/22-rdf-syntax-ns#"
                    "owl" "http://www.w3.org/2002/07/owl#"}))
    true))

(defn load-rdf []
  (with-transaction db ReadWrite/WRITE
    (ts/insert-rdf db rdf-file-path)))

(defn query-sparql [sparql]
  (with-transaction db ReadWrite/READ
    (ts/select-query db sparql)))

(defn make-query-func [sparql-frmt queried-parameter]
  (fn [parameter-value]
    (->>
      (query-sparql (format sparql-frmt (name queried-parameter) parameter-value))
      ts/result->map
      (map queried-parameter))))

(test-file-write)
(load-rdf)

(def find-historical-quarters (make-query-func
                  "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                   prefix : <https://purl.org/denis-shirshov#>
                   prefix owl: <http://www.w3.org/2002/07/owl#>
                   SELECT *
                   WHERE {?%s :has_building :%s.}
                   LIMIT 10"
                  :block))
