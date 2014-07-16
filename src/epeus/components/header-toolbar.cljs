(ns epeus.components.header-toolbar
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [chan put!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [epeus.history :as history]))

;;
;; Actions
;;

(defn reset-app-state
  [state]
  (om/transact! state [:main :items]
                #(-> %
                     (assoc :title "")
                     (assoc :children {}))
                :create-restore-point))

(defn download-document
  "Generates compressed project file and triggers download event.

  Highly inspired by https://github.com/jackschaedler/goya"
  [state]
  (let [download-link (. js/document (getElementById "document-download-link"))
        content       (->> (get-in @state [:main :items])
                           (pr-str)
                           (.compressToBase64 js/LZString)
                           (str "data:application/octet-stream;base64,"))]
    (set! (.-href download-link) content)
    (.click download-link)))

;;
;; Helpers
;;

(defn toolbar-item-class
  [predicate]
  (str "toolbar-item"
       (when-not (predicate) " disabled")))

;;
;; Components
;;

(defn button-component
  [item owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [commands]}]
      (dom/div #js {:className (:class item)
                    :onClick   #(put! commands {:command (:command item)
                                                :owner   owner})}
               (dom/i #js {:className (:type item)})))))

(defn header-toolbar-component
  [state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:commands (chan)})

    om/IWillMount
    (will-mount [_]
      (let [commands (om/get-state owner :commands)]
        (go-loop []
                 (when-let [{:keys [command owner]} (<! commands)]
                   (case command
                     :undo     (history/undo)
                     :redo     (history/redo)
                     :reset    (reset-app-state state)
                     :download (download-document state)
                     nil)
                   (recur)))))

    om/IRenderState
    (render-state [_ {:keys [commands]}]
      (dom/div #js {:className "inner"}
               (om/build button-component
                         {:class   (toolbar-item-class history/can-undo?)
                          :type    "icon-undo"
                          :command :undo}
                         {:init-state {:commands commands}})
               (om/build button-component
                         {:class   (toolbar-item-class history/can-redo?)
                          :type    "icon-redo"
                          :command :redo}
                         {:init-state {:commands commands}})
               (om/build button-component
                         {:class   "toolbar-item"
                          :type    "icon-trash"
                          :command :reset}
                         {:init-state {:commands commands}})
               (om/build button-component
                         {:class   "toolbar-item"
                          :type    "icon-download"
                          :command :download}
                         {:init-state {:commands commands}})
               (om/build button-component
                         {:class   "toolbar-item"
                          :type    "icon-upload"
                          :command :upload}
                         {:init-state {:commands commands}})
               (om/build button-component
                         {:class   "toolbar-item"
                          :type    "icon-picture"
                          :command :export}
                         {:init-state {:commands commands}})))))
