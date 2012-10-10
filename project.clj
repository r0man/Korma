(defproject org.clojars.r0man/korma "0.3.0-beta11"
  :description "Tasty SQL for Clojure"
  :url "http://github.com/ibdknox/korma"
  :dependencies [[environ "0.3.0"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [inflections "0.7.3"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]]
  :profiles {:dev {:env {:mysql "mysql://korma:korma@localhost/korma"
                         :postgresql "postgresql://korma:korma@localhost/korma"
                         :sqlite "sqlite://tmp/korma.sqlite"}
                   :dependencies [[mysql/mysql-connector-java "5.1.21"]
                                  [postgresql "9.1-901.jdbc4"]
                                  [org.xerial/sqlite-jdbc "3.7.2"]]}}
  :codox {:exclude [korma.sql.engine korma.sql.fns korma.sql.utils]}
  :plugins [[environ/environ.lein "0.3.0"]]
  :hooks [environ.leiningen.hooks]
  :test-selectors {:mysql :mysql
                   :postgresql :postgresql
                   :sqlite :sqlite})
