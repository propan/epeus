(defproject epeus "0.1.0-SNAPSHOT"
  :description "Knowledge Web Weaver"
  :url "https://github.com/propan/epeus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [net.drib/strokes "0.5.1"]
                 [om "0.6.2"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :source-paths ["src"]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "out/main.js"
                                   :output-dir "out/"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true
                                   :preamble ["resources/js/vendor/react-0.10.0/react_with_addons.min.js"]
                                   :externs ["resources/js/vendor/react-0.10.0/react_with_addons.js"]
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}]}
)
