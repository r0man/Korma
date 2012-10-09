(ns korma.util
  (:require [clojure.string :refer [blank? split]]))

(defmacro defn-memo
  "Just like defn, but memoizes the function using clojure.core/memoize"
  [fn-name & defn-stuff]
  `(do
     (defn ~fn-name ~@defn-stuff)
     (alter-var-root (var ~fn-name) memoize)
     (var ~fn-name)))

(defn parse-integer
  "Parse `s` as an integer."
  [s]
  (try (Integer/parseInt (str s))
       (catch NumberFormatException _ nil)))

(defn parse-params
  "Parse `s` as a query string and return a hash map."
  [s] (->> (split (or s "") #"&")
           (remove blank?)
           (map #(split %1 #"="))
           (mapcat #(vector (keyword (first %1)) (second %1)))
           (apply hash-map)))

(defn parse-db-url
  "Parse the database url `s` and return a clojure.java.jdbc and Ring
  compatible map."
  [s]
  (if-let [matches (re-matches #"(jdbc:)?([^:]+)://(([^:]+):([^@]+)@)?(([^:/]+)(:([0-9]+))?((/([^?]*))(\?(.*))?))" (str s))]
    (let [db (nth matches 12)
          scheme (nth matches 2)
          server-name (nth matches 7)
          server-port (parse-integer (nth matches 9))]
      {:db db
       :host (nth matches 7)
       :password (nth matches 5)
       :port server-port
       :scheme scheme
       :server-name server-name
       :server-port server-port
       :subname (str "//" server-name (if server-port (str ":" server-port)) "/" db)
       :subprotocol scheme
       :uri (nth matches 11)
       :user (nth matches 4)
       :params (parse-params (nth matches 14))
       :query-string (nth matches 14)})))