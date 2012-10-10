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
  (is (re-matches #".*korma.*" (connection-url (:vendor *database*)))))

(database-test test-make-connection-pool
  (let [pool (make-connection-pool (:vendor *database*))]
    (is (map? pool))
    (let [datasource (:datasource pool)]
      (is (instance? ComboPooledDataSource datasource))
      (is (re-matches #".*korma.*" (.getJdbcUrl datasource)))
      (is (= 15 (.getMaxPoolSize datasource)))
      (is (= 10800 (.getMaxIdleTime datasource)))
      (is (= 1800 (.getMaxIdleTimeExcessConnections datasource)))
      (.close datasource))))

(database-test test-resolve-connection
  (is (= (connection-url (:vendor *database*))
         (resolve-connection (:vendor *database*))))
  (is (= (connection-url (:vendor *database*))
         (resolve-connection (connection-url (:vendor *database*))))))

(database-test test-resolve-connection-pool
  (let [pool (resolve-connection-pool (:vendor *database*))]
    (is (map? pool))
    (is (re-matches #".*korma.*" (.getJdbcUrl (:datasource pool))))
    (is (= (:datasource pool) (:datasource (resolve-connection-pool (:vendor *database*)))))))

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
