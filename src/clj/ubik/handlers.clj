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

(defn- get-swiper-container [swiper-id slide-contents]
  (letfn [(get-slide-container [content]
            [:div {:class "swiper-slide"} [:p {:class "huge-text rotated-text"} content]])]
    [:div {:class "swiper-container" :id swiper-id}
     (reduce conj [:div {:class "swiper-wrapper"}] (map get-slide-container slide-contents))]))

(defn controller-handler [req]
  (hiccup/html
   [:style controller-style]
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/css/swiper.min.css")
   [:button#btn1 {:type "button"} "change-anim"]
   (reduce conj [:div {:id "main-container"}]
           (map (fn [[swiper-id slide-contents]] (get-swiper-container swiper-id slide-contents))
                [["top-container" [":" "8" "X"]]
                 ["center-container" ["-" "o" "."]]
                 ["bottom-container" ["(" "|" ")"]]]))
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/js/swiper.min.js"}]
   [:script {:src "ubik/controller/main.js"}]))
