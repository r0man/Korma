(ns korma.test
  (:use [environ.core :only [env]]
        clojure.test
        korma.connection))

(def ^:dynamic *database*
  {:url (env :sqlite) :vendor :sqlite})

(def ^:dynamic *vendors*
  [:mysql :postgresql :sqlite])

(defmacro database-test [test-name & body]
  `(do ~@(for [vendor# *vendors*
               :let [test-sym# (symbol (str test-name "-" (name vendor#)))]]
           `(do (deftest ~test-sym#
                  (binding [*database* {:url (env ~vendor#) :vendor ~vendor#}]
                    ~@body))
                (alter-meta! (var ~test-sym#) assoc ~vendor# true)))))
