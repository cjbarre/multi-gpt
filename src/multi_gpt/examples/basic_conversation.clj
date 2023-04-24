(ns multi-gpt.examples.basic-conversation
  (:require [multi-gpt.repl-interface :refer :all]
            [clojure.pprint :refer [pprint]]))

(comment
  ;;
  ;; Run below if you want to use portal to see the output
  ;;
  (require '[portal.api :as p])
  (def p (p/open {:launcher :vs-code}))
  (add-tap #'p/submit)

  ;;
  ;; Setup the system (includes a conversation manager)
  ;;
  (def system (setup-system ""
                            ""
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
               ;; If you want to view output with portal uncomment this
               ;;
               #_(tap>  (with-meta (update (get new-state (:id conversation))
                                           :messages
                                           reverse)
                          {:portal.viewer/default :portal.viewer/tree}))
               ;; Comment out below if using portal
               ;;
               (pprint
                (get new-state (:id conversation)))))

  ;;
  ;; Initialize your conversation with a system prompt
  ;; This is also a reasonable place to add example messages if needed
  ;;
  (update-conversation
   system
   conversation
   [{:role "system"
     :content "You will help me develop the next breakthrough in distributed systems."}])

  ;;
  ;; Engage in conversation
  ;;
  (chat "Please provide the most cutting edge research topics about these systems.")

  )