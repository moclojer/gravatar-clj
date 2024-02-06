(ns com.moclojer.gravatar-clj.core
  (:require
   #?(:clj [clj-commons.digest :as digest])
   [clojure.string :as str]))

(defonce gravatar-base-url "https://gravatar.com/avatar")
(defonce auth0-cdn-base-url "https://cdn.auth0.com/avatars")

(defn prepare [email]
  (->> email
       str/trim
       str/lower-case))

#?(:cljs
   (defn encode
     ([email]
      (encode email "utf-8"))
     ([email encoding]
      (.encode (js/TextEncoder. encoding) email))))

#?(:cljs
   (defn digest [encoded-email]
     (-> (.-subtle js/crypto)
         (.digest "SHA-256" encoded-email))))

#?(:cljs
   (defn decode [hash-buf]
     (-> (.from js/Array (js/Uint8Array. hash-buf))
         (.map #(-> (.toString % 16)
                    (.padStart 2 "0")))
         (.join ""))))

(defn gen-default-pfp-url [username]
  (let [uq-names (str/split username #" ")
        initials (if (>= (count uq-names) 2)
                   (->> uq-names
                        (take 2)
                        (map #(take 1 %))
                        flatten
                        (str/join "")
                        str/lower-case)
                   (nth username 0))]
    (str auth0-cdn-base-url "/" initials ".png")))

(defn gen-pfp-url [default-pfp hex]
  (str gravatar-base-url "/" hex "?default=" default-pfp))

(defn get-pfp-url-wrapped [email default-pfp]
  (let [prp-email (prepare email)]
    #?(:cljs
       (-> prp-email
           (.then encode)
           (.then digest)
           (.then decode)
           (.then (partial gen-pfp-url default-pfp)))
       :clj
       (gen-pfp-url default-pfp (digest/sha-256 prp-email)))))

(defn get-pfp-url
  ([email]
   (get-pfp-url email (gen-default-pfp-url email) nil))
  ([email default-pfp]
   (get-pfp-url email default-pfp nil))
  ([email default-pfp username]
   (get-pfp-url-wrapped email (if default-pfp
                                default-pfp
                                (gen-default-pfp-url username)))))
