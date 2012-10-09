(ns korma.test.util
  (:use clojure.test
        korma.util))

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

(deftest test-parse-db-url
  (doseq [url [nil "" "x"]] (is (nil? (parse-db-url nil))))
  (let [spec (parse-db-url "postgresql://localhost:5432/korma")]
    (is (= "postgresql" (:scheme spec)))
    (is (= "postgresql" (:subprotocol spec)))
    (is (= "localhost" (:server-name spec)))
    (is (= "localhost" (:host spec)))
    (is (= 5432 (:server-port spec)))
    (is (= 5432 (:port spec)))
    (is (= "korma" (:db spec)))
    (is (= "/korma" (:uri spec)))
    (is (= "//localhost:5432/korma" (:subname spec)))
    (is (= {} (:params spec))))
  (let [spec (parse-db-url "postgresql://tiger:scotch@localhost:5432/korma?a=1&b=2")]
    (is (= "postgresql" (:scheme spec)))
    (is (= "postgresql" (:subprotocol spec)))
    (is (= "tiger" (:user spec)))
    (is (= "scotch" (:password spec)))
    (is (= "localhost" (:server-name spec)))
    (is (= "localhost" (:host spec)))
    (is (= 5432 (:server-port spec)))
    (is (= 5432 (:port spec)))
    (is (= "korma" (:db spec)))
    (is (= "/korma" (:uri spec)))
    (is (= "//localhost:5432/korma" (:subname spec)))
    (is (= {:a "1" :b "2"} (:params spec))))
  (let [spec (parse-db-url "jdbc:postgresql://localhost/korma")]
    (is (= "postgresql" (:scheme spec)))
    (is (= "postgresql" (:subprotocol spec)))
    (is (= "localhost" (:server-name spec)))
    (is (= "localhost" (:host spec)))
    (is (nil? (:server-port spec)))
    (is (nil?  (:port spec)))
    (is (= "korma" (:db spec)))
    (is (= "/korma" (:uri spec)))
    (is (= "//localhost/korma" (:subname spec)))
    (is (= {} (:params spec)))))
