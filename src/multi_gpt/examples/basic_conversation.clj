(ns multi-gpt.examples.basic-conversation
  (:require [multi-gpt.repl-interface :refer :all]
            [clojure.pprint :refer [pprint]]))

(comment
  ;;
  ;; Setup the system (includes a conversation manager)
  ;;
  (def system (setup-system "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"
                            "org-Jm9K7Bqobj7HWLK6mPRsJVZR"
                            "gpt-3.5-turbo"))
  ;;
  ;; Create a conversation
  ;;
  (def conversation (create-conversation system))

  ;;
  ;; Create a convenience function for chatting as the user role ;;
  ;;
  (def chat (fn [message] (update-conversation system conversation [{:role "user" :content message}])))

  ;;
  ;; Add a watch to print to the REPL (I also use portal and tap>)
  ;;
  (add-watch (-> system :conversation-manager :db)
             :on-update
             (fn [_ _ _ new-state]
               (pprint
                (get new-state (:id conversation)))))

  ;;
  ;; Initialize your conversation with a system prompt
  ;; This is also a reasonable place to add example messages if needed
  ;;
  (update-conversation system conversation [{:role "system" :content "You will help me develop the next breakthrough in distributed systems."}])

  ;;
  ;; Engage in conversation
  ;;
  (chat "Please provide the most cutting edge research topics about these systems.")

  )