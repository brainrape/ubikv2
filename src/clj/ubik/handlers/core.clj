(ns ubik.handlers.core
  (:require [ubik.handlers.page :refer [controller-handler face-handler bg-handler]]
            [ubik.sente :refer [ring-ajax-get-or-ws-handshake ring-ajax-post]]
            [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [resource :refer [wrap-resource]]]
            [ring.middleware.partial-content :refer [wrap-partial-content]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]))

(defroutes ubik-routes
  (GET "/" req (controller-handler req))
  (GET "/face" req (face-handler req))
  (GET "/bg" req (bg-handler req))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "404"))

(def ubik-ring-handler
  (-> ubik-routes
      (wrap-defaults site-defaults)
      (wrap-partial-content)
      (wrap-resource "/")))
