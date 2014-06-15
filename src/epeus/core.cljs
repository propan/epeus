(ns epeus.core
  (:require [epeus.state :as a]
            [epeus.components.web :as web]
            [epeus.components.header-toolbar :as header-toolbar]
            [epeus.history :as history]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(om/root
 web/web-component
 a/app-state
 {:target    (. js/document (getElementById "zoom-container"))
  :tx-listen history/handle-transaction
  :shared    {:dim (atom {})}})

(om/root
 header-toolbar/header-toolbar-component
 a/app-state
 {:target (. js/document (getElementById "header-toolbar"))})
