(ns multi-gpt.task-manager
  (:require [multi-gpt.conversation-manager :as cm]
            [clojure.core.async :refer [<! go]]
            [clojure.string :as s]
            [clojure.edn :as edn]))

(defprotocol TaskManager
  (create-task [this task-description] "Create a new task, break it into sub-tasks and return the initial task state")
  (generate-sub-tasks [this task] "Generate sub-tasks for the given task using GPT")
  (process-sub-tasks [this sub-tasks] "Process the given sub-tasks and return a list of results")
  (generate-sub-task-result [this sub-task] "Generate the result for the given sub-task using GPT"))

(defrecord SimpleTaskManager [conversation-manager db]
  TaskManager
  (create-task [this task-description]
    (let [conversation-id (cm/create-conversation conversation-manager)
          task-id (str (java.util.UUID/randomUUID))]
      (go
        (let [task {:task-id task-id
                    :conversation-id conversation-id
                    :task-description task-description
                    :completed []}
              sub-tasks (<! (generate-sub-tasks this task))]
          (swap! db assoc task-id (assoc task :sub-tasks sub-tasks))
          task))))
  (generate-sub-tasks [this task]
    (let [conversation-id (cm/create-conversation conversation-manager)
          message [{:role "system" :content "Break down the following task into sub-tasks. Please ensure the sub-task is re-stated in a way that implies the primary task, but won't lead to redundant results. Please reply with a Clojure vector of strings"}
                   {:role "user" :content "Organize a children's birthday party"}
                   {:role "assistant" :content (str ["Decide on a theme" "Send invitaions" "Select a location" "Come up with games"])}
                   {:role "user" :content (str "Break down the following task into sub-tasks: " (:task-description task))}]] 
      (go
        (let [gpt-response (<! (cm/update-conversation conversation-manager conversation-id message))]
          (if-not (:core-api/error gpt-response)
            (->> gpt-response
                 :content
                 (edn/read-string)
                 (mapv (fn [sub-task-content]
                         {:task-id (:task-id task)
                          :conversation-id (cm/create-conversation conversation-manager)
                          :content (format "Sub-Task: %s" sub-task-content)})))
            gpt-response)))))
  (process-sub-tasks [this sub-tasks]
    (mapv #(generate-sub-task-result this %) sub-tasks))
  (generate-sub-task-result [this sub-task]
    (let [{:keys [conversation-manager]} this
          conversation-id (:conversation-id sub-task)
          message [{:role "system" :content (str "Complete sub-task: " (:content sub-task))}]]
      (go
        (let [gpt-response (<! (cm/update-conversation conversation-manager conversation-id message))]
          (swap! db update-in [(:task-id sub-task) :completed] conj {:sub-task sub-task :result gpt-response})
          gpt-response)))))

(defn create-task-manager [conversation-manager]
  (let [tasks (atom {})]
    (->SimpleTaskManager conversation-manager tasks)))
