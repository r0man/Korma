(ns korma.test
  (:use [environ.core :only [env]]
        clojure.test))

(def ^:dynamic *database*
  {:url (env :sqlite) :vendor :sqlite})

(def ^:dynamic *vendors*
  [:mysql :postgresql :sqlite])

(defmacro database-test [test-name & body]
  `(do ~@(for [vendor# *vendors*]
           `(do (deftest ~(symbol (str test-name "-" (name vendor#)))
                  (binding [*database* {:url (env ~vendor#) :vendor ~vendor#}]
                    (try
                      ~@body
                      (finally (.delete (java.io.File. "/tmp/korma"))))))
                (alter-meta! (var ~(symbol (str test-name "-" (name vendor#))))
                             assoc ~vendor# true)))))
