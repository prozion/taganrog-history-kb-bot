(ns taganrog-history-bot.ontologies
  (:require
            [clojure.string :as s]
            [jena.triplestore :as ts]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.time :as time]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.odysseus.text :as text]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [org.clojars.prozion.tabtree.utils :as tabtree-utils]
            [org.clojars.prozion.tabtree.rdf :as rdf]
            [taganrog-history-bot.globals :refer :all]
))

(def RDF_DIR "../taganrog-history-kb/_export/")

(defn merge-ontology-parts [& ontology-tabtrees]
  (let [all-namespaces (apply
                    merge
                    (map #(tabtree-utils/get-subtree [:namespaces] %) ontology-tabtrees))
        all-namespaces (conj
                          all-namespaces
                          {:namespaces {:__id :namespaces :__children (keys (dissoc all-namespaces :namespaces))}})
        merged-tabtrees (merge
                          (apply merge-with merge ontology-tabtrees)
                          all-namespaces)
        ontology-parts-root-ids
                (->> merged-tabtrees
                    (filter
                      (fn [[k v]]
                        (= (tabtree/get-item-parameter :a v) :tabtree/OntologyPart)))
                    keys)
        merged-tabtrees (apply dissoc merged-tabtrees ontology-parts-root-ids )
       ]
    merged-tabtrees))

(defn make-rdf-files [tabtrees-m]
  (for [[k v] tabtrees-m]
    (let [tabtree-files v
          tabtree-files (if (coll? tabtree-files) tabtree-files (list tabtree-files))
          tabtrees (map tabtree/parse-tab-tree tabtree-files)
          merged-tabtree (apply merge-ontology-parts tabtrees)
          rdf-string (rdf/tabtree->rdf merged-tabtree)
          rdf-filepath (str RDF_DIR (name (or k "file")) ".ttl")]
      (io/write-to-file rdf-filepath rdf-string)
      rdf-filepath)))

(defn init-db [tabtrees-m & {:keys [reasoner] :default {:reasoner nil}}]
  (let [rdf-files (make-rdf-files tabtrees-m)]
    (if reasoner
      (apply ts/init-db-with-reasoner reasoner DATABASE_PATH rdf-files)
      (apply ts/init-db DATABASE_PATH rdf-files))))

(defn init-part-of-broken-rdf []
  (ts/init-db DATABASE_PATH "/home/denis/projects/taganrog-history-kb/_export/city_ontology_part.ttl"))
