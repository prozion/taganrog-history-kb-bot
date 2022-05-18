(ns odysseus.utils
  (:require [clojure.set :as set]
            [clojure.string :as s]
            ))

(defn index-of? [coll el]
  (and
    coll
    (not (empty? coll))
    (.contains coll el)))

(defn truthy? [val]
  (if val true false))

(defn falsy? [val]
  (not (truthy? val)))

(defn ->int [x]
  (Integer. (str x)))

(defn map-map [f m]
  (into (empty m) (map f m)))

(defn filter-map [f m]
  (into (empty m) (map f (filter f m))))

(defn minus [coll1 coll2]
  (into (empty coll1) (set/difference (set coll1) (set coll2))))

(defn symbol-split [sym]
  (map symbol (clojure.string/split (str sym) #"\.")))

(defn unique-concat [seq1 seq2]
  ; (into (empty seq1) (set/union (set seq1) (set seq2))))
  (concat
    seq1
    (remove
      #(index-of? seq1 %)
      seq2)))

(defn sort-by-order [seq given-order-v]
  (->> given-order-v (filter #(index-of? seq %)) (#(concat % (minus seq given-order-v)))))
