(ns korma.test.connection
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           com.mchange.v2.c3p0.impl.NewProxyConnection
           java.sql.Connection)
  (:require [clojure.java.jdbc :as jdbc])
  (:use korma.connection
        korma.util
        korma.test
        clojure.test))

(deftest test-connection-url
  (is (thrown? IllegalArgumentException (connection-url :unknown-db)))
  (is (re-matches #".*korma.*" (connection-url (:vendor *database*)))))

(deftest test-make-connection-pool
  (let [pool (make-connection-pool (:vendor *database*))]
    (is (map? pool))
    (let [datasource (:datasource pool)]
      (is (instance? ComboPooledDataSource datasource))
      (is (re-matches #".*korma.*" (.getJdbcUrl datasource)))
      (is (= 15 (.getMaxPoolSize datasource)))
      (is (= 10800 (.getMaxIdleTime datasource)))
      (is (= 1800 (.getMaxIdleTimeExcessConnections datasource)))
      (.close datasource))))

(deftest test-resolve-connection
  (prn (:vendor *database*))
  (is (= (connection-url (:vendor *database*))
         (resolve-connection (:vendor *database*))))
  (is (= (connection-url (:vendor *database*))
         (resolve-connection (connection-url (:vendor *database*))))))

(deftest test-resolve-connection-pool
  (let [pool (resolve-connection-pool (:vendor *database*))]
    (is (map? pool))
    (is (re-matches #".*korma.*" (.getJdbcUrl (:datasource pool))))
    (is (= (:datasource pool) (:datasource (resolve-connection-pool (:vendor *database*)))))))

(deftest test-with-connection
  (with-connection (:vendor *database*)
    (is (instance? Connection (jdbc/connection))))
  (is (thrown? IllegalArgumentException (with-connection :unknown))))

(deftest test-with-connection-pool
  (with-connection-pool (:vendor *database*)
    (is (instance? NewProxyConnection (jdbc/connection)))))

(deftest test-wrap-connection
  ((wrap-connection
    (fn [request]
      (is (instance? Connection (jdbc/connection))))
    (:vendor *database*))
   {}))

(deftest test-wrap-connection-pool
  ((wrap-connection-pool
    (fn [request]
      (is (instance? NewProxyConnection (jdbc/connection))))
    (:vendor *database*))
   {}))
