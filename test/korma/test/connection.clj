(ns korma.test.connection
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           com.mchange.v2.c3p0.impl.NewProxyConnection
           java.sql.Connection)
  (:require [clojure.java.jdbc :as jdbc])
  (:use korma.connection
        korma.util
        korma.test
        clojure.test))

(database-test test-connection-url
  (is (thrown? IllegalArgumentException (connection-url :unknown-db)))
  (is (re-matches (re-pattern (str (name (:vendor *database*)) "://.*korma.*")) (connection-url (:vendor *database*)))))

(database-test test-connection-spec
  (is (thrown? IllegalArgumentException (connection-spec :unknown-db)))
  (is (map? (connection-spec (:vendor *database*))))
  (is (map? (connection-spec (:url *database*)))))

(database-test test-connection-pool
  (let [pool (connection-pool (:vendor *database*))]
    (is (map? pool))
    (let [datasource (:datasource pool)]
      (is (instance? ComboPooledDataSource datasource))
      (is (re-matches #".*korma.*" (.getJdbcUrl datasource)))
      (is (= 15 (.getMaxPoolSize datasource)))
      (is (= 10800 (.getMaxIdleTime datasource)))
      (is (= 1800 (.getMaxIdleTimeExcessConnections datasource))))))

(database-test test-with-connection
  (with-connection (:vendor *database*)
    (is (instance? Connection (jdbc/connection))))
  (is (thrown? IllegalArgumentException (with-connection :unknown))))

(database-test test-with-connection-pool
  (with-connection-pool (:vendor *database*)
    (is (instance? NewProxyConnection (jdbc/connection)))))

(database-test test-wrap-connection
  ((wrap-connection
    (fn [request]
      (is (instance? Connection (jdbc/connection))))
    (:vendor *database*))
   {}))

(database-test test-wrap-connection-pool
  ((wrap-connection-pool
    (fn [request]
      (is (instance? NewProxyConnection (jdbc/connection))))
    (:vendor *database*))
   {}))
