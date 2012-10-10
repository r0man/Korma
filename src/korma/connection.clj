(ns korma.connection
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [inflections.core :refer [dasherize underscore]]
            [korma.util :refer [defn-memo parse-db-url parse-integer]]))

(def ^:dynamic *c3p0-settings*
  {:initial-pool-size 3
   :max-idle-time (* 3 60 60)
   :max-idle-time-excess-connections (* 30 60)
   :max-pool-size 15
   :min-pool-size 3})

(def ^:dynamic *naming-strategy*
  {:entity underscore :keyword dasherize})

(defn connection-url
  "Lookup the database connection url for `database`."
  [database]
  (if-let [url (env database)]
    url (throw (IllegalArgumentException. (format "Can't find connection url via environ: %s" database)))))

(defn connection-spec
  "Returns the connection spec for `database`."
  [database]
  (cond
   (keyword? database)
   (connection-spec (connection-url database))
   (map? database)
   database
   (string? database)
   (parse-db-url database)
   :else (throw (IllegalArgumentException. (format "Can't find connection: %s" database)))))

(defn-memo connection-pool
  "Make a C3P0 connection pool for `database`."
  [database]
  (if-let [database (connection-spec database)]
    (let [params (merge *c3p0-settings* (:params database))]
      {:datasource
       (doto (ComboPooledDataSource.)
         (.setDriverClass (:classname database))
         (.setJdbcUrl (str "jdbc:" (:subprotocol database) ":" (:subname database)))
         (.setUser (:user database))
         (.setPassword (:password database))
         (.setInitialPoolSize (:initial-pool-size params))
         (.setMaxIdleTimeExcessConnections (parse-integer (:max-idle-time-excess-connections params)))
         (.setMaxIdleTime (parse-integer (:max-idle-time params)))
         (.setMaxPoolSize (parse-integer (:max-pool-size params)))
         (.setMinPoolSize (parse-integer (:min-pool-size params))))})
    (throw (IllegalArgumentException. (format "Can't find connection pool: %s" database)))))

(defmacro with-connection
  "Evaluates body in the context of a connection to the database
  `name`. The connection spec for `name` is looked up via environ."
  [database & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (connection-spec ~database)
       ~@body)))

(defmacro with-connection-pool
  "Evaluates body in the context of a connection to the database
  `name`. The connection spec for `name` is looked up via environ."
  [database & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (connection-pool ~database)
       ~@body)))

(defn wrap-connection
  "Returns a Ring handler with an open connection to the `database`."
  [handler database]
  (fn [request]
    (with-connection database
      (handler request))))

(defn wrap-connection-pool
  "Returns a Ring handler with an open connection from a C3P0 pool to
  the `database`."
  [handler database]
  (fn [request]
    (with-connection-pool database
      (handler request))))
