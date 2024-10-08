(ns frontend.worker.rtc.crypt
  "Fns to en/decrypt some block attrs"
  (:require [promesa.core :as p]))

(defonce ^:private encoder (new js/TextEncoder "utf-8"))
(defonce ^:private decoder (new js/TextDecoder "utf-8"))

(defn <encrypt
  [message public-key]
  (let [data (.encode encoder message)]
    (js/crypto.subtle.encrypt
     #js{:name "RSA-OAEP"}
     public-key
     data)))

(defn <decrypt
  [cipher-text private-key]
  (p/let [result (js/crypto.subtle.decrypt
                  #js{:name "RSA-OAEP"}
                  private-key
                  cipher-text)]
    (.decode decoder result)))

(defonce ^:private key-algorithm
  #js{:name "RSA-OAEP"
      :modulusLength 4096
      :publicExponent (new js/Uint8Array #js[1 0 1])
      :hash "SHA-256"})

(defn <gen-key-pair
  []
  (p/let [result (js/crypto.subtle.generateKey
                  key-algorithm
                  true
                  #js["encrypt", "decrypt"])]
    (js->clj result :keywordize-keys true)))

(defn <export-key
  [key']
  (js/crypto.subtle.exportKey "jwk" key'))

(defn <import-public-key
  [jwk]
  (js/crypto.subtle.importKey "jwk" jwk key-algorithm true #js["encrypt"]))

(defn <import-private-key
  [jwk]
  (js/crypto.subtle.importKey "jwk" jwk key-algorithm true #js["decrypt"]))


(comment
  (p/let [{:keys [publicKey privateKey]} (<gen-key-pair)]
    (p/doseq [msg (map #(str "message" %) (range 1000))]
      (p/let [encrypted (<encrypt msg publicKey)
              ;; plaintxt (<decrypt encrypted privateKey)
              ]
        (prn :encrypted msg)
        ;; (prn :plaintxt plaintxt)
        ))))
