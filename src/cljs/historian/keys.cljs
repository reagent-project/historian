(ns historian.keys
  (:require [historian.core :as hist]
            [goog.events :as events])
  (:import [goog.events EventType]))


(defn bind-ctrl-z
  "Bind 'ctrl-z' to the undo function." []
  (events/listen js/window EventType.KEYDOWN 
                 #(when (and (= (.-keyCode %) 90) ;; 90 is Z
                             (.-ctrlKey %))
                    (hist/undo!))))
(defn bind-ctrl-y
  "Bind 'ctrl-y' to the redo function." []
  (events/listen js/window EventType.KEYDOWN 
                 #(when (and (= (.-keyCode %) 89) ;; 89 is Y
                             (.-ctrlKey %))
                    (hist/redo!))))

(defn bind-keys
  "Bind 'ctrl-z' and 'ctrl-y' to undo/redo."[]
  (bind-ctrl-z)
  (bind-ctrl-y))
