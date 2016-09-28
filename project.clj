(defproject historian "1.1.1"
  :description "Automatically save atoms and restore their previous states if needed."
  :url "https://github.com/Frozenlock/historian"
  :scm {:name "git"
         :url "https://github.com/Frozenlock/historian"}

  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]]

  :source-paths ["src/cljc" "src/cljs"])
