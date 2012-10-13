(ns korma.util
  (:use [clojure.string :only [blank? split]]))

(defn illegal-argument-exception [format-message & format-args]
  (throw (IllegalArgumentException. (apply format format-message format-args))))

(defn invoke-constructor [clazz & args]
  (clojure.lang.Reflector/invokeConstructor
   (Class/forName (str clazz)) (into-array args)))

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

(defn parse-subprotocol
  "Parse the JDBC subprotocol from `db-url`."
  [db-url]
  (if-let [matches (re-matches #"(([^:]+):)?([^:/]+):.+" (str db-url))]
    (keyword (nth matches 3))
    (illegal-argument-exception "Can't parse JDBC subprotocol: %s" db-url)))

(defn parse-db-url
  "Parse the database url `s` and return a Ring compatible map."
  [s]
  (if-let [matches (re-matches #"(([^:]+):)?([^:]+)://(([^:]+):([^@]+)@)?(([^:/]+)(:([0-9]+))?((/([^?]*))(\?(.*))?))" (str s))]
    (let [db (nth matches 13)
          server-name (nth matches 8)
          server-port (parse-integer (nth matches 10))
          query-string (nth matches 15)]
      {:db db
       :params (parse-params query-string)
       :password (nth matches 6)
       :pool (keyword (or (nth matches 2) :jdbc))
       :query-string query-string
       :server-name server-name
       :server-port server-port
       :subname (str "//" server-name (if server-port (str ":" server-port)) "/" db (if-not (blank? query-string) (str "?" query-string)))
       :subprotocol (keyword (nth matches 3))
       :uri (nth matches 12)
       :user (nth matches 5)})
    (illegal-argument-exception "Can't parse database connection url %s:" s)))