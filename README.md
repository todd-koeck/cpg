# cpg: Clojure Password Guesser

This program attempts to brute-force guess passwords for rar files.
While the program has that function, the primary reason for writing this
program was to gain some experience with multi-threading in clojure.
Brute force guessing passwords of more than 4 or 5 characters in length
as implemented in this program will take a long time.

### Password Generation
The program uses the functions in password_generator.clj to generate a
list of passwords which will be tested against the rar file using the
rar program.  Those passwords are tested either one at a time or in
parallel, depending on which backend is chosen.

Passwords can either be specified by the contents of a file, one password per
line, or via a password specification, e.g. "(:lower-case :upper-case :length 4)".

### Backends

Currently, there are three backend implementations available in the program.  The
first implementation performs the password guessing in a single thread and can be found in the single\_threaded.clj file.  The second implementation, in agents\_only.clj, uses agents to guess passwords on multiple threads.  A third implementation using LinkedBlockingQueue, from java\.util\.concurrent, and Threads
can be found in blocking\_queue.clj.

# Installation

You'll need [leiningen](https://leiningen.org/) to build and/or run the program.  Follow the instructions on the [leiningen](https://leiningen.org/) website to
install it, if you haven't already.

Clone the repository and use lein to build the uberjar to run via *java -jar* or clone and run via *lein run*.

	 $ git clone https://github.com/toddk/cpg.git
	 $ lein uberjar

# Usage

	 $ java -jar target/uberjar/cpg-0.1.0-SNAPSHOT-standalone.jar [options] filename
	-or-
	 $ lein run -- [options] filename

## Options

	$ lein run -- -h
	Options:
	  -p, --passwords [password-spec|filename]  nil               Password filename or specification.
	  -b, --backend [single-threaded|agent]     :single-threaded  Backend type
	  -n, --num-threads [N]                     nil               Number of threads for the agent backend, defaults to 20
	  -v, --verbose                                               Enable verbose output
	  -h, --help                                                  Show usage help.
	
	You must provide a password spec of some sort.  A password spec
	is either the name of a file that contains one password per line
	or a clojure list.
	
	The clojure list should contain a string to represent the characters
	used to generate the passwords or a list of one or more of the
	following keywords; :lower-case, :upper-case, :digits,
	:common-special or :all-chars.
	
	The length of the passwords must also be set.  You can use the
	keywords :min-length, :max-length, :length to do so.
	
	Examples:
	    "(:lower-case :upper-case :length 4)"
	    "(:all-chars :min-length 1 :max-length 5)"
	    "(\"this is my password\" :length 12)"

## Examples

	$ lein run -- -b agent -p "(:lower-case :min-length 1 :max-length 3)" -n 40 -h test-PWD\=zzz.rar
	Password count: 18278 backend: :agent file: test-PWD=zzz.rar
	Unencrypted headers, first file: ' passwords2.txt '
	
	
	>>>> New run with 40 threads<<<<
	Password 1000 of 18278 is <| alm |> 445.43 pwds/sec
	Password 2000 of 18278 is <| bxy |> 450.25 pwds/sec
	Password 3000 of 18278 is <| dkk |> 451.26 pwds/sec
	Password 4000 of 18278 is <| eww |> 452.74 pwds/sec
	Password 5000 of 18278 is <| gji |> 452.98 pwds/sec
	Password 6000 of 18278 is <| hvu |> 452.59 pwds/sec
	Password 7000 of 18278 is <| jig |> 453.69 pwds/sec
	Password 8000 of 18278 is <| kus |> 453.69 pwds/sec
	Password 9000 of 18278 is <| mhe |> 453.95 pwds/sec
	Password 10000 of 18278 is <| ntq |> 453.45 pwds/sec
	Password 11000 of 18278 is <| pgc |> 453.44 pwds/sec
	Password 12000 of 18278 is <| qso |> 453.82 pwds/sec
	Password 13000 of 18278 is <| sfa |> 454.23 pwds/sec
	Password 14000 of 18278 is <| trm |> 453.71 pwds/sec
	Password 15000 of 18278 is <| vdy |> 453.75 pwds/sec
	Password 16000 of 18278 is <| wqk |> 453.75 pwds/sec
	Password 17000 of 18278 is <| ycw |> 454.08 pwds/sec
	Password 18000 of 18278 is <| zpi |> 454.05 pwds/sec
	Found password: 'zzz' in 40.323 seconds.
	--------
	Completed
	$ 

# Bugs

Plenty, I'm sure.

A rar program must be installed in the PATH for the program to run.

The program was tested until Debian Stretch, Buster and Bullseye.  It should
run on most \*nix installations that can run clojure.  It should run on
Windows, but wasn't tested there.

# Possible Enhancements

None of the enhancements below are planned.  This list is simply  a
place to collect ideas as I've thought of them.

The purpose of this project was to learn some parts of Clojure that
I hadn't played with before. Brute force guessing passwords has severe
limitations which I don't expect to overcome in this project.

If at some point in the future, implementing something from this list
serves my purposes of learning Clojure, I may do so.

* cpg has no special knowledge of rar files and no special way to access rar
files.  It uses the rar and 7z programs to test if a password is correct.
* Support similar functionality for .7z and .zip files.
* Add a GUI for those afraid of the command line.

## License

Copyright © 2020 Todd K.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
