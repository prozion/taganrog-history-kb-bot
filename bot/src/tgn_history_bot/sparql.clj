(ns tgn-history-bot.sparql
  (:require
            [clojure.string :as s]
            [jena-clj.triplestore :as ts :refer [with-transaction]]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            [org.clojars.prozion.clj-tabtree.aux :as aux]
            [org.clojars.prozion.clj-tabtree.rdf :as rdf])
  (:import (org.apache.jena.query ReadWrite)))

  (defonce db (ts/init-database "/var/db/jena/tgn-history"))

  (def FILE "blocks")

  (def rdf-file-path (format "test/taganrog_history_bot/output/%s.ttl" FILE))

  (defn tree-file->rdf-file [tree-file rdf-file]
    (let [tabtree (tabtree/parse-tab-tree tree-file)]
      (aux/write-to-file
        rdf-file
        (rdf/to-rdf
          tabtree
          :namespaces {"" "https://purl.org/taganrog#"
                      "rdf" "https://www.w3.org/1999/02/22-rdf-syntax-ns#"
                      "owl" "http://www.w3.org/2002/07/owl#"}))
      true))

  (defn load-rdf [rdf-file-path]
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

  (defn init-db [tree-file-path]
    (let [tree-file-name (-> tree-file-path (s/split #"/") last (s/split #"\.") first)
          rdf-file-path (format "output/%s.ttl" tree-file-name)]
      (tree-file->rdf-file tree-file-path rdf-file-path)
      (load-rdf rdf-file-path)))

  (def find-historical-quarters (make-query-func
                    "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                     prefix : <https://purl.org/taganrog#>
                     prefix owl: <http://www.w3.org/2002/07/owl#>
                     SELECT *
                     WHERE {?%s :has_building :%s.}
                     LIMIT 10"
                    :block))
