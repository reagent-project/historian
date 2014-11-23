(defproject historian "1.0.6"
  :description "Automatically save atoms and restore their previous states if needed."
  :url "https://github.com/Frozenlock/historian"
  :scm {:name "git"
         :url "https://github.com/Frozenlock/historian"}

  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :dependencies [[org.clojure/clojurescript "0.0-2371" :scope "provided"]]


  :profiles {:dev {:plugins [[org.clojars.frozenlock/cljx "0.4.6"]]
                   :hooks [cljx.hooks]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :cljs}
                                   
                                   {:source-paths ["test/cljx"]
                                    :output-path "target/test"
                                    :rules :clj}
                                   
                                   ;; {:source-paths ["test/cljx"]
                                   ;;  :output-path "target/test"
                                   ;;  :rules :cljs}
                                   ]}
                   }}

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["target/classes" "src/cljs"]

  :test-paths ["target/test"])
