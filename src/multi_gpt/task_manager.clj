(ns multi-gpt.task-manager
  (:require [multi-gpt.conversation-manager :as cm]
            [clojure.core.async :refer [<! go]]
            [clojure.string :as s]))

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
        (let [sub-tasks (<! (generate-sub-tasks this task-description))
              task-state {:task-id task-id
                          :conversation-id conversation-id
                          :task-description task-description
                          :sub-tasks sub-tasks
                          :completed []}]
          (swap! db assoc task-id task-state)
          task-state))))
  (generate-sub-tasks [this task]
    (let [conversation-id (cm/create-conversation conversation-manager)
          message {:role "system" :content (str "Break down the following task into sub-tasks. Please ensure the sub-task is re-stated in a way that implies the primary task, but won't lead to redundant results. Please reply with a Clojure vector of strings:" task)}]
      (go
        (let [gpt-response (<! (cm/update-conversation conversation-manager conversation-id message))]
          (->> gpt-response
               :content
               (read-string)
               (mapv (fn [sub-task-content] (println sub-task-content) {:conversation-id (cm/create-conversation conversation-manager)
                                                                        :content (format "Sub-Task: %s" sub-task-content)})))))))
  (process-sub-tasks [this sub-tasks]
    (mapv #(generate-sub-task-result this %) sub-tasks))
  (generate-sub-task-result [this sub-task]
    (let [{:keys [conversation-manager]} this
          conversation-id (:conversation-id sub-task)
          message {:role "system" :content (str "Complete sub-task: " (:content sub-task))}]
      (go
        (let [gpt-response (<! (cm/update-conversation conversation-manager conversation-id message))]
          (swap! db update-in [(:task-id sub-task) :completed] conj {:sub-task sub-task :result gpt-response})
          gpt-response)))))

(defn create-task-manager [conversation-manager]
  (let [tasks (atom {})]
    (->SimpleTaskManager conversation-manager tasks)))
