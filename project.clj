(defproject epeus "0.1.0-SNAPSHOT"
  :description "Knowledge Web Weaver"
  :url "https://github.com/propan/epeus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [om "0.6.4"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :source-paths ["src"]
  :cljsbuild {:builds {:dev
                       {:source-paths ["src"]
                        :compiler {:output-to "out/main.js"
                                   :output-dir "out/"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true
                                   :preamble ["resources/js/vendor/react-0.10.0/react-with-addons.min.js"
                                              "resources/js/vendor/lz-string/lz-string-1.3.3-min.js"]
                                   :externs ["resources/js/vendor/react-0.10.0/react-with-addons.js"
                                             "resources/js/vendor/lz-string/lz-string-1.3.3-min.js"]
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}

                       :prod
                       {:source-paths ["src"]
                        :compiler {:output-to "resources/main.min.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["js/vendor/react-0.10.0/react-with-addons.min.js"
                                              "js/vendor/lz-string/lz-string-1.3.3-min.js"]
                                   :externs ["js/vendor/react-0.10.0/react-with-addons.js"
                                             "js/vendor/lz-string/lz-string-1.3.3-min.js"]
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}}}
)
