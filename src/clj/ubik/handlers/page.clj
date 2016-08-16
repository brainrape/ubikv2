(ns ubik.handlers.page
  (:require [garden.core :refer [css]]
            [hiccup
             [core :as hiccup]
             [page :refer [include-css]]]))

(def controller-style
  (css
   [:#main-container {:position "absolute" :top 0 :right 0 :left 0 :bottom 0 :visibility "hidden"}]
   [:#wait-container {:position "absolute" :top 0 :right 0 :left 0 :bottom 0 :visibility "visible"}]
   [:.swiper-container {:height "20%" :width "100%"}]
   [:.swiper-slide {:vertical-align "middle"}]
   [:.huge-text {:text-align "center" :font-size "500%" :font-family "sans-serif"}]))

(defn anim-handler [req]
  (hiccup/html
   [:body
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/three.js/r79/three.min.js"}]
    [:script {:src "ubik/anim/main.js"}]]))

(defn- get-swiper-container [swiper-id]
  [:div {:class "swiper-container" :id swiper-id}
   (reduce conj [:div {:class "swiper-wrapper"}]
           (map (fn [i] [:div {:class "swiper-slide"} [:p {:class "huge-text rotated-text"} i]])
                (range 4)))])

(defn controller-handler [req]
  (hiccup/html
   [:style controller-style]
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/css/swiper.min.css")
   (let [swiper-types ["top-container" "center-container" "bottom-container" "bg-container"]
         main-container (reduce conj [:div {:id "main-container"}] (map get-swiper-container swiper-types))]
     (conj main-container [:div {:id "desc-container"} [:p "swipe them"]]))
   [:div {:id "wait-container"}
    [:p {:id "countdown" :class "huge-text"}]]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/Swiper/3.3.1/js/swiper.min.js"}]
   [:script {:src "ubik/controller/main.js"}]))
