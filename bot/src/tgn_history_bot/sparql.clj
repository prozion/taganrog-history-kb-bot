(ns tgn-history-bot.sparql
  (:require
            [clojure.string :as s]
            [jena-clj.triplestore :as ts :refer [with-transaction]]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            [odysseus.debug :refer :all]
            [odysseus.io :as io]
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
        ; _ (--- (:Итальянский_78 joined-tabtree))
        joined-rdf (rdf/to-rdf
                      joined-tabtree
                      :namespaces {"" "https://purl.org/taganrog#"
                                  "rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                  "rdfs" "http://www.w3.org/2000/01/rdf-schema#"
                                  "owl" "http://www.w3.org/2002/07/owl#"})]
    (io/write-to-file RDF-FILE joined-rdf)
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
  (let [result (->>
                  (query-sparql
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?description ?quarter ?year ?url ?title
                       WHERE {
                         OPTIONAL { :?x :description ?description }
                         OPTIONAL { ?quarter :has_building :?x }
                         OPTIONAL { ?quarter :has_building ?t.
                                    ?t :eq :?x }
                         OPTIONAL { :?x :built ?year }
                         OPTIONAL { :?x :url ?url }
                         OPTIONAL { :?x :title ?title }
                       }
                       LIMIT 10"
                       #"\?x"
                       address))
                  ts/result->map
                  first)]
    (and result (merge result {:normalized-address address}))))

(defn list-houses-on-the-street [street]
  ; (--- 111
  ;     (ts/result->map
  ;       (query-sparql
  ;         "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
  ;          prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
  ;          prefix : <https://purl.org/taganrog#>
  ;          prefix owl: <http://www.w3.org/2002/07/owl#>
  ;          SELECT ?object
  ;          WHERE {
  ;            ?object rdf:type :House .
  ;            ?object :description ?description .
  ;          }")))
  (let [result (some->>
                  (query-sparql
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?house
                       WHERE {
                         ?house rdf:type :House .
                         ?house :street :<street> .
                         ?house :description ?description .
                       }"
                       #"<street>"
                       street))
                  ts/result->map)]
      result))
