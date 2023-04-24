(ns multi-gpt.core-api
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<! >! go chan put!]]))

;; Define the protocol for the Chat API
(defprotocol GPTAPI
  (send-request [this messages opt] "Send a request to the GPT Chat API with the given messages and return a channel with the parsed response")
  (parse-response [this response] "Parse the response from the GPT Chat API"))

;; Implementation for the GPT Chat API with a configurable model
(defrecord GPTAPIImpl [api-key org-id model]
  GPTAPI
  (send-request [this messages {:keys [temperature top-p n stream stop max-tokens presence-penalty frequency-penalty] 
                                :or {temperature 1 top-p 1 n 1 stream false stop nil max-tokens nil presence-penalty 0 frequency-penalty 0}}]
    (println (str "sending request " (java.util.UUID/randomUUID)))
    (let [url "https://api.openai.com/v1/chat/completions"
          headers {"Authorization" (str "Bearer " api-key)
                   "OpenAI-Organization" org-id
                   "Content-Type" "application/json"}
          body {:model model
                :messages messages
                :temperature temperature
                :top_p top-p
                :n n
                :stream stream
                :stop stop
                :max_tokens max-tokens
                :presence_penalty presence-penalty
                :frequency_penalty frequency-penalty}
          response-chan (chan 1)]
      (http/post url
                  {:headers headers
                   :form-params body
                   :content-type :json
                   :socket-timeout 30000
                   :connection-timeout 30000
                   :async? true}
                  (fn [response]
                    (put! response-chan (parse-response this response))
                    (async/close! response-chan))
                  (fn [exception]
                    (put! response-chan {:core-api/error exception})
                    (async/close! response-chan)))
      response-chan))
  (parse-response [this response]
    (-> response :body (json/read-str :key-fn keyword) :choices first :message)))

;; Function to create a new GPT Chat API instance with a configurable model
(defn create-gpt-api [api-key org-id model]
  (->GPTAPIImpl api-key org-id model))
