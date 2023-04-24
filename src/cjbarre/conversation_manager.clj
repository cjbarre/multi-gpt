(ns cjbarre.conversation-manager
  (:require [cjbarre.core-api :as core-api]
            [clojure.core.async :refer [<! go]]
            [cjbarre.conversation-manager :as cm]))

;; Define the Conversation Manager protocol
(defprotocol ConversationManager
  (create-conversation [this] "Create a new conversation and return its ID")
  (get-conversation [this conversation-id] "Get the conversation by its ID")
  (update-conversation [this conversation-id message] "Update the conversation with a new message and return the GPT response")
  (end-conversation [this conversation-id] "Mark the conversation as inactive"))

;; Implementation of the Conversation Manager using an in-memory store
(defrecord InMemoryConversationManager [gpt-api db]
  ConversationManager
  (create-conversation [this]
    (let [conversation {:id (str (java.util.UUID/randomUUID))
                        :active true
                        :messages []}]
      (swap! db assoc (:id conversation) conversation)
      conversation))

  (get-conversation [this {:keys [id] :as conversation}]
    (if-let [conversation (get @db id)]
      conversation
      (throw (ex-info "Conversation not found" {:conversation-id id}))))

  (update-conversation [this {:keys [id] :as conversation} messages]
    (if-let [conversation (get @db id)]
      (if (:active conversation)
        (let [_ (swap! db update-in [id :messages] concat messages)
              updated-messages (concat (:messages conversation) messages)
              response-chan (core-api/send-request gpt-api updated-messages {})]
          (go
            (let [gpt-response (<! response-chan)]
              (if-not (:core-api/error gpt-response)
                (swap! db update-in [id :messages] concat [gpt-response])
                (swap! db update-in [id :error] assoc gpt-response))
              gpt-response)))
        (throw (ex-info "Conversation is not active" {:conversation-id id})))
      (throw (ex-info "Conversation not found" {:conversation-id id}))))

  (end-conversation [this {:keys [id] :as conversation}]
    (if-let [conversation (get @db id)]
      (swap! db update-in [id :active] (constantly false))
      (throw (ex-info "Conversation not found" {:conversation-id id})))))

;; Function to create a new InMemoryConversationManager instance
(defn create-conversation-manager [gpt-api & {:keys [on-update]}]
  (let [conversations (atom {})
        cm (->InMemoryConversationManager gpt-api conversations)]
    cm))
