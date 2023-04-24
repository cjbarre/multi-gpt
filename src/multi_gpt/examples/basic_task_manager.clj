(ns multi-gpt.examples.basic-task-manager 
  (:require [multi-gpt.task-manager :as task-manager]))

(comment

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Task Manager still very much a WIP ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;; Load the namespaces and required libraries
  (require '[multi-gpt.conversation-manager :as cm]
           '[multi-gpt.repl-interface :refer :all]
           '[multi-gpt.task-manager :as tm]
           '[clojure.core.async :refer [<!! go]]
           '[clojure.pprint :refer [pprint]])

  (def system (setup-system "api-key"
                            "org-id"
                            "gpt-3.5-turbo"))

  ;; Create a new task using the task manager
  (def task (<!! (create-task system "Organize a birthday party for a friend")))

  (add-watch (-> system :task-manager :db)
             :on-update
             (fn [_ _ _ new-state]
               (pprint
                (get new-state (:task-id task)))))


   ;; Process the sub-tasks
  (def sub-task-results (process-sub-tasks system (:task-id task)))

)


