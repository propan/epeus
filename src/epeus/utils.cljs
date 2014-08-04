(ns epeus.utils
  (:require [goog.color :as color]
            [goog.style :as style]
            [goog.dom :as dom]
            [goog.dom.forms :as forms])
  (:import [goog.ui IdGenerator]))

(defn next-uid
  []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn hidden
  [^boolean is-hidden]
  (if is-hidden
    #js {:display "none"}
    #js {}))

(defn get-by-id
  "Finds DOM element by id"
  [id]
  (. js/document (getElementById id)))

(defn scroll-to
  "Scrolls the given containter to the element inside that container."
  [container element center]
  (style/scrollIntoContainerView element container center))

(defn set-value
  [el val]
  (forms/setValue el val))

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
  [color factor]
  (-> color
      (color/hexToRgb)
      (color/darken factor)
      (color/rgbArrayToHex)))

(defn lighten
  [color factor]
  (-> color
      (color/hexToRgb)
      (color/lighten factor)
      (color/rgbArrayToHex)))

(defn show-element
  [el]
  (style/setElementShown el true))

(defn now
  []
  (.now js/Date))
