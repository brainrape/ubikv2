(ns ubik.styles
  (:require [garden.core :refer [css]]))


(def controller-style
  (css
   [:#main-container {:position "absolute" :top 0 :right 0 :left 0 :bottom 0}]
   [:.swiper-container {:height "33%" :width "100%"}]
   [:.swiper-slide {:vertical-align "middle"}]
   [:.huge-text {:text-align "center" :font-size "500%" :font-family "sans-serif"}]))
