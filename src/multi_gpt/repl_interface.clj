(ns multi-gpt.repl-interface
  (:require [multi-gpt.core-api :as core-api]
            [multi-gpt.conversation-manager :as cm]
            [multi-gpt.task-manager :as tm]))

(defn setup-system [api-key org-id model]
  (let [gpt-api (core-api/create-gpt-api api-key org-id model)
        on-update (fn [new-state]
                    (let [latest-conversation (first (vals new-state))
                          messages (:messages latest-conversation)
                          message (last messages)]
                      (println (if (= (:role message) "assistant") "GPT response:" "User message:") (:content message))))
        conversation-manager (cm/create-conversation-manager gpt-api :on-update on-update)
        task-manager (tm/create-task-manager conversation-manager)]
    {:gpt-api gpt-api
     :conversation-manager conversation-manager
     :task-manager task-manager}))

(defn create-task [system task-description]
  (tm/create-task (:task-manager system) task-description))

(defn generate-sub-tasks [system task]
  (tm/generate-sub-tasks (:task-manager system) task))

(defn process-sub-tasks [system sub-tasks]
  (tm/process-sub-tasks (:task-manager system) sub-tasks))

(defn generate-sub-task-result [system sub-task]
  (tm/generate-sub-task-result (:task-manager system) sub-task))

(defn create-conversation [system]
  (cm/create-conversation (:conversation-manager system)))

(defn get-conversation [system conversation-id]
  (cm/get-conversation (:conversation-manager system) conversation-id))

(defn update-conversation [system conversation-id message]
  (cm/update-conversation (:conversation-manager system) conversation-id message))

(defn end-conversation [system conversation-id]
  (cm/end-conversation (:conversation-manager system) conversation-id))
