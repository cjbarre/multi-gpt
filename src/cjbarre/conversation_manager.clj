(ns cjbarre.conversation-manager
  (:require [cjbarre.core-api :as core-api]
            [clojure.core.async :refer [<! go]]
            [cjbarre.conversation-manager :as cm]))

(def conversations (atom {}))

;; Define the Conversation Manager protocol
(defprotocol ConversationManager
  (create-conversation [this] "Create a new conversation and return its ID")
  (get-conversation [this conversation-id] "Get the conversation by its ID")
  (update-conversation [this conversation-id message] "Update the conversation with a new message and return the GPT response")
  (end-conversation [this conversation-id] "Mark the conversation as inactive"))

;; Implementation of the Conversation Manager using an in-memory store
(defrecord InMemoryConversationManager [gpt-api]
  ConversationManager
  (create-conversation [this]
    (let [conversation-id (str (java.util.UUID/randomUUID))]
      (swap! conversations assoc conversation-id {:active true :messages []})
      conversation-id))

  (get-conversation [this conversation-id]
    (if-let [conversation (get @conversations conversation-id)]
      conversation
      (throw (ex-info "Conversation not found" {:conversation-id conversation-id}))))

  (update-conversation [this conversation-id message]
    (if-let [conversation (get @conversations conversation-id)]
      (if (:active conversation)
        (let [_ (swap! conversations update-in [conversation-id :messages] conj message)
              updated-messages (conj (:messages conversation) message)
              response-chan (core-api/send-request gpt-api updated-messages {})]
          (go
            (let [gpt-response (<! response-chan)]
              (swap! conversations update-in [conversation-id :messages] conj gpt-response)
              gpt-response)))
        (throw (ex-info "Conversation is not active" {:conversation-id conversation-id})))
      (throw (ex-info "Conversation not found" {:conversation-id conversation-id}))))

  (end-conversation [this conversation-id]
    (if-let [conversation (get @conversations conversation-id)]
      (swap! conversations update-in [conversation-id :active] (constantly false))
      (throw (ex-info "Conversation not found" {:conversation-id conversation-id})))))


(defn init [conversations on-update]
      (let [watch-key :on-update]
        (add-watch conversations watch-key
                   (fn [_key _atom _old-state new-state]
                     (when on-update
                       (on-update new-state))))
        watch-key))

;; Function to create a new InMemoryConversationManager instance
(defn create-conversation-manager [gpt-api & {:keys [on-update]}]
  (let [cm (->InMemoryConversationManager gpt-api)]
    (init conversations on-update)
    cm))
