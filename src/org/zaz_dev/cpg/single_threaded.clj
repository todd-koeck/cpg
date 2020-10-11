(ns org.zaz-dev.cpg.single-threaded
  (:require (org.zaz-dev.cpg
             [password-generator :refer :all]
             [utils :refer :all]))
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; This is an example of brute force guessing a password for
;; a rar file, trying one password at a time.  A simple
;; loop is used and the passwords are processed in the caller's
;; thread.
;;
(defn iterate-passwords
  "Iterates through the sequence of passwords, running rar on the file specified
  by fname.

  passwords - the sequence of passwords to try.
  fname - the name of the rar file."
  [{:keys [passwords count]} fname]
  (let [pwd-cnt count
        start-time (now)
        f (check-rarfile fname)]
    (loop [pwd (first passwords)
           rst (rest passwords)
           n 1]
      (let [res (run-rar fname pwd f)]
        (if (= 0 (mod n 100))
          (println "Password" n "of" pwd-cnt "is <|" pwd
                   "|> res:" (:exit-value res)
                   (format "%.02f" (/ (*  n 1000.0) (- (now) start-time)))
                   "pwds/sec"))
        (cond 
          (= 0 (:exit-value res)) (println (str  "Good password: <|" pwd "|>"))
          (empty? rst) (println "Terminating on no more passwords.")
          :else (recur (first rst) (rest rst) (inc n)))))))
