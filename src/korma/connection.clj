(ns korma.connection
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [inflections.core :refer [dasherize underscore]]
            [korma.util :refer [defn-memo illegal-argument-exception parse-db-url parse-integer]]))

(def ^:dynamic *c3p0-settings*
  {:initial-pool-size 3
   :max-idle-time (* 3 60 60)
   :max-idle-time-excess-connections (* 30 60)
   :max-pool-size 15
   :min-pool-size 3})

(def ^:dynamic *naming-strategy*
  {:entity underscore :keyword dasherize})

(defn connection-url
  "Returns the connection url for `database`."
  [database]
  (or (env database)
      (illegal-argument-exception "Can't find connection url: %s" database)))

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
   :else (illegal-argument-exception "Can't find connection spec: %s" database)))

(defn-memo c3p0-pool
  "Returns the cached connection pool for `database`."
  [database]
  (if-let [database (connection-spec database)]
    (if-let [clazz (Class/forName "com.mchange.v2.c3p0.ComboPooledDataSource")]
      (let [params (merge *c3p0-settings* (:params database))]
        {:datasource
         (doto (clojure.lang.Reflector/invokeConstructor clazz (into-array []))
           (.setDriverClass (:classname database))
           (.setJdbcUrl (str "jdbc:" (:subprotocol database) ":" (:subname database)))
           (.setUser (:user database))
           (.setPassword (:password database))
           (.setInitialPoolSize (:initial-pool-size params))
           (.setMaxIdleTimeExcessConnections (parse-integer (:max-idle-time-excess-connections params)))
           (.setMaxIdleTime (parse-integer (:max-idle-time params)))
           (.setMaxPoolSize (parse-integer (:max-pool-size params)))
           (.setMinPoolSize (parse-integer (:min-pool-size params))))})
      (throw (Exception. "Can't find the C3P0 library on class path.")))
    (illegal-argument-exception "Can't find connection pool: %s" database)))

(defmacro with-c3p0-pool
  "Evaluates `body` with a pooled connection to `database`."
  [database & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (c3p0-pool ~database)
       ~@body)))

(defmacro with-connection
  "Evaluates `body` with a connection to `database`."
  [database & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (connection-spec ~database)
       ~@body)))

(defn wrap-c3p0-pool
  "Wraps a pooled connection to `database` around the Ring `handler`"
  [handler database]
  (fn [request]
    (with-c3p0-pool database
      (handler request))))

(defn wrap-connection
  "Wraps a connection to `database` around the Ring `handler`"
  [handler database]
  (fn [request]
    (with-connection database
      (handler request))))
