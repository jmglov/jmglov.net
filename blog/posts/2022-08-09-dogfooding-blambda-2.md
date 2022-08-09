Title: Dogfooding Blambda : I heard you liked layers
Tags: clojure,aws,blambda,s3,babashka,lambda
Date: 2022-08-09

After a brief detour to port my blog to
[quickblog](https://github.com/borkdude/quickblog) and enjoy some sun in Málaga,
I'm back to eating yummy dogfood by trying to use
[Blambda](https://github.com/jmglov/blambda) to parse my HTTP access logs:
[jmglov/s3-log-parser](https://github.com/jmglov/s3-log-parser).

I didn't realise it until I started writing this post, but it has actually been
a month since I last used Blambda; I'm sure you all remember the wacky hijinks
that ensued when [I tried to make Blambda work with Babashka
pods](2022-07-04-dogfooding-blambda-1.html). Well, more wacky hijinks ensued
when I came back to the project and realised that the released version of the
[babasha-aws pod](https://github.com/babashka/pod-babashka-aws) was compiled for
AMD64, and my lambda is of course ARM64 because I want to be cool. Instead of
doing what a reasonable person would do and switch to an AMD64 lambda, I decided
instead to use [awyeah-api](https://github.com/grzm/awyeah-api), which is a port
of Cognitect's [aws-api](https://github.com/cognitect-labs/aws-api) to Babashka.
You know, because it's cool to not have to rely on an external binary and
bbencode and all of that stuff. 😉

Of course, Blambda didn't actually support dependencies outside of pods, so the
first step to switching to awyeah-api was implementing regular deps in Blambda.
How hard could it be, right?

Just like I did for pods, I wanted to keep all deps in a layer so that it would
be fast to deploy new versions of the lambda itself, and deps typically don't
change very often. This should be pretty straightforward: just get Babashka to
download my deps and zip them up into a layer.

I created a `bb.edn` with all of the awyeah-api stuff:

``` clojure
{:paths ["."]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.206"}
        com.cognitect.aws/s3 {:mvn/version "822.2.1109.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "0fa7dd51f801dba615e317651efda8c597465af6"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "433b0778e2c32f4bb5d0b48e5a33520bee28b906"}}}
```

Now, running `bb tasks` in that directory downloaded all of the dependencies into
my local Maven cache, `~/.m2/repository`, so I just need to grab them and stuff
them into a zip file. Except there seem to be about a million Java libraries in
that directory, and I definitely don't want more than I actually need, since I
want to keep my layer zipfile as small as possible. So what I would like to do
is create a new directory and tell Babashka to use that as the Maven cache, so
I know that everything downloaded there belongs to my lambda. But how to tell
Babashka where I want my deps?

Searching the web wasn't much help, mainly because I couldn't find the right
magic phrase, so I turned to Clojurians Slack for help.
[borkdude](https://github.com/borkdude) himself stopped by and told me that
Babashka uses the [Clojure CLI](https://clojure.org/reference/deps_and_cli) to
resolve dependencies, and then Alex Miller (who must have a Slack alert for any
mention of `-Sdeps`) told me about the [`:mvn/local-repo`
key](https://clojure.org/reference/deps_and_cli#_dependencies), which does
exactly what I want. Excellent!

And then I ran into the next issue: in addition to Maven dependencies, I also
have Git dependencies, and these are handled differently. Going back to the
[Deps and CLI
Reference](https://clojure.org/reference/deps_and_cli#_configuration_and_debugging),
I found that one can set the `GITLIBS` environment variable to tell the CLI
where to cache Git deps.

So now I knew how to get all of my deps right where I wanted them, so it was
time to automate things with Babashka. I created a new `build-deps` task in
Blamba's `bb.edn`. The first step was to consume the lambda's `bb.edn` and add
the `:mvn/local-repo` key to it:

``` clojure
build-deps {:docs "Builds a layer for dependencies"
            :requires ([clojure.edn :as edn])
            :task (let [{:keys [deps-path work-dir]} (th/parse-args)]
                    (when-not deps-path
                      (th/error "Mising required argument: --deps-path"))
                    (fs/create-dirs work-dir)
                    (let [m2-dir "m2-repo"
                          deps (->> deps-path slurp edn/read-string :deps)]
                      (spit (fs/file work-dir "deps.edn")
                            {:deps deps
                             :mvn/local-repo (str m2-dir)})))}
```

This will create a `.work/deps.edn` file that contains all of the lambda's
dependency, plus the magic `:mvn/local-repo` key to tell `tools.deps` to use
`.work/m2-repo` for my Maven deps.

Now I need to run `clojure` somehow to download the deps into the right place.
Chatting to borkdude revealed that there's a `babashka.deps/clojure` function
that invokes the Clojure CLI. Awesome, so I can use that! I just need to not
forget to set the `GITLIBS` env var to `.work/gitlibs` to make sure the Git deps
end up in the right place.

``` clojure
build-deps {:docs "Builds a layer for dependencies"
            :requires ([clojure.edn :as edn]
                       [babashka.deps :refer [clojure]])
            :task (let [{:keys [deps-path work-dir]} (th/parse-args)]
                    ;; ...
                    (let [gitlibs-dir "gitlibs"
                          m2-dir "m2-repo"
                          deps (->> deps-path slurp edn/read-string :deps)]
                      ;; ...
                      (clojure ["-Spath"]
                               {:dir work-dir
                                :env (assoc (into {} (System/getenv))
                                            "GITLIBS" (str gitlibs-dir))})))}
```

And now that I have all the deps, I just need to zip them up:

``` clojure
build-deps {:docs "Builds a layer for dependencies"
            :requires ([clojure.java.shell :refer [sh]]
                       [clojure.edn :as edn]
                       [babashka.deps :refer [clojure]])
            :task (let [{:keys [deps-path target-dir work-dir]} (th/parse-args)
                        deps-zipfile (th/deps-zipfile target-dir)]
                    ;; ...
                    (fs/create-dirs target-dir work-dir)
                    (let [gitlibs-dir "gitlibs"
                          m2-dir "m2-repo"
                          deps (->> deps-path slurp edn/read-string :deps)]
                      ;; ...
                      (println "Compressing custom runtime layer:" deps-zipfile)
                      (let [{:keys [exit err]}
                            (sh "zip" "-r" deps-zipfile
                                (fs/file-name gitlibs-dir)
                                (fs/file-name m2-dir)
                                :dir work-dir)]
                        (when (not= 0 exit)
                          (println "Error:" err)))))}
```

This gives me a `target/deps.zip` that I can use as a layer. Lambda layers unzip
to `/opt`, so I'll need to update the Blambda runtime to add
`GITLIBS=/opt/gitlibs` to my Babashka invocation and update my lambda's `bb.edn`
to include `:mvn/local-repo "/opt/m2-repo"`, and then of course add the deps
layer to my lambda function.

Once all this was done, I took a deep breath and pressed the **Test** button in
the AWS Lambda console. And of course got an error. 😭

``` text
Exception in thread "main" java.lang.Exception: Couldn't find 'java'.
Please set JAVA_HOME.
  at borkdude.deps$_main.invokeStatic(deps.clj:436)
  at borkdude.deps$_main.doInvoke(deps.clj:425)
  at clojure.lang.RestFn.applyTo(RestFn.java:137)
  at clojure.core$apply.invokeStatic(core.clj:667)
  at babashka.impl.deps$add_deps$fn__26630$fn__26631.invoke(deps.clj:92)
  at babashka.impl.deps$add_deps$fn__26630.invoke(deps.clj:92)
  at babashka.impl.deps$add_deps.invokeStatic(deps.clj:92)
  at babashka.main$exec.invokeStatic(main.clj:789)
  at babashka.main$main.invokeStatic(main.clj:1052)
  at babashka.main$main.doInvoke(main.clj:1027)
  at clojure.lang.RestFn.applyTo(RestFn.java:137)
  at clojure.core$apply.invokeStatic(core.clj:667)
  at babashka.main$_main.invokeStatic(main.clj:1085)
  at babashka.main$_main.doInvoke(main.clj:1077)
  at clojure.lang.RestFn.applyTo(RestFn.java:137)
  at babashka.main.main(Unknown Source)
```

Yikes! I guess this makes sense, though. Babashka is using the Clojure CLI to
resolve dependencies, and the Clojure CLI needs a JVM. Unfortunately, this won't
work for me, because the lambda container doesn't have Java installed.

At this point, having learned a lot about what's going on under the hood in
Babashka, I decided to cheat and see how [Holy
Lambda](https://github.com/FieryCod/holy-lambda) solved this problem for the
Babashka backend. Here's what [HL's bootstrap
script](https://github.com/FieryCod/holy-lambda/blob/master/modules/holy-lambda-babashka-layer/bootstrap)
looks like:

``` bash
#!/bin/sh

set -e

export BABASHKA_DISABLE_SIGNAL_HANDLERS="true"

export XDG_CACHE_HOME=/opt
export XDG_CONFIG_HOME=/opt
export XDG_DATA_HOME=/opt
export HOME=/var/task
export GITLIBS=/opt
export CLOJURE_TOOLS_DIR=/opt
export CLJ_CACHE=/opt
export CLJ_CONFIG=/opt

export BABASHKA_CLASSPATH="/opt/.m2:var/task/src:/var/task/.m2:/var/task:/var/task/src/clj:/var/task/src/cljc:src/cljc:src/clj:/var/task/resources"
export BABASHKA_PRELOADS='(load-file "/opt/hacks.clj")'

if [ -z "$HL_ENTRYPOINT" ]; then
  echo "Environment variable \"HL_ENTRYPOINT\" is not set. See https://fierycod.github.io/holy-lambda/#/babashka-backend-tutorial"
fi;

/opt/bb -Duser.home=/var/task -m "$HL_ENTRYPOINT"
```

Aha! HL sets the Babashka classpath explicitly so that Babashka won't need to do
any resolution, and furthermore, that exciting looking
[`hacks.clj`](https://github.com/FieryCod/holy-lambda/blob/master/modules/holy-lambda-babashka-layer/hacks.clj)
actually disables dependency resolution altogether:

``` clojure
(require '[babashka.deps])

(alter-var-root
 #'babashka.deps/add-deps
 (fn [f]
   (fn [m]
     (println "[holy-lambda] Dependencies should not be added via add-deps. Move your dependencies to a layer!")
     (System/exit 1))))
```

Sneaky but awesome! I'll have to copy this later, but for now, let me see if I
can just make things work.

I'll take inspiration from Holy Lambda and set the classpath explicitly, so the
first thing to do is figure out what the classpath should be. Luckily, I know
how to calculate a classpath: `clj -Spath`. In fact, my `build-deps` code is
already using for its side effect of downloading dependencies, so all I have to
do is capture its output. I already noticed that `babashka.deps/clojure` is
printing the classpath to standard output, so if I wrap this in a
[`with-out-str`](https://clojuredocs.org/clojure.core/with-out-str), I'm all
good:

``` clojure
(let [classpath
      (with-out-str
        (clojure ["-Spath"]
                 {:dir work-dir
                  :env (assoc (into {} (System/getenv))
                              "GITLIBS" (str gitlibs-dir))}))]
  (println "Classpath:" classpath)
  ;; ...
)
```

Running `bb build-deps --deps-path ../s3-log-parser/src/bb.edn`, I see:

``` text
Classpath: src:~/.work/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...
```

and so on. Cool! Now I have a classpath. The only problem with this is that it
is an absolute path to my `.work` directory, and my dependencies are going to
end up in `/opt` when I add my deps layer to my lambda. So I'll need to rewrite
the classpath to say `/opt` everywhere it currently says `~/.work`. No worries,
Clojure can do that:

``` clojure
(let [deps-base-dir (str (fs/path (fs/cwd) work-dir))
      classpath
      (with-out-str
        (clojure ["-Spath"]
                 {:dir work-dir
                  :env (assoc (into {} (System/getenv))
                              "GITLIBS" (str gitlibs-dir))}))
      deps-classpath (str/replace classpath deps-base-dir "/opt")]
  (println "Classpath before transforming:" classpath)
  (println "Classpath after transforming:" deps-classpath)
  ;; ...
)
```

Running this gives me what I'm looking for:

``` text
Classpath before transforming: src:~/.work/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...
Classpath after transforming: src:/opt/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...
```

Victory! 🎉

The next step is to pass that classpath along to Blambda so that it can set it
when invoking `bb` in the custom runtime. I decided to write the classpath to a
file that can be included in the deps layer, which Blambda will read when
initialising the runtime:

``` clojure
(let [classpath-file (fs/file work-dir "deps-classpath")
      deps-base-dir (str (fs/path (fs/cwd) work-dir))
      classpath
      (with-out-str
        (clojure ["-Spath"]
                 {:dir work-dir
                  :env (assoc (into {} (System/getenv))
                              "GITLIBS" (str gitlibs-dir))}))
      deps-classpath (str/replace classpath deps-base-dir "/opt")]
  (println "Classpath before transforming:" classpath)
  (println "Classpath after transforming:" deps-classpath)
  (spit classpath-file deps-classpath)

  (println "Compressing custom runtime layer:" deps-zipfile)
  (let [{:keys [exit err]}
        (sh "zip" "-r" deps-zipfile
            (fs/file-name gitlibs-dir)
            (fs/file-name m2-dir)
            (fs/file-name classpath-file)
            :dir work-dir)]
    (when (not= 0 exit)
      (println "Error:" err))))
```

When the deps layer is unzipped on the lambda instance, I'll have a file named
`/opts/deps-classpath` containing the transformed classpath. Now all I have to
do is update Blambda's `bootstrap` script to use the file if it's there:

``` bash
#!/bin/sh

set -e

LAYERS_DIR=/opt
DEPS_CLASSPATH_FILE="$LAYERS_DIR/deps-classpath"

CLASSPATH=$LAMBDA_TASK_ROOT
if [ -e $DEPS_CLASSPATH_FILE ]; then
  CLASSPATH="$CLASSPATH:`cat $DEPS_CLASSPATH_FILE`"
fi

export BABASHKA_DISABLE_SIGNAL_HANDLERS="true"
export BABASHKA_PODS_DIR=$LAYERS_DIR/.babashka/pods
export GITLIBS=$LAYERS_DIR/gitlibs

echo "Starting Babashka:"
echo "$LAYERS_DIR/bb -cp $CLASSPATH $LAYERS_DIR/bootstrap.clj"

$LAYERS_DIR/bb -cp $CLASSPATH $LAYERS_DIR/bootstrap.clj
```

Now the classpath will always start with `$LAMBDA_TASK_ROOT`, which is the
directory where the lambda zipfile is extracted (`/var/task`), and then if there
exists an `/opt/deps-classpath` file, its contents will be appended to the
classpath.

Invoking `bb` with the `-cp` flag overrides the default classpath and stops
Babashka from building the classpath from `bb.edn`. This also means that there's
no need to include a `bb.edn` in the lambda archive, which saves precious bytes!
😉

After running `bb deploy` to deploy the new version of the Blambda custom
runtime and creating a new version of my deps layer with `target/deps.zip`, I
held my breath and clicked the **Test** button once again:

![The lambda console showing a successful test result][blambda]

Victory! 🎉

Of course, there's a lot that is pretty yucky about this. The yuckiest bit is
probably that I have to build and deploy Blambda from the Blambda repo, build
the deps layer from the Blambda repo, but deploy the deps layer and build and
deploy the lambda archive itself from the s3-log-parser repo. Gross. Will fix.

[blambda]:[assets/2022-08-07-blambda.png "Such logs!" width=800px]
