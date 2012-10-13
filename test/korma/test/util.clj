(ns korma.test.util
  (:use clojure.test
        korma.util))

(deftest test-format-server
  (are [server expected]
       (is (= expected (format-server server)))
       {:server-name "example.com"}
       "example.com"
       {:server-name "example.com" :server-port 123}
       "example.com:123"))

(deftest test-parse-integer
  (is (nil? (parse-integer nil)))
  (is (nil? (parse-integer "")))
  (is (= 1 (parse-integer 1)))
  (is (= 1 (parse-integer "1"))))

(deftest test-parse-params
  (are [params expected]
       (is (= expected (parse-params params)))
       nil {}
       "" {}
       "a=1" {:a "1"}
       "a=1&b=2" {:a "1" :b "2"}))

(deftest test-parse-subprotocol
  (doseq [db-url [nil "" "x"]]
    (is (thrown? IllegalArgumentException (parse-subprotocol db-url))))
  (are [db-url subprotocol]
       (is (= subprotocol (parse-subprotocol db-url)))
       "bonecp:mysql://localhost/korma" "mysql"
       "c3p0:mysql://localhost/korma" "mysql"
       "jdbc:mysql://localhost/korma" "mysql"
       "mysql://localhost/korma" "mysql"))

(deftest test-parse-db-url
  (doseq [url [nil "" "x"]]
    (is (thrown? IllegalArgumentException (parse-db-url url))))
  (let [url (parse-db-url "postgresql://localhost:5432/korma")]
    (is (= :jdbc (:pool url)))
    (is (= "localhost" (:server-name url)))
    (is (= 5432 (:server-port url)))
    (is (= "korma" (:db url)))
    (is (= "/korma" (:uri url)))
    (is (= {} (:params url)))
    (let [spec (:spec url)]
      (is (= "//localhost:5432/korma" (:subname spec)))
      (is (= "postgresql" (:subprotocol spec)))))
  (let [url (parse-db-url "postgresql://tiger:scotch@localhost:5432/korma?a=1&b=2")]
    (is (= :jdbc (:pool url)))
    (is (= "tiger" (:username url)))
    (is (= "scotch" (:password url)))
    (is (= "localhost" (:server-name url)))
    (is (= 5432 (:server-port url)))
    (is (= "korma" (:db url)))
    (is (= "/korma" (:uri url)))
    (is (= {:a "1" :b "2"} (:params url)))
    (let [spec (:spec url)]
      (is (= "//localhost:5432/korma?a=1&b=2" (:subname spec)))
      (is (= "postgresql" (:subprotocol spec)))))
  (let [url (parse-db-url "c3p0:postgresql://localhost/korma")]
    (is (= :c3p0 (:pool url)))
    (is (= "localhost" (:server-name url)))
    (is (nil? (:server-port url)))
    (is (nil?  (:port url)))
    (is (= "korma" (:db url)))
    (is (= "/korma" (:uri url)))
    (is (= {} (:params url)))
    (let [spec (:spec url)]
      (is (= "//localhost/korma" (:subname spec)))
      (is (= "postgresql" (:subprotocol spec))))))
