(set-env!
 :project 'ubikv2
 :version "0.0.2"
 :source-paths #{"src/clj" "src/cljs"}
 :resource-paths #{"public"}
 :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                 [adzerk/boot-reload "0.4.12" :scope "test"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [org.clojure/core.async "0.2.385"]
                 [com.taoensso/sente "1.10.0"]
                 [http-kit "2.1.19"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [garden "1.3.2"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [mount "0.1.10"]])

(require
 '[ubik.core]
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-reload :refer [reload]]
 '[mount.core :refer [start stop] :as mount])

(deftask dev []
  (comp
   (watch)
   (cljs :source-map true)
   (reload)
   (repl :server true)))

(deftask build []
  (comp
   (cljs :source-map true)
   (aot :namespace '#{ubik.core})
   (pom :project (get-env :project)
        :version (get-env :version))
   (uber)
   (jar :main 'ubik.core :file (format "%s-%s.jar" (get-env :project) (get-env :version)))))

(deftask print-version []
  (print (get-env :version)))
