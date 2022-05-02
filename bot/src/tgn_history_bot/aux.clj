(ns tgn-history-bot.aux
  (:require [clojure.set :as set]
            [clojure.java.io :as io])
  (:use [clojure.java.io]))

(defn index-of? [coll el]
  (and
    coll
    (not (empty? coll))
    (.contains coll el)))

(defn ->int [x]
  (Integer. (str x)))

(defn map-map [f m]
  (into (empty m) (map f m)))

(defn filter-map [f m]
  (into (empty m) (filter f m)))

(defn minus [coll1 coll2]
  (into (empty coll1) (set/difference (set coll1) (set coll2))))

(defn symbol-split [sym]
  (map symbol (clojure.string/split (str sym) #"\.")))

(defn read-file-by-lines [filepath]
  (with-open [f (io/reader filepath)]
    (doall (line-seq f))))

(defn write-to-file [filepath content]
  (with-open [wrtr (writer filepath)]
    (.write wrtr content)))

(def --- println)
