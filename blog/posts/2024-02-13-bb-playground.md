Title: Playing on the Babashka playground
Date: 2024-02-13
Tags: clojure,babashka
Description: In which I construct a playground and proceed to have fun
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1707819270474379
Image: assets/2024-02-13-bb-playground-preview.png
Image-Alt: A child with the Babashka logo as a face on a playground slide

![A child with the Babashka logo as a face on a playground slide][preview]
[preview]: assets/2024-02-13-bb-playground-preview.png "Photo by Amber Faust on Unsplash" width=800px

As a longtime Linux user, I often find myself starting to type a complicated
command. As a longtime programmer, I often find myself thinking, "Maybe I should
just automate this," and open up a `whatever.sh` in Emacs. As a longtime Bash
scripter, I often find myself hundreds of lines into a script and looking up how
to do string substition for the 60,000th time. As a recent-ish convert to
[Babashka](https://babashka.org/), I often find myself closing the `whatever.sh`
buffer quickly and opening `whatever.bb` instead. As a true believer in the
power of the REPL, I often ask myself why I'm writing code in `whatever.bb` and
then executing it in my terminal and rolling my eyes when it doesn't work and
going back to Emacs and changing something and executing it in my terminal again
like a caveman instead of just **C-c C-v f c e**-ing like a normal person.

Surely there must be a better way!

![Blackadder saying: I've got a plan so cunning you could put a tail on it and call it a weasel][cunning]
[cunning]: assets/2024-02-13-cunning-plan.png "I too have a cunning plan!"

Enter the playground! And not just any playground, but a playground where joyous
Babashkas (Babashki?) frolic, REPLing their little hearts out!

Here's what I did:

## Constructing the playground

First, I created a directory called `bb-playground` and dropped a `bb.edn` in
it:

``` clojure
{:paths ["dev" "src"]}
```

Now if I create a `dev/user.clj`, I can start playing around in my REPL:

``` clojure
(ns user
  (:require [clojure.string :as str]))

;; 1. Start a REPL with C-c M-j
;; 2. Evaluate this buffer with C-c C-k

(comment

  ;; Do fun stuff here by putting your cursor at the end of an expression and
  ;; whacking C-c C-v f c e

  (->> (System/getProperties)
       (filter (fn [[k _]] (str/starts-with? k "babashka.")))
       (into {}))
  ;; => {"babashka.version" "1.3.188",
  ;;     "babashka.config" "/home/jmglov/Documents/code/bb-playground/bb.edn"}

  )
```

That was quite easy, and very useful if I just want to play around with Clojure
or any of the [libraries that ship with
Babashka](https://book.babashka.org/#libraries), but what if I want to do
something like list all of the objects in a certain S3 bucket with a specific
prefix?

## Making the playground more fun

In order to do what I want, I'm going to need my old favourite
[awyeah-api](https://github.com/grzm/awyeah-api), which itself needs some
friends from the the Cognitect
[aws-api](https://github.com/cognitect-labs/aws-api) library:

``` clojure
{:deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.504"}
        com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "e5513349a2fd8a980a62bbe0d45a0d55bfcea141"
                             :git/tag "v0.8.84"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "1a841c4cc1d4f6dab7505a98ed2d532dd9d56b78"}}}
```

So I could just paste those deps into my `bb.edn`, but I want the latest and
greatest from the AWS APIs, which I can grab from
[aws-api/latest-releases.edn](https://github.com/cognitect-labs/aws-api/blob/main/latest-releases.edn),
but then I have to get git hashes for awyeah-api and org.babashka/spec.alpha and
paste them into my `bb.edn` and that seems like a lot of work that I'm too lazy
to do.

What I can do instead is use the power of
[neil](https://github.com/babashka/neil) to add my dependencies for me!

Since [someone](https://github.com/jlesquembre) has been lovely enough to add
neil to
[nixpkgs](https://github.com/NixOS/nixpkgs/blob/c0b7a892fb042ede583bdaecbbdc804acb85eabe/pkgs/development/tools/neil/default.nix),
installing it is as easy as plopping it into my
[home.nix](https://github.com/jmglov/nixos-config/blob/59983ddf4e959f634d0d87aee9fbdd2a01f8ce95/jmglov/home.nix#L37)
and running:

``` text
: ~; sudo nixos-rebuild switch 
```

Now I can add dependencies like this:

``` text
: bb-playground; neil dep add --deps-file bb.edn com.cognitect.aws/endpoints
```

The only problem is that I'm never in a million years going to remember all the
arguments to neil. 🤔

## Working around my poor memory

What if my `bb.edn` knew how to add dependencies to itself? Like, could I just
run:

``` text
bb add-dep com.cognitect.aws/s3
```

and be done with it? Even I should be able to remember that! 😅

Well, Babashka has this thing called the [task
runner](https://book.babashka.org/#tasks), whereby you can drop stuff like this
in your `bb.edn`:

``` clojure
{:tasks
 {:requires ([babashka.fs :as fs])
  clean (do (println "Removing target folder.")
            (fs/delete-tree "target"))
  }
 }
```

and then run:

``` text
$ ls target
total 4
-rw-r--r-- 1 jmglov users 107 Dec 18 13:28 stuff.jar
$ bb clean
Removing target folder.
$ ls target
ls: cannot access 'target': No such file or directory
```

So with this, let's add a task to our `bb.edn`!

``` clojure
{:paths ["dev" "src"]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.626"}}
 :aliases {}
 :tasks
 {add-dep (println "What to do, what to do?")}}
```

And try it out:

``` text
: bb-playground; bb add-dep
What to do, what to do?
```

Cool! Now, if we think about what we want to do, it's basically: prepend "neil
dep add --deps-file bb.edn" to the command line passed to `bb add-dep`.
[According to the docs](https://book.babashka.org/#_command_line_arguments): >

> Command line arguments are available as *command-line-args*, just like in
> Clojure.

We can drop this in our `bb.edn`:

``` clojure
{ ; ...
 :tasks
 {add-dep (println "ARGS:" *command-line-args*)}}}
```

And see what the command line looks like when we play around:

``` text
: bb-playground; bb add-dep com.cognitect.aws/s3
ARGS: (com.cognitect.aws/s3)
```

OK, nice. So now we want to pass that along to neil. We can do this with
[babashka.process/shell](https://github.com/babashka/process#shell):

``` clojure
{ ; ...
:tasks
 {:requires ([babashka.process :as p])
  add-dep (apply p/shell "neil dep add --deps-file bb.edn" *command-line-args*)}}
```

Now let's try this out:

``` text
: bb-playground; bb add-dep com.cognitect.aws/s3

: bb-playground; cat bb.edn 
{:paths ["dev" "src"]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.626"} com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}}
 :aliases {}
 :tasks
 {:requires ([babashka.process :as p]
             [clojure.string :as str])
  add-dep (apply p/shell "neil dep add --deps-file bb.edn" *command-line-args*)}}
```

OMG wat!

Another cool thing we can do with the task runner is ask it what tasks we can
run:

``` text
: bb-playground; bb tasks
The following tasks are available:

add-dep
```

We can even make this nicer by adding a description to the task:

``` clojure
{ ; ...
:tasks
 {:requires ([babashka.process :as p])
  add-dep {:doc "Add a dependency to the playground"
           :task (apply p/shell "neil dep add --deps-file bb.edn" *command-line-args*)}}}
```

Now we get some additional memory joggage:

``` text
: bb-playground; bb tasks
The following tasks are available:

add-dep Add a dependency to the playground
```

OK, but what if we don't remember what args `add-dep` takes? Let's try asking
for help:

``` text
: bb-playground; bb add-dep --help
Usage: neil add dep [lib] [options]
Options:
  --lib                         Fully qualified library name.
  --version                     Optional. When not provided, picks newest version from Clojars or Maven Central.
  --sha                         When provided, assumes lib refers to Github repo.
  --latest-sha                  When provided, assumes lib refers to Github repo and then picks latest SHA from it.
  --tag                         When provided, assumes lib refers to Github repo.
  --latest-tag                  When provided, assumes lib refers to Github repo and then picks latest tag from it.
  --deps/root                   Sets deps/root to give value.
  --as                          Use as dependency name in deps.edn
  --alias      <alias>          Add to alias <alias>.
  --deps-file  <file>  deps.edn Add to <file> instead of deps.edn.
```

Oh neat! Of course, it's a bit confusing that the usage line says "neil add dep"
instead of "bb add-dep". Let's fix that!

``` clojure
{ ; ...
 :tasks
 {:requires ([babashka.process :as p]
             [clojure.string :as str])
  add-dep {:doc "Add a dependency to the playground"
           :task (let [args (or *command-line-args* ["--help"])
                       neil-args (concat ["neil" "dep" "add" "--deps-file" "bb.edn"] args)
                       {:keys [out]} (apply p/shell {:out :string} neil-args)]
                   (if (= "--help" (first args))
                     (->> [(str/replace out "Usage: neil add dep" "Usage: bb add-dep")
                           "Examples:\n"
                           "bb add-dep com.cognitect.aws/endpoints"
                           "bb add-dep com.cognitect.aws/s3 --version 848.2.1413.0"
                           "bb add-dep grzm/awyeah-api --latest-sha"]
                          (str/join "\n")
                          println)
                     (println out)))}}}
```

Now if we ask for help:

``` text
Usage: bb add-dep [lib] [options]
Options:
  --lib                         Fully qualified library name.
  --version                     Optional. When not provided, picks newest version from Clojars or Maven Central.
  --sha                         When provided, assumes lib refers to Github repo.
  --latest-sha                  When provided, assumes lib refers to Github repo and then picks latest SHA from it.
  --tag                         When provided, assumes lib refers to Github repo.
  --latest-tag                  When provided, assumes lib refers to Github repo and then picks latest tag from it.
  --deps/root                   Sets deps/root to give value.
  --as                          Use as dependency name in deps.edn
  --alias      <alias>          Add to alias <alias>.
  --deps-file  <file>  deps.edn Add to <file> instead of deps.edn.

Examples:

bb add-dep com.cognitect.aws/endpoints
bb add-dep com.cognitect.aws/s3 --version 848.2.1413.0
bb add-dep grzm/awyeah-api --latest-sha
```

The one problem with all of this is that we've been doing the thing that I was
complaining about at the top: writing some code, saving the file, executing it
in a terminal, realising it doesn't quite work, going back to the editor... etc.

![Christopher Walken cowbell meme: I've got a fever... and the only prescription is more REPL][cowbell]
[cowbell]: assets/2024-02-13-cowbell.jpg "Bring me your finest CIDER!"

## Getting back to the REPL

OK, so remember the REPL we had running over in `dev/user.clj`? We can use that
to drive our `bb.edn` development. Let's create a `src/tasks.clj` file and copy
all the `add-dep` stuff over to it:

``` clojure
(ns tasks
  (:require [babashka.process :as p]
            [clojure.string :as str]))

(defn add-dep [command-line-args]
  (let [args (or command-line-args ["--help"])
        neil-args (concat ["neil" "dep" "add" "--deps-file" "bb.edn"] args)
        {:keys [out]} (apply p/shell {:out :string} neil-args)]
    (if (= "--help" (first args))
      (->> [(str/replace out "Usage: neil add dep" "Usage: bb add-dep")
            "Examples:\n"
            "bb add-dep com.cognitect.aws/endpoints"
            "bb add-dep com.cognitect.aws/s3 --version 848.2.1413.0"
            "bb add-dep grzm/awyeah-api --latest-sha"]
           (str/join "\n")
           println)
      (println out))))
```

Let's give it a **C-c C-k** to evaluate the buffer, just to make sure everything
is in order. 😉

Now we can use this from `bb.edn`

``` clojure
{:paths ["dev" "src"]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.626"}
        com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}}
 :aliases {}
 :tasks
 {:requires ([tasks])
  add-dep {:doc "Add a dependency to the playground"
           :task (tasks/add-dep *command-line-args*)}}}
```

And just to verify that it still works:

``` text
: bb-playground; bb add-dep --help
Usage: bb add-dep [lib] [options]
Options:
  --lib                         Fully qualified library name.
  --version                     Optional. When not provided, picks newest version from Clojars or Maven Central.
  --sha                         When provided, assumes lib refers to Github repo.
  --latest-sha                  When provided, assumes lib refers to Github repo and then picks latest SHA from it.
  --tag                         When provided, assumes lib refers to Github repo.
  --latest-tag                  When provided, assumes lib refers to Github repo and then picks latest tag from it.
  --deps/root                   Sets deps/root to give value.
  --as                          Use as dependency name in deps.edn
  --alias      <alias>          Add to alias <alias>.
  --deps-file  <file>  deps.edn Add to <file> instead of deps.edn.

Examples:

bb add-dep com.cognitect.aws/endpoints
bb add-dep com.cognitect.aws/s3 --version 848.2.1413.0
bb add-dep grzm/awyeah-api --latest-sha
```

From now on, we won't need to leave our REPL to develop our tasks! Let's prove
it by making `add-dep` print out the new dependencies after adding them:

``` clojure
(ns tasks
  (:require [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]))

(defn add-dep [command-line-args]
  (let [ ; ...
        ]
    (if (= "--help" (first args))
      ;; ...
      (do
        (println "Dependency added. Dependencies are now:")
        (-> (slurp "bb.edn")
            edn/read-string
            :deps
            pprint)))))

(comment

  (add-dep ["grzm/awyeah-api"])
  ;; nil

  )
```

The REPL buffer should now say:

``` text
Dependency added. Dependencies are now:
{com.cognitect.aws/endpoints #:mvn{:version "1.1.12.626"},
 com.cognitect.aws/s3 #:mvn{:version "848.2.1413.0"},
 grzm/awyeah-api
 #:git{:url "https://github.com/grzm/awyeah-api",
       :sha "d98a9f6210c61d64f22e9b577d2254d6f6d2f35f"}}
```

Hoorah! And just to prove that this also works from the terminal:

``` text
: bb-playground; bb add-dep babashka/spec.alpha
Dependency added. Dependencies are now:
{com.cognitect.aws/endpoints #:mvn{:version "1.1.12.626"},
 com.cognitect.aws/s3 #:mvn{:version "848.2.1413.0"},
 grzm/awyeah-api
 #:git{:url "https://github.com/grzm/awyeah-api",
       :sha "d98a9f6210c61d64f22e9b577d2254d6f6d2f35f"},
 babashka/spec.alpha
 #:git{:url "https://github.com/babashka/spec.alpha",
       :sha "951b49b8c173244e66443b8188e3ff928a0a71e7"}}
```

## So about listing that bucket...

If we pop back over to `user.clj` and give it a **C-c C-k** get our REPL firmly
planted back in that namespace, let's start REPL-driving some S3 goodness:

``` clojure
(ns user
  (:require [clojure.string :as str]
            [com.grzm.awyeah.client.api :as aws]))

(comment

  (def s3 (aws/client {:api :s3}))

  )
```

Tragically, the second we try to evaluate this, everything goes a bit sideways:

``` text
clojure.lang.ExceptionInfo: Could not locate com/grzm/awyeah/client/api.bb, com/grzm/awyeah/client/api.clj or com/grzm/awyeah/client/api.cljc on classpath.
{:type :sci/error, :line 2, :column 3, :message "Could not locate com/grzm/awyeah/client/api.bb, com/grzm/awyeah/client/api.clj or com/grzm/awyeah/client/api.cljc on classpath.", :sci.impl/callstack #object[clojure.lang.Volatile 0x70ac29ff {:status :ready, :val ({:line 2, :column 3, :file "/home/jmglov/Documents/code/bb-playground/dev/user.clj", :ns #object[sci.lang.Namespace 0x460699aa "user"]})}], :file "/home/jmglov/Documents/code/bb-playground/dev/user.clj"}
 at sci.impl.utils$rethrow_with_location_of_node.invokeStatic (utils.cljc:135)
    [...]
Caused by: java.io.FileNotFoundException: Could not locate com/grzm/awyeah/client/api.bb, com/grzm/awyeah/client/api.clj or com/grzm/awyeah/client/api.cljc on classpath.
 at babashka.main$exec$fn__32207$load_fn__32218.invoke (main.clj:924)
    sci.impl.load$handle_require_libspec.invokeStatic (load.cljc:163)
    [...]
```

Oh yes, our REPL was started before we added all the dependencies. 🤦🏼

Now, if you thought for a second about restarting the REPL, this is clearly your
first time on this blog. We never take the coward's way out around here! Even
(or especially) when that would be super fast and easy and the alternative is
many many keystrokes and the occasional muttered curse word under our breath!

So let's screw our courage to the sticking point and hotload those damned
dependencies! (OK, so the cursing isn't always under our breath.)

``` clojure
(ns user
  (:require [babashka.deps :as deps]
            [clojure.edn :as edn]))

(comment

  (-> (slurp "bb.edn")
      edn/read-string
      deps/add-deps)
  ;; => nil

  (require '[com.grzm.awyeah.client.api :as aws])
  ;; => nil

  )
```

Exceptional! Or rather, not exceptional, since no exceptions were thrown. Which
is what we wanted. 😅

This actually looks pretty useful, so let's make it a function so we can just
call it whenever we add a dependency:

``` clojure
(ns user
  (:require [babashka.deps :as deps]
            [clojure.edn :as edn]))

(defn refresh-deps []
  (-> (slurp "bb.edn")
      edn/read-string
      deps/add-deps))
```

Having done this, we can get back to listing stuff in S3.

``` clojure
(comment

  (def s3 (aws/client {:api :s3, :region "eu-west-1"}))
  ;; => #'user/s3

  (->> (aws/invoke s3 {:op :ListObjectsV2
                       :request {:Bucket "misc.jmglov.net"}})
       :Contents
       count)
  ;; => 1000

  )
```

Oh my, that's a lot of stuff! And it's somewhat suspicious that it's exactly
1000 stuffs. Especially since 1000 is the default page size for the
[ListObjectsV2](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html)
API request. In fact, I seem to remember writing a couple
[blog](2022-09-22-aws-paging.html) [posts](2022-10-02-page-2.html) about paging
and S3. I'll just go ahead and liberate some code from that second post there:

``` clojure
(ns user
  (:require [babashka.deps :as deps]
            [com.grzm.awyeah.client.api :as aws]
            [clojure.edn :as edn])
  (:import (java.time Instant)))

(defmacro ->map [& ks]
  (assert (every? symbol? ks))
  (zipmap (map keyword ks)
          ks))

(defn lazy-concat [colls]
  (lazy-seq
   (when-first [c colls]
     (lazy-cat c (lazy-concat (rest colls))))))

(defn log [msg data]
  (prn {:msg msg
        :data data
        :timestamp (str (Instant/now))}))

(defn error [msg data]
  (log msg data)
  (throw (ex-info msg data)))

(defn validate-aws-response [res]
  (when (:cognitect.anomalies/category res)
    (let [data (merge (select-keys res [:cognitect.anomalies/category])
                      {:err-msg (:Message res)
                       :err-type (:__type res)})]
      (error "AWS request failed" data)))
  res)

(defn mk-s3-req
  ([s3-bucket prefix s3-page-size]
   (mk-s3-req s3-bucket prefix s3-page-size nil))
  ([s3-bucket prefix s3-page-size continuation-token]
   (merge {:Bucket s3-bucket
           :Prefix prefix}
          (when s3-page-size
            {:MaxKeys s3-page-size})
          (when continuation-token
            {:ContinuationToken continuation-token}))))

(defn get-s3-page [{:keys [s3-client s3-bucket s3-page-size]}
                   prefix
                   {continuation-token :NextContinuationToken
                    truncated? :IsTruncated
                    page-num :page-num
                    :as prev}]
  (when prev (log "Got page" (dissoc prev :Contents)))
  (let [page-num (inc (or page-num 0))
        done? (false? truncated?)
        request (mk-s3-req s3-bucket prefix s3-page-size continuation-token)
        response (when-not done?
                   (log (format "Requesting page %d" page-num) request)
                   (-> (aws/invoke s3-client {:op :ListObjectsV2
                                              :request request})
                       validate-aws-response
                       (assoc :page-num page-num)))]
    response))

(defn list-objects [{:keys [s3-bucket limit] :as logs-client} prefix]
  (log "Listing S3 objects" (merge (->map s3-bucket prefix)
                                   (when limit {:limit limit})))
  (let [apply-limit (if limit (partial take limit) identity)]
    (->> (iteration (partial get-s3-page logs-client prefix)
                    :vf :Contents)
         lazy-concat
         apply-limit
         (map :Key))))
```

I won't explain all this here. If you're curious, please do read the [Page
2](page-2.html) post.

In any case, having done all of this, let's try using it:

``` clojure
(comment

  (def cfg {:aws-region "eu-west-1"
            :s3-bucket "misc.jmglov.net"
            :s3-page-size 1000})
  ;; => #'user/cfg

  (def ctx (assoc cfg :s3-client
                  (aws/client {:api :s3, :region "eu-west-1"})))
  ;; => #'user/ctx

  (->> (list-objects ctx "")
       (take 5))
  ;; => (".write_access_check_file.temp"
  ;;     "1-what-do-i-want.json"
  ;;     "Abeba_Birhane.json"
  ;;     "Adrian_C_Jackson.json"
  ;;     "Advice_Aniyia_Williams.json")

  )
```

Now we're getting somewhere!

## No one is afraid of JSON Voorhees

I recall using this bucket for some transcription I did for the excellent
[Conversations with Kim Crayton](https://blubrry.com/1475055/) podcast, which is
what all those JSON files are. Perhaps I can do some organising here by moving
them to a separate "folder". Let's just see how many I'm dealing with here:

``` clojure
(ns user
  (:require ; ...
            [clojure.string :as str])
  (:import (java.time Instant)))

(comment

  (->> (list-objects ctx "")
       (filter #(str/ends-with? % ".json"))
       count)
  ;; => 224

  )
```

And how many of these are in the "root directory"?

``` clojure
(comment

  (->> (list-objects ctx "")
       (filter #(and (str/ends-with? % ".json")
                     (not (str/includes? % "/"))))
       count)
  ;; => 209

  )
```

That is many! Let's see about moving one into another folder:

``` clojure
(comment

  (def src-filename (->> (list-objects ctx "")
                         (filter #(and (str/ends-with? % ".json")
                                       (not (str/includes? % "/"))))
                         first))
  ;; => #'user/src-filename

  (aws/invoke (:s3-client ctx) {:op :GetObject
                                :request {:Bucket (:s3-bucket ctx)
                                          :Key src-filename}})
  ;; => {:LastModified #inst "2023-02-25T10:40:19.000-00:00",
  ;;     :ETag "\"95a40408c21908a18e596f9b46eb10ac\"",
  ;;     :Body
  ;;     #object[java.io.BufferedInputStream 0x19011b9f "java.io.BufferedInputStream@19011b9f"],
  ;;     :Metadata {},
  ;;     :ServerSideEncryption "AES256",
  ;;     :ContentLength 290662,
  ;;     :ContentType "binary/octet-stream",
  ;;     :AcceptRanges "bytes",
  ;;     :VersionId "ROt3VOKf67.OaoHFFsBTWCVSL4vIb7MI"}

  )
```

OK, seems like the `:Body` is what we want here. It's an input stream, which is
exactly what the
[PutObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html)
API request wants, according to
[aws-api/examples/s3_examples.clj](https://github.com/cognitect-labs/aws-api/blob/44711d911988b4d8dd309c19277ce53848605b49/examples/s3_examples.clj#L64).
Let's give it a shot:

``` clojure
(comment

  (def target-filename (format "podcasts/conversations-with-kim-crayton/%s" src-filename))
  ;; => #'user/target-filename

  (let [{:keys [s3-client s3-bucket]} ctx
        obj (aws/invoke s3-client {:op :GetObject
                                   :request {:Bucket s3-bucket
                                             :Key src-filename}})]
    (aws/invoke s3-client {:op :PutObject
                           :request {:Bucket s3-bucket
                                     :Key target-filename
                                     :Body (:Body obj)}}))
  ;; => {:ETag "\"95a40408c21908a18e596f9b46eb10ac\"",
  ;;     :ServerSideEncryption "AES256",
  ;;     :VersionId "KuURLdZOq.qwk3Q6OYNG92Q2C49JYivc"}

  (aws/invoke (:s3-client ctx) {:op :GetObject
                                :request {:Bucket (:s3-bucket ctx)
                                          :Key target-filename}})
  ;; => {:LastModified #inst "2024-02-08T15:37:40.000-00:00",
  ;;     :ETag "\"95a40408c21908a18e596f9b46eb10ac\"",
  ;;     :Body
  ;;     #object[java.io.BufferedInputStream 0x65579e67 "java.io.BufferedInputStream@65579e67"],
  ;;     :Metadata {},
  ;;     :ServerSideEncryption "AES256",
  ;;     :ContentLength 290662,
  ;;     :ContentType "binary/octet-stream",
  ;;     :AcceptRanges "bytes",
  ;;     :VersionId "KuURLdZOq.qwk3Q6OYNG92Q2C49JYivc"}

  )
```

Now all we have to do is remove the "file" from the "root directory":

``` clojure
(comment

  (aws/invoke (:s3-client ctx) {:op :DeleteObject
                                :request {:Bucket (:s3-bucket ctx)
                                          :Key src-filename}})
  ;; => {:DeleteMarker true, :VersionId "sY4dbN7Knyff0B67Id5BeenVAT1bmN.k"}

  (aws/invoke (:s3-client ctx) {:op :GetObject
                                :request {:Bucket (:s3-bucket ctx)
                                          :Key src-filename}})
  ;; => {:Error
  ;;     {:HostIdAttrs {},
  ;;      :KeyAttrs {},
  ;;      :Message "The specified key does not exist.",
  ;;      :Key "1-what-do-i-want.json",
  ;;      :CodeAttrs {},
  ;;      :RequestIdAttrs {},
  ;;      :HostId
  ;;      "kE4zTuMao5e8TbMPn7rs1h48fNc9kEuMfBqLmayvcP+/SmEfbgfBGCsmJ3iZKcl6hpeyYKvSWXU=",
  ;;      :MessageAttrs {},
  ;;      :RequestId "YV31XT7C98PQWTMX",
  ;;      :Code "NoSuchKey"},
  ;;     :ErrorAttrs {},
  ;;     :cognitect.aws.http/status 404,
  ;;     :cognitect.anomalies/category :cognitect.anomalies/not-found,
  ;;     :cognitect.aws.error/code "NoSuchKey"}

  )
```

![Bill and Ted saying: excellent][excellent]
[excellent]: assets/excellent.jpg "We are not worthy of the REPL!" width=800px

## Getting all corporate and boring and stuff

We've been happily playing on the playground, but now we've created some stuff
that might be useful, specifically a function that lists a bunch of objects and
some code that moves an object from one key to another. Let's apply some
organisation to make this stuff more reusable.

First, we can move all of the utility functions out of `user.clj` into a new
`src/util.clj` file:

``` clojure
(ns util
  (:import (java.time Instant)))

(defmacro ->map [& ks]
  (assert (every? symbol? ks))
  (zipmap (map keyword ks)
          ks))

(defn lazy-concat [colls]
  (lazy-seq
   (when-first [c colls]
     (lazy-cat c (lazy-concat (rest colls))))))

(defn log [msg data]
  (prn {:msg msg
        :data data
        :timestamp (str (Instant/now))}))

(defn error [msg data]
  (log msg data)
  (throw (ex-info msg data)))
```

And then the S3-specific stuff goes in a new `src/s3.clj` file:

``` clojure
(ns s3
  (:require [com.grzm.awyeah.client.api :as aws]
            [clojure.string :as str]
            [util :refer [log error lazy-concat ->map]]))

(defn validate-aws-response [res]
  (when (:cognitect.anomalies/category res)
    (let [data (merge (select-keys res [:cognitect.anomalies/category])
                      {:err-msg (:Message res)
                       :err-type (:__type res)})]
      (error "AWS request failed" data)))
  res)

(defn mk-s3-req
  ([s3-bucket prefix s3-page-size]
   (mk-s3-req s3-bucket prefix s3-page-size nil))
  ([s3-bucket prefix s3-page-size continuation-token]
   (merge {:Bucket s3-bucket
           :Prefix prefix}
          (when s3-page-size
            {:MaxKeys s3-page-size})
          (when continuation-token
            {:ContinuationToken continuation-token}))))

(defn get-s3-page [{:keys [s3-client s3-bucket s3-page-size]}
                   prefix
                   {continuation-token :NextContinuationToken
                    truncated? :IsTruncated
                    page-num :page-num
                    :as prev}]
  (when prev (log "Got page" (dissoc prev :Contents)))
  (let [page-num (inc (or page-num 0))
        done? (false? truncated?)
        request (mk-s3-req s3-bucket prefix s3-page-size continuation-token)
        response (when-not done?
                   (log (format "Requesting page %d" page-num) request)
                   (-> (aws/invoke s3-client {:op :ListObjectsV2
                                              :request request})
                       validate-aws-response
                       (assoc :page-num page-num)))]
    response))

(defn list-objects [{:keys [s3-bucket limit] :as logs-client} prefix]
  (log "Listing S3 objects" (merge (->map s3-bucket prefix)
                                   (when limit {:limit limit})))
  (let [apply-limit (if limit (partial take limit) identity)]
    (->> (iteration (partial get-s3-page logs-client prefix)
                    :vf :Contents)
         lazy-concat
         apply-limit
         (map :Key))))

(defn mk-client [{:keys [aws-region] :as cfg}]
  (assoc cfg :s3-client
         (aws/client {:api :s3, :region aws-region})))
```

With this plumbing, let's write a function that actually moves an object:

``` clojure
(defn move-object [{:keys [s3-client s3-bucket] :as ctx} source-key target-key]
  (let [obj (aws/invoke s3-client {:op :GetObject
                                   :request {:Bucket s3-bucket
                                             :Key source-key}})]
    (aws/invoke s3-client {:op :PutObject
                           :request {:Bucket s3-bucket
                                     :Key target-key
                                     :Body (:Body obj)}})
    (aws/invoke (:s3-client ctx) {:op :DeleteObject
                                  :request {:Bucket s3-bucket
                                            :Key source-key}})))
```

And now back in `user.clj`, let's try it all out:

``` clojure
(comment

  (let [source-key (->> (s3/list-objects ctx "")
                        (filter #(and (str/ends-with? % ".json")
                                      (not (str/includes? % "/"))))
                        first)
        target-key (format "podcasts/conversations-with-kim-crayton/%s" source-key)]
    (s3/move-object ctx source-key target-key))
  ;; => {:DeleteMarker true, :VersionId "B6.FetnSH9qYS7FwCJOuY0NxiPL1ur7b"}

  )
```

## Wrapping it all up in a neat little package

Having built this beautiful little playground, I now have a REPL just lying
around that I can try stuff out in, automate little things that I would normally
do with a Bash "one-liner" that quickly grows into a Bash "100-liner", and if
and when I discover useful little functions, move them into namespaces under
`src/`, ready to be copied and pasted into real programs.
