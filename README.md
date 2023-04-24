# multi-gpt

Hello there, this project is about building a useful interface into the GPT language model from Clojure.

It's a work in progress, but it has a working in-memory conversation manager (conversational memory) and a rudimentary WIP task manager (agents).

It's early days, but I intend to use this in my work and also to learn the fundamentals of interacting with language models.

## Contact
If you're interested, want to help, or need help you can find me in the [#multi-gpt](https://clojurians.slack.com/archives/C054R2069AQ) channel on the Clojurians slack.

## Current Features
- In memory Conversation Manager
  - Keeps track of multiple conversations and manages sending the full message context on each chat request.
- In memory Task Manager
  - Beginnings of agents.
## Road Map
 - External memory architecture presented in the paper: [Generative Agents: Interactive Simulacra of Human Behavior](https://arxiv.org/abs/2304.03442) 
 - External tool usage
 - Durable Conversation Manager
 - Durable Task Manager

## Usage

Here's how to get started with the conversation manager, which you can also find in the examples under basic_conversation.clj

```clojure
(require '[multi-gpt.repl-interface :refer :all]
         '[clojure.pprint :refer [pprint]])
;;
;; Setup the system (includes a conversation manager)
;;
(def system (setup-system "api-key"
                          "org-id"
                          "gpt-3.5-turbo"))
;;
;; Create a conversation
;;
(def conversation (create-conversation system))

;;
;; Create a convenience function for chatting as the user role ;;
;;
(def chat (fn [message] 
              (update-conversation system conversation [{:role "user" :content message}])))

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
(update-conversation 
 system 
 conversation 
 [{:role "system" :content "You will help me develop the next breakthrough in distributed systems."}])

;;
;; Engage in conversation
;;
(chat "Please provide the most cutting edge research topics about these systems.")    
```

## License

Copyright 2023 Cameron Barre

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
