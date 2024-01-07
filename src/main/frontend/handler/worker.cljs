(ns frontend.handler.worker
  "Handle messages received from the db worker"
  (:require [cljs-bean.core :as bean]
            [frontend.handler.file :as file-handler]
            [frontend.handler.notification :as notification]
            [clojure.edn :as edn]
            [frontend.state :as state]))

(defmulti handle identity)

(defmethod handle :write-files [_ {:keys [repo files]}]
  (file-handler/alter-files repo files {}))

(defmethod handle :notification [_ data]
  (apply notification/show! (edn/read-string data)))

(defmethod handle :add-repo [_ data]
  (state/add-repo! {:url (:repo (edn/read-string data))}))

(defmethod handle :default [_ data]
  (prn :debug "Worker data not handled: " data))

(defn handle-message!
  [^js worker]
  (assert worker "worker doesn't exists")
  (set! (.-onmessage worker)
        (fn [event]
          (let [data (.-data event)]
            (when-not (= (.-type data) "RAW")
              (let [[e payload] (bean/->clj data)]
                (handle (keyword e) payload)))))))
