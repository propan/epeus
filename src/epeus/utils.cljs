(ns epeus.utils
  (:require [goog.color :as color]
            [goog.style :as style]
            [goog.dom :as dom])
  (:import [goog.ui IdGenerator]))

(defn next-uid
  []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn hidden
  [^boolean is-hidden]
  (if is-hidden
    #js {:display "none"}
    #js {}))

;;TODO: remove
(defn element-offset
  [el]
  (let [offset (style/getPageOffset el)]
    [(.-x offset) (.-y offset)]))

(defn element-bounds
  [el]
  (let [rect (style/getBounds el)]
    [(.-width rect) (.-height rect)]))

(defn darken
  [color]
  (-> color
      (color/hexToRgb)
      (color/darken 0.15)
      (color/rgbArrayToHex)))

(defn lighten
  [color]
  (-> color
      (color/hexToRgb)
      (color/lighten 0.15)
      (color/rgbArrayToHex)))
