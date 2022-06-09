(ns taganrog-history-bot.city
  (:require
    [clojure.string :as s]
    [org.clojars.prozion.tabtree.tabtree :as tabtree]
    [org.clojars.prozion.odysseus.debug :refer :all]
    [org.clojars.prozion.odysseus.utils :refer :all]
    [org.clojars.prozion.odysseus.text :as text]))

(def ADDRESSES (tabtree/parse-tab-tree "../taganrog-history-kb/factbase/streets/ids.tree"))

(defn normalize-address
  ([address-string]
    (-> address-string
        s/lower-case
        (s/replace "null" "")
        (s/replace "," "")
        (s/replace "/" "-")
        (s/replace #"(\b((ул\.\s*)|(ул\s+)|(пер\.\s*)|(пер\s+)|(д\.\s*)|((?<=\s)д\s+)|(туп\.\s*)|(туп\s+)))" "")
        (s/replace #"\b(улица)|(переулок)|(дом)|(тупик)\s+" "")
        (s/replace #"(\bulitsa\s+)|(\bul\.?\s+)|(\bper\.?\s+)|(\bpereulok\s+)|(\bdom\s+)" "")
        (s/replace "." "")
        (s/replace #"(\s+корп\.?\s+)|(\s+корпус\s+)" "-")
        (s/replace #"(\s+пл\.?\s+)" "площадь")
        (s/replace #"(\s+бул\.?\s+)" "бульвар")
        (s/replace #"антона\s*" "") ; Антона Глушко -> Глушко
        (s/replace "grecheskaya" "греческая")
        (s/replace "petrovskaya" "петровская")
        (s/replace "italyansky" "итальянский")
        (s/replace "frunze" "фрунзе")
        (s/replace "chekhova" "чехова")
        (s/replace #"(-й)|(-я)" "")
        (s/replace #"\s+" " ")
        s/trim
        ((fn [s] (s/join "_" (map s/capitalize (s/split s #" ")))))))
  ([street housenumber]
    (let [housenumber (-> housenumber
                          (or "")
                          (s/split #"/|\s")
                          first)
          street (or street "неизвестная")] ; use of "?" causes problems in downstream sparql requests
      (some->
        (format
          "%s %s"
          street
          housenumber)
        normalize-address))))

(defn get-canonical-address [normalized-address]
  (and
    normalized-address
    (let [parts (s/split normalized-address #"_")
          street-id (keyword (s/join "_" (butlast parts)))
          street-canonical (or
                              (get-in ADDRESSES [street-id :short-canonical])
                              (get-in ADDRESSES [street-id :canonical])
                              street-id)
          number (last parts)
          number-parts (s/split number #"-")
          first-number (first number-parts)
          block-number (second number-parts)
          canonical-address (format
                              "%s, %s%s"
                              (or street-canonical (name street-id))
                              first-number
                              (if block-number
                                  (format ", корпус %s" block-number)
                                  ""))]
      canonical-address)))

(defn normalize-title [title]
  (-> title
    (s/replace "«" "")
    (s/replace "»" "")
    (s/replace ")" "")
    (s/replace "(" "")
    normalize-address))

; (defn get-historical-quarter [address-text]
;   (some-> address-text normalize-address sparql/find-historical-quarters first))

(defn break-up-address [address]
  (cond
    (empty? address) []
    (nil? address) []
    :else
    (let [parts (s/split address #"_")
          streetname (s/join "_" (butlast parts))
          number-part (last parts)
          blocks (s/split number-part #"-")
          block-parts (map #(rest (re-matches #"(\d+)(.*)" %)) blocks)]
      (remove
        #(or
          (not %)
          (and (string? %) (empty? %)))
        (flatten
          `(~streetname
            ~(map
                #(list (some-> % first ->int) (rest %))
                block-parts)))))))

(defn compare-addresses [address1 address2]
  (loop [address-parts1 (break-up-address address1)
         address-parts2 (break-up-address address2)]
    (cond
      (empty? address-parts1) -1
      (empty? address-parts2) 1
      :else
      (let [comparison-result (compare (first address-parts1) (first address-parts2))]
        (if (= 0 comparison-result)
          (recur (rest address-parts1) (rest address-parts2))
          comparison-result)))))

(defn build-photo-list [data-m]
  (let [photos (:photo data-m)]
    (reduce
      (fn [acc photo]
        (if photo
          (format "%s\n<a href=\"%s\">%s</a>" acc photo photo)
          acc))
      (format "\n%s\n\n" (text/boldify "Фотографии:"))
      (if (coll? photos) photos (list photos)))))

(defn get-address-chunks [id]
  (map
    (fn [chunk]
      (or (->integer chunk) chunk))
    (s/split id #"(?<=\b)(_|-)[1-9]?(?=\b)")))

(defn compare-address [id1 id2]
  (let [chunks1 (get-address-chunks id1)
        chunks2 (get-address-chunks id2)]
    (->>
        (cond
          (empty? (or chunks1 [])) [-1]
          (empty? (or chunks2 [])) [1]
          :else
            (map (fn [c1 c2]
                    (cond
                      (or (string? c1) (string? c2))
                        (compare (str c1) (str c2))
                      :else
                        (compare c1 c2)))
                  chunks1
                  chunks2))
        (remove #(= % 0))
        (#(if (empty? %) 0 (first %))))))
