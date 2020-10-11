(ns org.zaz-dev.cpg.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string])
  (:require (org.zaz-dev.cpg
             [agents-only :refer [agents-only]]
             [blocking-queue :refer [blocking-queue]]
             [password-generator :refer :all]
             [single-threaded :refer :all]
             [utils :refer :all]))
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; This program uses the rar program to test passwords on rar files.
;;;; The passwords can live in a file or can be generated through a
;;;; password spec.
;;;;
;;;; This is intented as an exercise in learning clojure.  Using this
;;;; program to crack a rar file's password that is more than 4 or 5
;;;; characters will take a long, long time.
;;;;
;;;; Performance on my system:
;;;;
;;;; System: Debian Bullseye, 64GB memory, i9-9900K @ stock clock
;;;;
;;;; Java 11.0.8
;;;; single threaded, iterate-passwords processes about 60 passwords/sec
;;;; agent based, agents-only processes about 473 passwords/sec.
;;;; queue based, blocking-queue processes about 485 passwords/sec.
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-password-opt
  "Parses the password option.  Returns a map that contains a sequence
  of passwords, :passwords, and a count, :count, or nil if opt couldn't
  be parsed.

  opt will be treated as a string and should contain either a filename
  or a password spec.
  
  If opt contains the name of a file, the file is read and each line
  is used as a password.

  If a file named by opt can't be found, the string is expected to contain
  a password spec.

  A password spec is a string that contains a clojure list of one or
  more keywords to define the character sets used or a string containing
  the list of characters.  The size of the passwords to try must also
  be specified by the length keywords below.

  The known character sets can be found in password-generator/char-sets.

  After the list of characters is built, the characters are put into a
  set to ensure their uniqueness.

  The password size must be specified with :min-length and :max-length
  or with :length.  You must specify :length or both :min-length and
  :max-length when specifying a password spec.  These keywords are
  only used for a password spec and can't be used with a filename.

  Example password specs:
     /tmp/passwords.txt
     (:lower-case :upper-case :length 4)
     (:all-chars :min-length 1 :max-length 5)
     (\"this is my password\" :length 12)"
  [opt]
  (cond
    ;; If the first character of the string is a \(, treat it as a list.
    (= (first opt) \() (-> opt
                           parse-password-spec
                           make-pwds-from-spec)

    ;; We'll load the contents of the file to use as a password list.
    (.isFile (java.io.File. opt)) (load-password-file opt)

    ;; It wasn't a list or a filename, we can't do anything.
    :else nil))

(defn usage [options-summary]
  (->> ["Clojure Password Guesser (for rar files)"
        ""
        "Usage: cpg [options] filename"
        ""
        "Options:"
        options-summary
        ""
        "You must provide a password spec of some sort.  A password spec"
        "is either the name of a file that contains one password per line"
        "or a clojure list."
        ""
        "The clojure list should contain a string to represent the characters"
        "used to generate the passwords or a list of one or more of the"
        "following keywords; :lower-case, :upper-case, :digits,"
        ":common-special or :all-chars."
        ""
        "The length of the passwords must also be set.  You can use the"
        "keywords :min-length, :max-length, :length to do so."
        ""
        "Examples:"
        "    \"(:lower-case :upper-case :length 4)\""
        "    \"(:all-chars :min-length 1 :max-length 5)\""
        "    \"(\\\"this is my password\\\" :length 12)\""]
       (string/join \newline)))

(def cli-options
  ;; An option with a required argument
  [["-p" "--passwords [password-spec|filename]"
    "Password filename or specification."
    :default nil
    :parse-fn #(parse-password-opt %)
    :validate [#(and %) "Must be the name of a filename containing passwords."]]

   ["-b" "--backend [single-threaded|agent|queue]" "Backend type"
    :default :single-threaded
    :parse-fn #(keyword %)
    :validate [#(or (= % :single-threaded) (= % :agent) (= % :queue))
               "Backend should be one of 'single-threaded', 'agent' or 'queue'"]]

   ["-n" "--num-threads [N]" (str "Number of threads for the agent backend, "
                                  "defaults to " default-num-threads)
    :default default-num-threads
    :parse-fn #(Integer/parseInt %)
    :validate [#(and (> % 0) (<= % 100))
               "The number of threads must be greater than 0 and less than 100."]]

   ;; A non-idempotent option (:default is applied first)
   ["-v" "--verbose" "Enable verbose output"
    :id :verbosity
    :default 0
    :update-fn inc]

   ;; A boolean option defaulting to nil
   ["-h" "--help" "Show usage help."]])


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      ;; help => exit OK with usage summary
      (:help options)
      {:exit-message (usage summary) :ok? true}

      ;; errors => exit with description of errors
      errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (= 1 (count arguments))
      {:filename (first arguments) :options options}

      ;; failed custom validation => exit with usage summary
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "Attempt to brute force guess a password for a rar file.

  This is an exercise at writing a clojure program and not one in
  cracking passwords.   While it may crack a password for a rar file,
  it'll be   slow for any passwords larger than 4 or 5 characters in
  length."
  [& args]
  (let [{:keys [filename options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [opts options
            verbose (:verbosity opts)
            pwds (:passwords opts)
            backend (:backend opts)
            num-threads (:num-threads opts)
            arguments opts]
        #_(println "verbose:" verbose "pwd-spec:" pwd-spec "arguments: "
                   arguments "backend:" backend)
        (println "Password count:" (:count pwds) "backend:" backend "file:" filename)
        (cond
          (= backend :single-threaded) (iterate-passwords pwds filename)
          (= backend :queue) (blocking-queue pwds filename num-threads)
          (= backend :agent) (agents-only pwds filename num-threads))
        #_(println "passwords:" pwds)
        #_(iterate-passwords pwds (arguments 0))
        (println "--------\nCompleted")
        (System/exit 0)))))
