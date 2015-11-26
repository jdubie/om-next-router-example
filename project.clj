(defproject om-next-router-example "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.omcljs/om "1.0.0-alpha24"]
                 [figwheel-sidecar "0.5.0-1" :scope "provided"]
                 [bidi "1.21.1"]
                 [kibu/pushy "0.3.6"]
                 [cljs-log "0.2.2"]
                 [ring/ring "1.4.0"]
                 [com.cognitect/transit-clj "0.8.281"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [cljs-http "0.1.30" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]])
