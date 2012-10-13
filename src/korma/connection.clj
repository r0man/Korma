(ns korma.connection
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [inflections.core :refer [dasherize underscore]]
            [korma.util :as util]))

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
  "Lookup the JDBC connection url for `db-name` via environ."
  [db-name]
  (or (env db-name)
      (util/illegal-argument-exception "Can't find connection url: %s" db-name)))

(defmulti connection-spec
  "Parse `db-url` and return the connection spec."
  (fn [db-url] (keyword (util/parse-subprotocol db-url))))

(defmethod connection-spec :mysql [db-url]
  (assoc (util/parse-db-url db-url)
    :classname "com.mysql.jdbc.Driver"))

(defmethod connection-spec :oracle [db-url]
  (let [{:keys [server-name server-port] :as url} (util/parse-db-url db-url)]
    (assoc url
      :classname "oracle.jdbc.driver.OracleDriver"
      :subprotocol "oracle:thin"
      :subname (str ":" (:user url) "/" (:password url) "@" server-name (if server-port (str ":" server-port)) ":" (:db url)))))

(defmethod connection-spec :postgresql [db-url]
  (assoc (util/parse-db-url db-url)
    :classname "org.postgresql.Driver"))

(defmethod connection-spec :sqlite [db-url]
  (if-let [matches (re-matches #"(([^:]+):)?([^:]+):([^?]+)(\?(.*))?" (str db-url))]
    {:classname "org.sqlite.JDBC"
     :pool (keyword (or (nth matches 2) :jdbc))
     :subname (nth matches 4)
     :subprotocol (nth matches 3)
     :params (util/parse-params (nth matches 5))}))

(defmethod connection-spec :sqlserver [db-url]
  (let [url (util/parse-db-url db-url)]
    (assoc url
      :classname "com.microsoft.sqlserver.jdbc.SQLServerDriver"
      :subprotocol "sqlserver"
      :subname (str "//" (:server-name url) ":" (:server-port url) ";database=" (:db url) ";user=" (:user url) ";password=" (:password url)))))

(defmulti connection-pool
  "Returns the connection pool for `db-spec`."
  (fn [db-spec] (:pool db-spec)))

(defmethod connection-pool :bonecp [db-spec]
  (let [config (util/invoke-constructor "com.jolbox.bonecp.BoneCPConfig")]
    (.setJdbcUrl config (str "jdbc:" (name (:subprotocol db-spec)) ":" (:subname db-spec)))
    (.setUsername config (:user db-spec))
    (.setPassword config (:password db-spec))
    {:datasource (util/invoke-constructor "com.jolbox.bonecp.BoneCPDataSource" config)}))

(defmethod connection-pool :c3p0 [db-spec]
  (let [params (merge *c3p0-settings* (:params db-spec))
        datasource (util/invoke-constructor "com.mchange.v2.c3p0.ComboPooledDataSource")]
    (.setAcquireRetryAttempts datasource (util/parse-integer (:acquire-retry-attempts params)))
    (.setDriverClass datasource (:classname db-spec))
    (.setInitialPoolSize datasource (:initial-pool-size params))
    (.setJdbcUrl datasource (str "jdbc:" (name (:subprotocol db-spec)) ":" (:subname db-spec)))
    (.setMaxIdleTime datasource (util/parse-integer (:max-idle-time params)))
    (.setMaxIdleTimeExcessConnections datasource (util/parse-integer (:max-idle-time-excess-connections params)))
    (.setMaxPoolSize datasource (util/parse-integer (:max-pool-size params)))
    (.setMinPoolSize datasource (util/parse-integer (:min-pool-size params)))
    (.setPassword datasource (:password db-spec))
    (.setUser datasource (:user db-spec))
    {:datasource datasource}))

(defn connection [db-spec]
  "Returns the db-spec connection for `db-spec`."
  (cond
   (keyword? db-spec)
   (connection (connection-url db-spec))
   (string? db-spec)
   (let [db-spec (connection-spec db-spec)]
     (if (= :jdbc (:pool db-spec))
       db-spec (connection-pool db-spec)))
   :else db-spec))

(util/defn-memo cached-connection [db-spec]
  "Returns the cached db-spec connection for `db-spec`."
  (connection db-spec))

(defmacro with-connection
  "Evaluates `body` with a connection to `db-spec`."
  [db-spec & body]
  `(jdbc/with-naming-strategy *naming-strategy*
     (jdbc/with-connection (cached-connection ~db-spec)
       ~@body)))

(defn wrap-connection
  "Wraps a connection to `db-spec` around the Ring `handler`"
  [handler db-spec]
  (fn [request]
    (with-connection db-spec
      (handler request))))
