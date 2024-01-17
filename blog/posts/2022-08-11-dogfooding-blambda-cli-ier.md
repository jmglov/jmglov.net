Title: Dogfooding Blambda 4: CLI, CLIier, CLIiest
Date: 2022-08-11
Tags: clojure,aws,blambda,babashka,lambda
Discuss: https://clojurians.slack.com/archives/C04PSBFMMDJ/

In [yesterday's installment of Dogfooding
Blambda](2022-08-10-dogfooding-blambda-cli.html), I fully intended to show y'all
[Blambda](https://github.com/jmglov/blambda)'s command line interface, then walk
you through how I implemented it, using the amazing
[babashka.cli](https://github.com/babashka/cli) library. However, me being me, I
kinda meandered all over the place, and by the time I had finished the "show"
portion of Show and Tell, my dog let me know, kindly but firmly as only a dog
can, that the "tell" portion would need to wait, because he couldn't anymore.

But hey, today is a new day, and I have heaps of time, so let's dig in!

After the [less than wonderful experience I
had](2022-08-09-dogfooding-blambda-2.html) using Blambda in my
[s3-log-parser](https://github.com/jmglov/s3-log-parser) lambda, I decided that
Blambda should be a library that exposed its functionality via an API, similar
to how [quickblog](https://github.com/borkdude/quickblog) works. Thinking about
what the API should look like, I decided on the following functions:
- `build-runtime-layer`: builds the Blambda custom runtime as a Lambda layer
- `deploy-runtime-layer`: deploys the custom runtime layer
- `build-deps-layer`: builds a layer for lambda dependencies
- `deploy-deps-layer`: deploys the dependencies layer
- `clean`: sweeps up around the place to give you that peaceful happy feeling
  that you so crave

I had the code for building and deploying the custom runtime layer in Blamba's
[`bb.edn`](https://github.com/jmglov/blambda/blob/c253bf7d2b0bbbe3e53ad276b1c15c53b98d2088/bb.edn),
as well as the code for building the deps layer, but in my infinite wisdom, the
code for actually deploying the deps layer was in s3-log-parser, in the
wonderfully named
[`task_helper.clj`](https://github.com/jmglov/s3-log-parser/blob/dcbb6546cc5e0d61a1374feeff78c0f58aa2d3c3/task_helper.clj)
file.

It was straightforward enough to move the code for building and deploying into a
[`blambda.api`](https://github.com/jmglov/blambda/blob/2453e15cf75c03b2b02de5ca89c76081bba40251/src/blambda/api.clj)
namespace, and I had at least had the foresight to write a primitive argument
parser in Blambda's own
[`task_helper.clj`](https://github.com/jmglov/blambda/blob/c253bf7d2b0bbbe3e53ad276b1c15c53b98d2088/task_helper.clj#L41)
that turned something like `bb build-runtime-layer --bb-arch arm64 --bb-version
0.9.161` into:

``` clojure
{:bb-arch "arm64"
 :bb-version "0.9.161"}
```

Since this is also how babashka.cli works, the migration was fairly
straightforward. I took my old `task-helper/defaults`:

``` clojure
{:aws-region {:doc "AWS region"
              :default (or (System/getenv "AWS_DEFAULT_REGION") "eu-west-1")}
:bb-version {:doc "Babashka version"
             :default "0.9.161"}
:bb-arch {:doc "Architecture to target"
          :default "amd64"
          :values ["amd64" "arm64"]}
:deps-path {:doc "Path to bb.edn or deps.edn containing lambda deps"}
:layer-name {:doc "Name of custom runtime layer in AWS"
             :default "blambda"}
:target-dir {:doc "Build output directory"
             :default "target"}
:work-dir {:doc "Working directory"
           :default ".work"}}
```

and turned them into a babashka.cli
[spec](https://github.com/babashka/cli#spec):

``` clojure
{:aws-region {:desc "AWS region"
              :ref "<region>"
              :default (or (System/getenv "AWS_DEFAULT_REGION") "eu-west-1")}
 :bb-version {:desc "Babashka version"
              :ref "<version>"
              :default "0.9.161"}
 :bb-arch {:desc "Architecture to target"
           :ref "<arch>"
           :default "amd64"
           :values ["amd64" "arm64"]}
 :deps-path {:desc "Path to bb.edn or deps.edn containing lambda dependencies"
             :ref "<path>"}
 :deps-layer-name {:desc "Name of dependencies layer in AWS"
                   :ref "<name>"}
 :runtime-layer-name {:desc "Name of custom runtime layer in AWS"
                      :ref "<name>"
                      :default "blambda"}
 :target-dir {:desc "Build output directory"
              :ref "<dir>"
              :default "target"}
 :work-dir {:desc "Working directory"
            :ref "<dir>"
            :default ".work"}}
```

With this, I could create a nice CLI for Blambda:

``` clojure
(ns blambda.cli
  (:require [babashka.cli :as cli]))

(def spec
  {
  ;; ...
  })

(defn parse-opts [& _]
  (let [opts (cli/parse-opts *command-line-args* {:spec spec})]
    (if (:help opts)
      (do
        (println (cli/format-opts {:spec spec}))
        (System/exit 0))
      opts)))
```

This is all it takes to get a lovely usage message:

``` text
$ bb -m blambda.cli/parse-opts --help
  --aws-region         <region>  eu-west-1 AWS region
  --bb-version         <version> 0.9.161   Babashka version
  --bb-arch            <arch>    amd64     Architecture to target
  --deps-path          <path>              Path to bb.edn or deps.edn containing lambda dependencies
  --deps-layer-name    <name>              Name of dependencies layer in AWS
  --runtime-layer-name <name>    blambda   Name of custom runtime layer in AWS
  --target-dir         <dir>     target    Build output directory
  --work-dir           <dir>     .work     Working directory
```

So that's Blambda as a library. Now I can use that in s3-log-parser to manage
all of the moving parts in one place. In order to do this, all I need to do is
add tasks to s3-log-parser's `bb.edn`:

``` clojure
{:paths ["."]
 :deps {net.jmglov/blambda
        #_"You use the newest SHA here:"
        {:git/url "https://github.com/jmglov/blambda.git"
         :git/sha "e379410bb6b20bb9cf34acd42cfc65e5f4fd6845"}}
 :tasks
 {:requires ([blambda.api :as blambda])

  build-runtime-layer {:doc "Builds Blambda custom runtime layer"
                       :task (blambda/build-runtime-layer)}

  deploy-runtime-layer {:doc "Deploys Blambda custom runtime layer"
                        :task (blambda/deploy-runtime-layer)}

  build-deps-layer {:doc "Builds dependencies layer"
                    :task (blambda/build-deps-layer)}

  deploy-deps-layer {:doc "Deploys dependencies layer"
                     :task (blambda/deploy-deps-layer)}

  clean {:doc "Deletes target and work directories"
         :task (blambda/clean)}}}
```

Now I can do things like build my dependencies layer:

``` text
$ bb build-deps-layer --deps-path src/bb.edn 
Classpath before transforming: src:~/s3-log-parser/.work/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Classpath after transforming: src:/opt/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Compressing custom runtime layer: ~/s3-log-parser/target/deps.zip
```

It's a little annoying that I have to add one task per library function, and
even more annoying that I have to repeat information that's already there in
`blambda.api`:

``` clojure
(defn build-runtime-layer
  "Builds Blambda custom runtime layer"
  ;; ...
  )

(defn deploy-runtime-layer
  "Deploys Blambda custom runtime layer"
  ;; ...
  )

(defn build-deps-layer
  "Builds dependencies layer"
  ;; ...
  )

(defn deploy-deps-layer
  "Deploys dependencies layer"
  ;; ...
  )

(defn clean
  "Deletes target and work directories"
  ;; ...
  )
```

What I'd like to do is add a single task that delegates all of this stuff to the
Blambda CLI. But how to find this holiest of all holy grails?

Of course the mighty [borkdude](https://github.com/borkdude) has already thought
of this, and babashka.cli has support for
[subcommands](https://github.com/babashka/cli#subcommands). If I expose each
Blambda API function as a subcommand, I can interact with Blambda from
s3-log-parser as I showed you yesterday:

``` text
$ bb blambda build-runtime-layer --bb-arch arm64
Downloading https://github.com/babashka/babashka/releases/download/v0.9.161/babashka-0.9.161-linux-aarch64-static.tar.gz
Decompressing .work/babashka-0.9.161-linux-aarch64-static.tar.gz to .work
Adding file bootstrap
Adding file bootstrap.clj
Compressing custom runtime layer: ~/my-lambda/target/bb.zip

$ bb blambda deploy-runtime-layer --bb-arch arm64
Publishing layer version for layer blambda
Published layer arn:aws:lambda:eu-west-1:289341159200:layer:blambda:1
```

And all this by adding a single line to my `bb.edn` (actually two lines, since I
need to require `blambda.cli`, or rather three lines, since I want to nicely
format the map, but you know what I mean):

``` clojure
:tasks
{:requires ([blambda.cli :as blambda])

 blambda {:doc "Controls Blambda runtime and layers"
          :task (blambda/dispatch)}}
```

Wow, now that's some amazing UX! 🏆

But how does this subcommand stuff work? babashka.cli's documentation gives this
example:

``` clojure
(ns example
  (:require [babashka.cli :as cli]))

(defn copy [m]
  (assoc m :fn :copy))

(defn delete [m]
  (assoc m :fn :delete))

(defn help [m]
  (assoc m :fn :help))

(def table
  [{:cmds ["copy"]   :fn copy   :args->opts [:file]}
   {:cmds ["delete"] :fn delete :args->opts [:file]}
   {:cmds []         :fn help}])

(defn -main [& args]
  (cli/dispatch table args {:coerce {:depth :long}}))
```

So it looks like I need to build a table that looks something like this:

``` clojure
(def table
  [{:cmds ["build-runtime-layer"]  :fn api/build-runtime-layer}
   {:cmds ["deploy-runtime-layer"] :fn api/deploy-runtime-layer}
   {:cmds ["build-deps-layer"]     :fn api/build-deps-layer}
   {:cmds ["deploy-deps-layer"]    :fn api/deploy-deps-layer}
   {:cmds ["clean"]                :fn api/build-runtime-layer}
   {:cmds []                       :fn print-help}])
```

And according to the docs, I can include other `babashka.cli/parse-arg` options
in a table entry, so I'll add `:spec spec` to each.

Now I can run `bb blambda --help` from s3-log-parser to get a nice usage
message:

``` text
  --aws-region         <region>  eu-west-1 AWS region
  --bb-version         <version> 0.9.161   Babashka version
  --bb-arch            <arch>    amd64     Architecture to target
  --deps-path          <path>              Path to bb.edn or deps.edn containing lambda dependencies
  --deps-layer-name    <name>              Name of dependencies layer in AWS
  --runtime-layer-name <name>    blambda   Name of custom runtime layer in AWS
  --target-dir         <dir>     target    Build output directory
  --work-dir           <dir>     .work     Working directory
```

Well, nice-ish. If I run this, I have no idea what subcommands `bb blambda` has,
what they do, and which of these options apply to each command. Let's see if we
can fix this.

Let's start by enriching our `print-help` function a bit and modifying our table
to use it:

``` clojure
(defn print-help [cmds]
  (println
   (format
    "Usage: bb blambda <subcommand> <options>

Subcommands:

%s"
    (->> cmds
         (map (comp first :cmds))
         (str/join "\n\n")))))

(def table
  (let [cmds
        [{:cmds ["build-runtime-layer"]  :fn api/build-runtime-layer}
         {:cmds ["deploy-runtime-layer"] :fn api/deploy-runtime-layer}
         {:cmds ["build-deps-layer"]     :fn api/build-deps-layer}
         {:cmds ["deploy-deps-layer"]    :fn api/deploy-deps-layer}
         {:cmds ["clean"]                :fn api/build-runtime-layer}]]
    (conj cmds
          {:cmds [] :fn (fn [_] (print-help cmds))})))
```

Now running `bb blambda --help` (or `bb blambda help` or `bb blambda`, for that
matter) produces something a bit nicer:

``` text
Usage: bb blambda <subcommand> <options>

Subcommands:

build-runtime-layer

deploy-runtime-layer

build-deps-layer

deploy-deps-layer

clean
```

Let's add a description to each subcommand and then update `print-help`:

``` clojure
(def table
  (let [cmds
        [{:cmds ["build-runtime-layer"]
          :desc "Builds Blambda custom runtime layer"
          :fn api/build-runtime-layer}
         {:cmds ["deploy-runtime-layer"]
          :desc "Deploys Blambda custom runtime layer"
          :fn api/deploy-runtime-layer}
         ;; ...
        ]]
    (conj cmds
          {:cmds [] :fn (fn [_] (print-help cmds))})))

(defn print-help' [cmds]
  (println
   (format
    "Usage: bb blambda <subcommand> <options>

Subcommands:

%s"
    (->> cmds
         (map (fn [{:keys [cmds desc]}]
                (format "%s: %s" (first cmds) desc)))
         (str/join "\n\n")))))
```

Now we're getting somewhere!

``` text
Usage: bb blambda <subcommand> <options>

Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer

deploy-runtime-layer: Deploys Blambda custom runtime layer

build-deps-layer: Builds dependencies layer from bb.edn or deps.edn

deploy-deps-layer: Deploys dependencies layer

clean: Removes work and target folders
```

Now it's time to bring back our options. If we look at the options we have,
`--target-dir` and `--work-dir` apply to every command, `--aws-region` applies
to the two deployment commands, and the rest of the options (`--bb-arch`,
`--bb-version`, `--deps-path`, `--runtime-layer-name`, and `--deps-layer-name`)
apply only to specific commands. Let's see how we can express this in our
command table:

``` clojure
(def table
  (let [cmds
        [{:cmds ["build-runtime-layer"]
          :desc "Builds Blambda custom runtime layer"
          :fn api/build-runtime-layer
          :opts #{:bb-version :bb-arch}}
         {:cmds ["build-deps-layer"]
          :desc "Builds dependencies layer from bb.edn or deps.edn"
          :fn api/build-deps-layer
          :opts #{:deps-path}}
         {:cmds ["deploy-runtime-layer"]
          :desc "Deploys Blambda custom runtime layer"
          :fn api/deploy-runtime-layer
          :opts #{:aws-region :bb-arch :runtime-layer-name}}
         {:cmds ["deploy-deps-layer"]
          :desc "Deploys dependencies layer"
          :fn api/deploy-deps-layer
          :opts #{:aws-region :bb-arch :deps-layer-name}}
         {:cmds ["clean"]
          :desc "Removes work and target folders"
          :fn api/clean}]]
    (conj cmds
          {:cmds [], :fn (fn [m] (print-help cmds))})))
```

This makes sense, but now we need a way to turn a set of opts into a spec.
Whilst we're at it, let's add the global options (`--target-dir` and
`--work-dir`) to every spec:

``` clojure
(def specs
  {:aws-region
   {:desc "AWS region"
    :ref "<region>"
    :default (or (System/getenv "AWS_DEFAULT_REGION") "eu-west-1")}
   ;; ...
  })

(def global-opts #{:target-dir :work-dir})

(defn mk-spec [default-opts opts]
  (select-keys specs (set/union global-opts opts)))
```

Now we can plug `mk-spec` into our table:

``` clojure
(def table
  (let [cmds
        [{:cmds ["build-runtime-layer"]
          :desc "Builds Blambda custom runtime layer"
          :fn api/build-runtime-layer
          :spec (mk-spec #{:bb-version :bb-arch})}
         {:cmds ["build-deps-layer"]
          :desc "Builds dependencies layer from bb.edn or deps.edn"
          :fn api/build-deps-layer
          :spec (mk-spec #{:deps-path})}
         {:cmds ["deploy-runtime-layer"]
          :desc "Deploys Blambda custom runtime layer"
          :fn api/deploy-runtime-layer
          :spec (mk-spec #{:aws-region :bb-arch :runtime-layer-name})}
         {:cmds ["deploy-deps-layer"]
          :desc "Deploys dependencies layer"
          :fn api/deploy-deps-layer
          :spec (mk-spec #{:aws-region :deps-layer-name})}
         {:cmds ["clean"]
          :desc "Removes work and target folders"
          :fn api/clean}]]
    (conj cmds
          {:cmds [], :fn (fn [m] (print-help cmds))})))
```

And since we have the spec for each subcommand, we can use that in our help
message:

``` clojure
(defn ->subcommand-help [{:keys [cmd desc spec]}]
  (format "%s: %s\n%s" cmd desc
          (cli/format-opts {:spec spec})))

(defn print-help [cmds]
  (println
   (format
    "Usage: bb blambda <subcommand> <options>

All subcommands support the options:

%s

Subcommands:

%s"
    (cli/format-opts {:spec (select-keys specs global-opts)})
    (->> cmds
         (map ->subcommand-help)
         (str/join "\n\n"))))
  (System/exit 0))
```

Running `bb blambda` now is very satisfying:

``` text
Usage: bb blambda <subcommand> <options>

All subcommands support the options:

  --work-dir   <dir> .work  Working directory
  --target-dir <dir> target Build output directory

Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer
  --bb-arch    <arch>    amd64   Architecture to target (use amd64 if you don't care)
  --work-dir   <dir>     .work   Working directory
  --target-dir <dir>     target  Build output directory
  --bb-version <version> 0.9.161 Babashka version

build-deps-layer: Builds dependencies layer from bb.edn or deps.edn
  --work-dir   <dir>  .work  Working directory
  --deps-path  <path>        Path to bb.edn or deps.edn containing lambda deps
  --target-dir <dir>  target Build output directory

deploy-runtime-layer: Deploys Blambda custom runtime layer
  --bb-arch            <arch>   amd64     Architecture to target (use amd64 if you don't care)
  --runtime-layer-name <name>   blambda   Name of custom runtime layer in AWS
  --work-dir           <dir>    .work     Working directory
  --aws-region         <region> eu-west-1 AWS region
  --target-dir         <dir>    target    Build output directory

deploy-deps-layer: Deploys dependencies layer
  --work-dir        <dir>    .work     Working directory
  --aws-region      <region> eu-west-1 AWS region
  --target-dir      <dir>    target    Build output directory
  --deps-layer-name <name>             Name of dependencies layer in AWS

clean: Removes work and target folders
```

There's one tiny annoyance, though. The usage message says that all subcommands
support `--work-dir` and `--target-dir`, but then those options are repeated for
every subcommand, which is a bit unnecessary and distracting. What we need to do
is suppress the global options in `->subcommand-help`:

``` clojure
(defn ->subcommand-help [{:keys [cmd desc spec]}]
  (let [spec (apply dissoc spec global-opts)]
    (format "%s: %s\n%s" cmd desc
            (cli/format-opts {:spec spec}))))
```

`dissoc` is normally used to remove one key from a map:

``` clojure
(dissoc {:a 1, :b 2, :c 3} :a)  ;; => {:b 2, :c 3}
```

but you can also give it more keys:

``` clojure
(dissoc {:a 1, :b 2, :c 3} :a :b)  ;; => {:c 3}
```

We have a set, `global-opts`, which is seqable, so we can use `apply` to splat
it onto the end of the list of arguments to `dissoc`:

``` clojure
(let [spec (apply dissoc spec global-opts)]
  ;; Now spec has all of the opts except the global ones
  )
```

Let's see what we've accomplished:

``` text
$ bb blambda
Usage: bb blambda <subcommand> <options>

All subcommands support the options:

  --work-dir   <dir> .work  Working directory
  --target-dir <dir> target Build output directory

Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer
  --bb-arch    <arch>    amd64   Architecture to target (use amd64 if you don't care)
  --bb-version <version> 0.9.161 Babashka version

build-deps-layer: Builds dependencies layer from bb.edn or deps.edn
  --deps-path <path> Path to bb.edn or deps.edn containing lambda deps

deploy-runtime-layer: Deploys Blambda custom runtime layer
  --bb-arch            <arch>   amd64     Architecture to target (use amd64 if you don't care)
  --runtime-layer-name <name>   blambda   Name of custom runtime layer in AWS
  --aws-region         <region> eu-west-1 AWS region

deploy-deps-layer: Deploys dependencies layer
  --aws-region      <region> eu-west-1 AWS region
  --deps-layer-name <name>             Name of dependencies layer in AWS

clean: Removes work and target folders
```

Excellent!

We're still missing one thing that I showed off yesterday, though: the ability
for a client to override Blambda's defaults. In the case of s3-log-parser, I
want to make sure I'm building Blambda for the ARM64 architecture, and set my
deps path and deps layer name so that I don't have to remember to type the args
every time.

Let's start out by wishing the feature into existence in s3-log-parser's
`bb.edn`:

``` clojure
:tasks
{:requires ([blambda.cli :as blambda])

 blambda {:doc "Controls Blambda runtime and layers"
          :task (blambda/dispatch
                 {:bb-arch "arm64"
                  :deps-path "src/bb.edn"
                  :deps-layer-name "s3-log-parser-deps"})}}
```

So we want the client to be able to pass defaults to `blambda.cli/dispatch`.
Let's make it happen:

``` clojure
(defn dispatch
  ([]
   (dispatch {}))
  ([default-opts & args]
   (cli/dispatch (mk-table default-opts)
                 (or args
                     (seq *command-line-args*)))))
```

Because we're good Clojurists, we maintain backwards compatibility by keeping
the 0-arity version of `dispatch`, and just have it send an empty `default-opts`
map into the new 1-arity version.

We're not going to be able to keep our static version of `table` anymore either,
so we'll wrap it in a function called `mk-table` that incorporates our defaults:

``` clojure
(defn mk-table [default-opts]
  (let [cmds
        [{:cmds ["build-runtime-layer"]
          :desc "Builds Blambda custom runtime layer"
          :fn api/build-runtime-layer
          :spec (mk-spec default-opts #{:bb-version :bb-arch})}
         ;; ...
         ]]
    (conj cmds
          {:cmds [], :fn (fn [m] (print-help default-opts cmds))})))
```

We need to pass the `default-opts` along to `mk-spec` and `print-help` as well:

``` clojure
(defn mk-spec [default-opts opts]
  (->> (select-keys specs (set/union global-opts opts))
       (apply-defaults default-opts)))

(defn ->subcommand-help [default-opts {:keys [cmd desc spec]}]
  (let [spec (apply dissoc spec global-opts)]
    (format "%s: %s\n%s" cmd desc
            (cli/format-opts {:spec
                              (apply-defaults default-opts spec)}))))

(defn print-help [default-opts cmds]
  (println
   (format
    "Usage: bb blambda <subcommand> <options> ..."
    (cli/format-opts {:spec (select-keys specs global-opts)})
    (->> cmds
         (map (partial ->subcommand-help default-opts))
         (str/join "\n\n"))))
  (System/exit 0))
```

And finally, let's look at this mysterious new `apply-defaults` function:

``` clojure
(defn apply-defaults [default-opts spec]
  (->> spec
       (map (fn [[k v]]
              (if-let [default-val (default-opts k)]
                [k (assoc v :default default-val)]
                [k v])))
       (into {})))
```

If we run `bb blambda help` now, we can see the effects of our changes:

``` text
Usage: bb blambda <subcommand> <options>
[...]
Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer
  --bb-arch    <arch>    arm64   Architecture to target (use amd64 if you don't care)
  --bb-version <version> 0.9.161 Babashka version

build-deps-layer: Builds dependencies layer from bb.edn or deps.edn
  --deps-path <path> src/bb.edn Path to bb.edn or deps.edn containing lambda deps

deploy-deps-layer: Deploys dependencies layer
  --aws-region      <region> eu-west-1          AWS region
  --deps-layer-name <name>   s3-log-parser-deps Name of dependencies layer in AWS
```

Note that `--bb-arch` now defaults to `arm64`, and `--deps-path` and
`--deps-layer-name` now have default values, which they didn't before! 🎉

I realise I'm quite a few words into this post now, but I do want to add one
more tiny feature. With a subcommand setup, you expect `bb blambda
build-runtime-layer --help` to give you help on the `build-runtime-layer`
subcommand, but at the moment, our code just ignores the `--help` flag and calls
the `api/build-runtime-layer` function, which is definitely not what we want.

In order to support this, let's wrap the `api/build-runtime-layer` in a function
that checks for `--help` and does the right thing:

``` clojure
(defn mk-table [default-opts]
  (let [cmds
        [{:cmds ["build-runtime-layer"]
          :desc "Builds Blambda custom runtime layer"
          :fn (fn [opts]
                (when (:help opts)
                  (print-command-help cmd spec)
                  (System/exit 0))
                (api/build-runtime-layer))
          :spec (mk-spec default-opts #{:bb-version :bb-arch})}
         ;; ...
         ]]
    (conj cmds
          {:cmds [], :fn (fn [m] (print-help default-opts cmds))})))
```

We can define `print-command-help` as follows:

``` clojure
(defn print-command-help [cmd spec]
  (println
   (format "Usage: bb blambda %s <options>\n\nOptions:\n%s"
           cmd (cli/format-opts {:spec spec}))))
```

Now things work as expected:

``` text
$ bb blambda build-runtime-layer --help
Usage: bb blambda build-runtime-layer <options>

Options:
  --bb-arch    <arch>    arm64   Architecture to target (use amd64 if you don't care)
  --work-dir   <dir>     .work   Working directory
  --target-dir <dir>     target  Build output directory
  --bb-version <version> 0.9.161 Babashka version
```

Of course, now we're in the somewhat unpleasant situation of having to copy and
paste our wrapper for all of the other subcommands, so let's create a function
to do this for us:

``` clojure
(defn mk-cmd [default-opts {:keys [cmd spec] :as cmd-opts}]
  (merge
   cmd-opts
   {:cmds [cmd]
    :fn (fn [{:keys [opts]}]
          (when (:help opts)
            (print-command-help cmd spec)
            (System/exit 0))
          ((:fn cmd-opts) opts))}))
```

Now we can use this function in `mk-table`:

``` clojure
(defn mk-table [default-opts]
  (let [cmds
        [{:cmd "build-runtime-layer"
          :desc "Builds Blambda custom runtime layer"
          :fn api/build-runtime-layer
          :spec (mk-spec default-opts #{:bb-version :bb-arch})}
         ;; ...
         ]]
    (conj (mapv (partial mk-cmd default-opts) cmds)
          {:cmds [], :fn (fn [m] (print-help default-opts cmds))})))
```

So `--help` now works for all subcommands. And since we now have `mk-cmd`
wrapping our subcommand function for us, let's also ensure that all options are
set (we have no optional options here):

``` clojure
(defn mk-cmd [default-opts {:keys [cmd spec] :as cmd-opts}]
  (merge
   cmd-opts
   {:cmds [cmd]
    :fn (fn [{:keys [opts]}]
          (let [missing-args (->> (set (keys opts))
                                  (set/difference (set (keys spec)))
                                  (map #(format "--%s" (name %)))
                                  (str/join ", "))]
            (when (:help opts)
              (print-command-help cmd spec)
              (System/exit 0))
            (when-not (empty? missing-args)
              (error {:cmd cmd, :spec spec}
                     (format "Missing required arguments: %s" missing-args)))
            ((:fn cmd-opts) opts)))}))
```

The `error` function just formats a nice error message and exits:

``` clojure
(defn error [{:keys [cmd spec]} msg]
  (println (format "%s\n" msg))
  (print-command-help cmd spec)
  (System/exit 1))
```

We can test this by commenting out the `:deps-path` key-value pair in
s3-log-parser's `bb.edn`:

``` clojure
:tasks
{:requires ([blambda.cli :as blambda])

 blambda {:doc "Controls Blambda runtime and layers"
          :task (blambda/dispatch
                 {:bb-arch "arm64"
;;                  :deps-path "src/bb.edn"
                  :deps-layer-name "s3-log-parser-deps"})}}
```

and then not mentioning `--deps-path` on the command line:

``` text
$ bb blambda build-deps-layer
Missing required arguments: --deps-path

Usage: bb blambda build-deps-layer <options>

Options:
  --work-dir   <dir>  .work  Working directory
  --deps-path  <path>        Path to bb.edn or deps.edn containing lambda deps
  --target-dir <dir>  target Build output directory
```

With that, we have achieved a great victory and can now move onto another
activity (in my case, sleeping).
