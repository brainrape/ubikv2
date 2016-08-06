(ns ubik.handlers.core
  (:require [ubik.handlers.page :refer [controller-handler anim-handler]]
            [ubik.sente :refer [ring-ajax-get-or-ws-handshake ring-ajax-post]]
            [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [resource :refer [wrap-resource]]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]))

(defroutes ubik-routes
  (GET "/" req (controller-handler req))
  (GET "/anim" req (anim-handler req))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "404"))

(def ubik-ring-handler
  (-> ubik-routes
      (wrap-defaults site-defaults)
      (wrap-resource "/")))
