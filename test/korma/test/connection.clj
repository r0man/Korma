(ns korma.test.connection
  (:import [com.jolbox.bonecp BoneCPDataSource ConnectionHandle]
           com.mchange.v2.c3p0.ComboPooledDataSource
           com.mchange.v2.c3p0.impl.NewProxyConnection
           java.sql.Connection)
  (:require [clojure.java.jdbc :as jdbc])
  (:use korma.connection
        korma.util
        korma.test
        clojure.test))

(deftest test-connection-spec
  (let [spec (connection-spec "mysql://tiger:scotch@localhost/korma?profileSQL=true")]
    (is (= :jdbc (:pool spec)))
    (is (= "com.mysql.jdbc.Driver" (:classname spec)))
    (is (= :mysql (:subprotocol spec)))
    (is (= "//localhost/korma?profileSQL=true" (:subname spec)))
    (is (= "localhost" (:server-name spec)))
    (is (nil? (:server-port spec)))
    (is (= "tiger" (:user spec)))
    (is (= "scotch" (:password spec)))
    (is (= "korma" (:db spec)))
    (is (= "/korma" (:uri spec)))
    (is (= {:profileSQL "true"} (:params spec))))
  (let [spec (connection-spec "bonecp:postgresql://tiger:scotch@localhost:5432/korma?ssl=true")]
    (is (= :bonecp (:pool spec)))
    (is (= "org.postgresql.Driver" (:classname spec)))
    (is (= :postgresql (:subprotocol spec)))
    (is (= "//localhost:5432/korma?ssl=true" (:subname spec)))
    (is (= "localhost" (:server-name spec)))
    (is (= 5432 (:server-port spec)))
    (is (= "tiger" (:user spec)))
    (is (= "scotch" (:password spec)))
    (is (= "korma" (:db spec)))
    (is (= "/korma" (:uri spec)))
    (is (= {:ssl "true"} (:params spec))))
  (let [spec (connection-spec "c3p0:sqlite://tmp/korma.sqlite")]
    (is (= :c3p0 (:pool spec)))
    (is (= "org.sqlite.JDBC" (:classname spec)))
    (is (= "sqlite" (:subprotocol spec)))
    (is (= "//tmp/korma.sqlite" (:subname spec)))
    (is (= {} (:params spec))))
  (let [spec (connection-spec "sqlite:korma.sqlite")]
    (is (= :jdbc (:pool spec)))
    (is (= "org.sqlite.JDBC" (:classname spec)))
    (is (= "sqlite" (:subprotocol spec)))
    (is (= "korma.sqlite" (:subname spec)))
    (is (= {} (:params spec)))))

(database-test test-connection-url
  (is (thrown? IllegalArgumentException (connection-url :unknown-db)))
  (is (re-matches (re-pattern (str ".*:" (name (:vendor *database*)) ":.*")) (connection-url (:vendor *database*)))))

(database-test test-connection
  (let [connection (connection (:vendor *database*))]
    (is (instance? ComboPooledDataSource (:datasource connection)))
    (is (not (= connection (connection (:vendor *database*)))))))

(database-test test-cached-connection
  (let [connection (cached-connection (:vendor *database*))]
    (is (instance? ComboPooledDataSource (:datasource connection)))
    (is (= connection (cached-connection (:vendor *database*))))))

(database-test test-with-connection
  (with-connection (:vendor *database*)
    (is (instance? Connection (jdbc/connection))))
  (is (thrown? IllegalArgumentException (with-connection :unknown))))

(deftest test-with-connection-bonecp
  (with-connection "bonecp:mysql://korma:korma@localhost/korma"
    (is (instance? ConnectionHandle (jdbc/connection))))
  (with-connection "bonecp:postgresql://korma:korma@localhost/korma"
    (is (instance? ConnectionHandle (jdbc/connection))))
  (with-connection "bonecp:sqlite://tmp/korma.sqlite"
    (is (instance? ConnectionHandle (jdbc/connection)))))

(deftest test-with-connection-c3p0
  (with-connection "c3p0:mysql://korma:korma@localhost/korma"
    (is (instance? NewProxyConnection (jdbc/connection))))
  (with-connection "c3p0:postgresql://korma:korma@localhost/korma"
    (is (instance? NewProxyConnection (jdbc/connection))))
  (with-connection "c3p0:sqlite://tmp/korma.sqlite"
    (is (instance? NewProxyConnection (jdbc/connection)))))

(database-test test-wrap-connection
  ((wrap-connection
    (fn [request]
      (is (instance? Connection (jdbc/connection))))
    (:vendor *database*)) {}))
