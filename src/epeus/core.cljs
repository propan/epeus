(ns epeus.core
  (:require [epeus.state :as a]
            [epeus.components.web :refer [web-component]]
            [epeus.components.header-toolbar :refer [header-toolbar-component]]
            [epeus.components.tooltip :refer [tooltip-component]]
            [epeus.history :as history]
            [epeus.utils :refer [show-element]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(om/root
 web-component
 a/app-state
 {:target    (. js/document (getElementById "document-container"))
  :tx-listen history/handle-transaction})

(om/root
 header-toolbar-component
 a/app-state
 {:target (. js/document (getElementById "header-toolbar"))})

(om/root
 tooltip-component
 a/app-state
 {:target (. js/document (getElementById "tooltip-holder"))})

(when (re-find #"(?i)(iPad|iPhone|iPod)" js/navigator.userAgent)
  (show-element (. js/document (getElementById "browser-warning"))))
