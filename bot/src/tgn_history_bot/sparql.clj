(ns tgn-history-bot.sparql
  (:require
            [clojure.string :as s]
            [jena-clj.triplestore :as ts :refer [with-transaction]]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            [org.clojars.prozion.clj-tabtree.aux :as aux :refer [---]]
            [org.clojars.prozion.clj-tabtree.rdf :as rdf])
  (:import (org.apache.jena.query ReadWrite)))

  (defonce db (ts/init-database "/var/db/jena/tgn-history"))

  (defn load-rdf [rdf-file-path]
    (with-transaction db ReadWrite/WRITE
      (ts/insert-rdf db rdf-file-path)))

  (defn query-sparql [sparql]
    (with-transaction db ReadWrite/READ
      (ts/select-query db sparql)))

  (defn init-db [& tree-files]
    (let [RDF-FILE "output/kb.ttl"
          tabtrees (map tabtree/parse-tab-tree tree-files)
          joined-tabtree (apply merge-with merge tabtrees)
          joined-rdf (rdf/to-rdf
                        joined-tabtree
                        :namespaces {"" "https://purl.org/taganrog#"
                                    "rdf" "https://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                    "owl" "http://www.w3.org/2002/07/owl#"})]
      (aux/write-to-file RDF-FILE joined-rdf)
      (load-rdf RDF-FILE)))

  ; (def find-historical-quarters (make-query-func
  ;                   "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
  ;                    prefix : <https://purl.org/taganrog#>
  ;                    prefix owl: <http://www.w3.org/2002/07/owl#>
  ;                    SELECT *
  ;                    WHERE {?%s :has_building :%s.}
  ;                    LIMIT 10"
  ;                   :block))

  (defn get-house-info [address]
    (let [result
          (->>
            (query-sparql
              (s/replace
                "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 prefix : <https://purl.org/taganrog#>
                 prefix owl: <http://www.w3.org/2002/07/owl#>
                 SELECT ?description ?quarter ?year ?url
                 WHERE {
                   :?x :description ?description .
                   OPTIONAL { ?quarter :has_building :?x }
                   OPTIONAL { :?x :year ?year }
                   OPTIONAL { :?x :url ?url }
                 }
                 LIMIT 10"
                 #"\?x"
                 address))
            ts/result->map
            first)]
      (merge result {:normalized-address address})))
