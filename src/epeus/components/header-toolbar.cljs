(ns epeus.components.header-toolbar
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [chan put!]]
            [cljs.reader :as reader]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [epeus.draw :refer [draw-image]]
            [epeus.events :refer [file-uploads]]
            [epeus.history :as history]
            [epeus.tree-utils :refer [apply-tree]]
            [epeus.utils :refer [next-uid get-by-id set-value]]))
;;
;; Downloading/uploading documents was more than highly inspired by https://github.com/jackschaedler/goya
;;


;;
;; Actions
;;

(defn set-app-state!
  [state items]
  (doto state
    (om/update! :graph {})
    (om/update! [:main :items] items :create-restore-point)))

(defn reset-app-state!
  [state]
  (set-app-state! state
                  {:uid      (next-uid)
                   :root?    true
                   :title    ""
                   :x        400
                   :y        200
                   :color    "#c0c0c0"
                   :children {}}))

(defn download-document
  [state]
  (let [download-link (get-by-id "document-download-link")
        content       (->> (get-in @state [:main :items])
                           (pr-str)
                           (.compressToBase64 js/LZString)
                           (str "data:application/octet-stream;base64,"))]
    (set! (.-href download-link) content)
    (.click download-link)))

(defn export-document-as-png
  [state]
  (let [download-link (get-by-id "image-download-link")
        content       (-> (get-in @state [:main :items])
                          (draw-image))]
    (set! (.-href download-link) content)
    (.click download-link)))

(defn choose-document
  []
  (let [document-chooser (get-by-id "document-chooser")]
    (.click document-chooser)))

(defn zoom-mouse-enter
  [e owner]
  (om/set-state! owner :hidden false))

(defn zoom-mouse-leave
  [e owner]
  (om/set-state! owner :hidden true))

(defn zoom-in
  [state]
  (om/transact! state [:zoom] #(if (< % 250) (+ % 25) %)))

(defn zoom-out
  [state]
  (om/transact! state [:zoom] #(if (> % 25) (- % 25) %)))

(defn zoom-reset
  [state]
  (om/update! state [:zoom] 100))

;;
;; Helpers
;;
(defn update-uid
  [node]
  (assoc node :uid (next-uid)))

(defn toolbar-item-class
  [predicate]
  (str "toolbar-item"
       (when-not (predicate) " disabled")))

(defn load-state-from-string [state data]
  (let [items (->> (aget (.split data ",") 1)
                   (.decompressFromBase64 js/LZString)
                   (reader/read-string)
                   (apply-tree update-uid))]
    (set-app-state! state items)
    (history/forget!)))

(defn file-list-to-cljs [js-col]
  (-> (clj->js [])
      (.-slice)
      (.call js-col)
      (js->clj)))

(defn handle-file-upload
  [state event]
  (let [file   (-> (.-target event)
                   (.-files)
                   (file-list-to-cljs)
                   (first))
        reader (js/FileReader.)]
    (set! (.-onload reader)
          #(do
             (set-value (get-by-id "document-chooser") "")
             (load-state-from-string state (.-result (.-target %)))))
    (.readAsDataURL reader file)))

;;
;; Components
;;
(defn zoom-button-component
  [state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:hidden   true})

    om/IRenderState
    (render-state [_ {:keys [hidden]}]
      (dom/div #js {:className "toolbar-item"
                    :onMouseOver #(zoom-mouse-enter % owner)
                    :onMouseOut  #(zoom-mouse-leave % owner)}
               (dom/i #js {:className   (if (< (:zoom state) 100)
                                          "icon-zoom-out"
                                          "icon-zoom-in")})
               (dom/div #js {:className "zoom-toolbar"
                             :style #js {:display (when hidden "none")}}
                        (dom/a #js {:className "button"
                                    :onClick   #(zoom-out state)}
                               "-")
                        (dom/span #js {:className "label"}
                                  (str (:zoom state) "%"))
                        (dom/a #js {:className "button"
                                    :onClick   #(zoom-in state)}
                               "+")
                        (dom/a #js {:className "button reset"
                                    :onClick   #(zoom-reset state)}
                               "reset"))))))

(defn button-component
  [item owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [commands]}]
      (dom/div #js {:className (:class item)
                    :onClick    #(if (= (:command item) :upload)
                                   (choose-document)
                                   (put! commands {:command (:command item)
                                                   :data    owner}))}
               (dom/i #js {:className (:type item)})))))

(defn header-toolbar-component
  [state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:commands (chan)
       :uploads  (file-uploads (get-by-id "document-chooser"))})

    om/IWillMount
    (will-mount [_]
      (let [commands (async/merge [(om/get-state owner :commands) (om/get-state owner :uploads)])]
        (go-loop []
                 (when-let [{:keys [command data]} (<! commands)]
                   (case command
                     :undo        (history/undo)
                     :redo        (history/redo)
                     :reset       (reset-app-state! state)
                     :download    (download-document state)
                     :upload      (choose-document)
                     :file-upload (handle-file-upload state data)
                     :export      (export-document-as-png state)
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
               (om/build zoom-button-component state)
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
