(ns korma.connection
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [inflections.core :refer [dasherize underscore]]
            [korma.util :refer [defn-memo illegal-argument-exception parse-db-url parse-integer]]))

(def ^:dynamic *bone-cp-settings* {})

(def ^:dynamic *c3p0-settings*
  {:acquire-retry-attempts 1 ; TODO: Set back to 30
   :initial-pool-size 3
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

(defn- invoke-constructor [clazz & args]
  (clojure.lang.Reflector/invokeConstructor
   (Class/forName (str clazz)) (into-array args)))

(defn-memo bone-cp-pool
  "Returns the cached BoneCP connection pool for `database`."
  [database]
  (if-let [database (connection-spec database)]
    (let [params (merge *bone-cp-settings* (:params database))]
      {:datasource
       (->> (doto (invoke-constructor "com.jolbox.bonecp.BoneCPConfig")
              (.setJdbcUrl (str "jdbc:" (:subprotocol database) ":" (:subname database)))
              (.setUsername (:user database))
              (.setPassword (:password database)))
            (invoke-constructor "com.jolbox.bonecp.BoneCPDataSource"))})
    (illegal-argument-exception "Can't find connection pool: %s" database)))

(defn-memo c3p0-pool
  "Returns the cached C3P0 connection pool for `database`."
  [database]
  (if-let [database (connection-spec database)]
    (let [params (merge *c3p0-settings* (:params database))]
      (if (:classname database)
        (Class/forName (:classname database)))
      {:datasource
       (doto (invoke-constructor "com.mchange.v2.c3p0.ComboPooledDataSource")
         (.setDriverClass (:classname database))
         (.setJdbcUrl (str "jdbc:" (:subprotocol database) ":" (:subname database)))
         (.setUser (:user database))
         (.setPassword (:password database))
         (.setAcquireRetryAttempts (parse-integer (:acquire-retry-attempts params)))
         (.setInitialPoolSize (:initial-pool-size params))
         (.setMaxIdleTimeExcessConnections (parse-integer (:max-idle-time-excess-connections params)))
         (.setMaxIdleTime (parse-integer (:max-idle-time params)))
         (.setMaxPoolSize (parse-integer (:max-pool-size params)))
         (.setMinPoolSize (parse-integer (:min-pool-size params))))})
    (illegal-argument-exception "Can't find connection pool: %s" database)))

(defmacro with-bone-cp-pool
  "Evaluates `body` with a pooled BoneCP connection to `database`."
  [database & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (bone-cp-pool ~database)
       ~@body)))

(defmacro with-c3p0-pool
  "Evaluates `body` with a pooled C3P0 connection to `database`."
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

(defn wrap-bone-cp-pool
  "Wraps a pooled BoneCP connection to `database` around the Ring `handler`"
  [handler database]
  (fn [request]
    (with-bone-cp-pool database
      (handler request))))

(defn wrap-c3p0-pool
  "Wraps a pooled C3P0 connection to `database` around the Ring `handler`"
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
