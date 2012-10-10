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
  (is (map? (connection-spec (:url *database*))))
  (is (map? (connection-spec (parse-db-url (:url *database*))))))

(database-test test-c3p0-pool
  (let [pool (c3p0-pool (:vendor *database*))]
    (is (map? pool))
    (let [datasource (:datasource pool)]
      (is (instance? ComboPooledDataSource datasource))
      (is (re-matches (re-pattern (str "jdbc:" (name (:vendor *database*)) "://.*korma.*")) (.getJdbcUrl datasource)))
      (is (= 3 (.getInitialPoolSize datasource)))
      (is (= 15 (.getMaxPoolSize datasource)))
      (is (= 3 (.getMinPoolSize datasource)))
      (is (= 10800 (.getMaxIdleTime datasource)))
      (is (= 1800 (.getMaxIdleTimeExcessConnections datasource))))))

(database-test test-with-c3p0-pool
  (with-c3p0-pool (:vendor *database*)
    (is (instance? NewProxyConnection (jdbc/connection)))))

(database-test test-with-connection
  (with-connection (:vendor *database*)
    (is (instance? Connection (jdbc/connection))))
  (is (thrown? IllegalArgumentException (with-connection :unknown))))

(database-test test-wrap-c3p0-pool
  ((wrap-c3p0-pool
    (fn [request]
      (is (instance? NewProxyConnection (jdbc/connection))))
    (:vendor *database*))
   {}))

(database-test test-wrap-connection
  ((wrap-connection
    (fn [request]
      (is (instance? Connection (jdbc/connection))))
    (:vendor *database*))
   {}))
