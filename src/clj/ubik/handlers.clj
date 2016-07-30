(ns ubik.handlers
  (:require [ubik.styles :refer [controller-style]]
            [hiccup
             [core :as hiccup]
             [page :refer [include-css]]]))

(defn anim-handler [req]
  (hiccup/html
   [:body
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/three.js/r79/three.min.js"}]
    [:script {:src "ubik/anim/main.js"}]]))

(defn- get-swiper-container [swiper-id]
  [:div {:class "swiper-container" :id swiper-id}
   [:div {:class "swiper-wrapper"}
    [:div {:class "swiper-slide"} [:p {:class "huge-text rotated-text"} "x"]]
    [:div {:class "swiper-slide"} [:p {:class "huge-text rotated-text"} "y"]]]])

(defn controller-handler [req]
  (hiccup/html
   [:style controller-style]
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/css/swiper.min.css")
   (reduce conj [:div {:id "main-container"}]
           (map get-swiper-container ["top-container" "center-container" "bottom-container"]))
   [:div {:id "wait-container"}
    [:p {:id "countdown"}]]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/js/swiper.min.js"}]
   [:script {:src "ubik/controller/main.js"}]))
