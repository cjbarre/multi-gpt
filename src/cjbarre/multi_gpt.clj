(ns cjbarre.multi-gpt
  (:require [cjbarre.core-api :as core-api]
            [clojure.core.async :refer [<!! <! go]]
            [clj-http.client :as http]
            [cjbarre.task-manager :as task-manager]))


(comment

  (require '[cjbarre.repl-interface :refer :all])

  (def system (setup-system ""
                            "org-Jm9K7Bqobj7HWLK6mPRsJVZR"
                            "gpt-3.5-turbo")) 

  (def conversation (create-conversation system))

  (update-conversation system conversation [{:role "system" :content "You will help me develop the next breakthrough in distributed systems."}])

  (def chat (fn [message] (update-conversation system conversation [{:role "user" :content message}])))

  (chat "Please provide the most cutting edge research topics about these systems.")

  (clojure.pprint/pprint (get-conversation system conversation))
  
  )