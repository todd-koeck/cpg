(ns org.zaz-dev.cpg.blocking-queue
  (:require (org.zaz-dev.cpg
             [password-generator :refer :all]
             [utils :refer :all]))
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; This strategy usings a java.util.concurrent.LinkedBlockingQueue
;;;; as the communication and throttling mechanism for the
;;;; java.lang.Threads spawned to test the passwords on the rar
;;;; file.
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def lb-queue (java.util.concurrent.LinkedBlockingQueue. max-queue-length))

(defn poll-next
  "Grab the next item from the LinkedBlockingQueue."
  ([queue] (poll-next queue default-timeout))
  ([queue timeout]
   (.poll queue timeout java.util.concurrent.TimeUnit/MILLISECONDS)))

(defn consumer
  "Consume the passwords from the LinkedBlockingQueue via
  the poll-next function and call run-rar to test the password."
  [queue cn rar-file first-file]
  #_(println (format "Consumer %03d started" cn))
  (loop [pwd (poll-next queue)]
    #_(println (""))
    (if @ans
      (do
        #_(println "Consumer" cn "complete.")
        nil)
      (do
        (when pwd
          (let [res (run-rar rar-file pwd first-file)]
            (if (= (:exit-value res) 0)
              (swap! ans #(identity %2) pwd)
              (recur (poll-next queue)))))))))

(defn make-consumers
  "Make the Threads that will consume the passwords from the LinkedBlocking Queue."
  ([n rar-file first-file] (make-consumers n lb-queue rar-file first-file))
  ([n queue rar-file first-file]
   (map (fn [cn] (Thread. #(consumer queue cn rar-file first-file))) (range n))))

(defn blocking-queue
  "Creates num-threads consumer threads that listen on a LinkedBlockingQueue for
  passwords guesses."
  [{:keys [passwords count]} fname num-threads]
  (let [rf fname
        f (check-rarfile rf)
        start-time (now)
        pwd-cnt count
        stat-cnt (int (/ pwd-cnt 10))
        stat-cnt (min stat-cnt 1000)
        cthreads (make-consumers num-threads rf f)]
    (reset! ans false)
    (println "\n>>>> New blocking queue run with" num-threads "threads <<<<")
    (doall (map #(.start %) cthreads))
    (loop [x 0
           pwd (first passwords)
           rst (rest passwords)]
      #_(println (format "Top of loop %5d, '%s' mod: %d" x pwd (mod x stat-cnt)))
      (if @ans
        (println (str "Answer found '" @ans "' in "
                      (/ (- (now) start-time) 1000.0) " seconds."))
        (do
          (when (and (= 0 (mod x stat-cnt)) (> x 0))
            (println (format "Password %5d of %d is <|%s|> %.02f pwds/sec"
                             x pwd-cnt pwd
                             (/ (*  x 1000.0) (- (now) start-time)))))
          (.put lb-queue pwd)
          (if (empty? rst)
            (do
              (Thread/sleep 1000)
              (if @ans
                (println (str "Answer found '" @ans "' in " (/ (- (now) start-time) 1000.0) " seconds."))
                (println "No more passwords to try, ans:" @ans)))
            (recur (inc x) (first rst) (rest rst))))))))

