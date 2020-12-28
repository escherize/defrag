(defproject escherize/defrag "0.1.3"
  :description "A facility for writing defn macros that handle arguments"
  :url "http://github.com/escherize/defrag"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/spec.alpha "0.2.187"]]
  :repl-options {:init-ns defrag.core})
