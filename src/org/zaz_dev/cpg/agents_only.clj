(ns org.zaz-dev.cpg.agents-only
  (:require (org.zaz-dev.cpg
             [password-generator :refer :all]
             [utils :refer :all]))
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; I don't think agents are a very good fit for this type of
;;;; task as for several reasons.  However, they were the first
;;;; thing I noticed that provide multi-threading in clojure, so
;;;; I wrote something that used them.
;;;;
;;;; It might make more sense to reimplement this using some
;;;; form of blocking queue.
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-agents
  "Makes n agents to be used to run the action-run-rar function."
  [n]
  (into [] (doall  (map #(agent % :error-mode :continue) (range n)))))

(defn action-run-rar
  "A function intended to be run via an agent.  The first
  arguement is the agent's value and is always returned.
  
  The remaining arguments are passed to the run-rar function.
  If the results of that provide an :exit-value of 0(zero),
  the ans atom is updated with the passowrd."
  [agent-state rarfile password f]
  (let [res (run-rar rarfile password f)]
    (if (= (:exit-value res) 0)
      (do
        (swap! ans #(identity %2) password)
        agent-state)
      agent-state)))

(defn agents-only
  "Sends the passwords to the agents who then test them
  by calling rar using the action-run-rar function."
  [{:keys [passwords count]} fname num-threads]
  (let [rf fname
        f (check-rarfile rf)
        start-time (now)
        pwd-cnt count
        agents (make-agents num-threads)]
    (println "\n\n>>>> New agent run with" num-threads "threads<<<<")
    (let [pwds passwords]
      (reset! ans false)
      (loop [x 0
             pwd (first pwds)
             rst (rest pwds)]
        (if @ans
          (println (str "Answer found '" @ans "' in "
                        (/ (- (now) start-time) 1000.0) " seconds."))
          (let [idx (mod x num-threads)
                ag (agents idx)]
            (if (and (= 0 (mod x 1000)) (> x 0))
              (println "Password" x "of" pwd-cnt "is <|" pwd "|>"
                       (format "%.02f" (/ (*  x 1000.0) (- (now) start-time)))
                       "pwds/sec"))

            ;; I'm using await to throttle how fast I'm sending passwords
            ;; to the agents.  I don't expect this is a good thing to do.
            (await ag)

            ;; send-off is used as it uses an expanding Thread pool to
            ;; deal with potentially blocking operations.  Running the
            ;; rar program causes a fork and exec as well as the attempt
            ;; to decrypt the file, both of which can wait on I/O. 
            (send-off ag action-run-rar rf pwd f)
            (if (empty? rst)
              (do
                (apply await-for 5000 agents)
                (if @ans
                  (println (str "Answer found '" @ans "' in " (/ (- (now) start-time) 1000.0) " seconds."))
                  (println "No more passwords to try, ans:" @ans)))
              (recur (inc x) (first rst) (rest rst)))))))))
