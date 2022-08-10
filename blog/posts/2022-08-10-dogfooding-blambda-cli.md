Title: Dogfooding Blambda 3: CLIify this!
Date: 2022-08-10
Tags: clojure,aws,blambda,babashka,lambda

When [last we left](2022-08-09-dogfooding-blambda-2.html) the epic tale of me
trying to use a thing that I made myself and then being outraged at how poorly
made the thing was, I had just gotten dependencies stuffed in a lambda layer and
proved that it worked by deploying a lambda that listed some files from an S3
bucket. Hurrah!

Of course, I ended that post by complaining about the incredibly bad UX of the
thing I had built, and a vague promise to make it less incredibly bad (never let
good be the enemy of less bad!). Since last we spoke, I did just that. Check it
out!

So I create a directory called `my-lambda`, then add a `bb.edn` like this:

``` clojure
{:deps {net.jmglov/blambda
        #_"You use the newest SHA here:"
        {:git/url "https://github.com/jmglov/blambda.git"
         :git/sha "2453e15cf75c03b2b02de5ca89c76081bba40251"}}
 :tasks
 {:requires ([blambda.cli :as blambda])
  blambda {:doc "Controls Blambda runtime and layers"
           :task (blambda/dispatch)}}}
```

This is enough to let me use Blambda:

``` text
$ bb blambda
Usage: bb blambda <subcommand> <options>

All subcommands support the options:

  --target-dir <dir> target Build output directory
  --work-dir   <dir> .work  Working directory

Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer
  --bb-version <version> 0.9.161 Babashka version
  --bb-arch    <arch>            Architecture to target (use amd64 if you don't care)

build-deps-layer: Builds dependencies layer from bb.edn or deps.edn
  --deps-path <path> Path to bb.edn or deps.edn containing lambda deps

deploy-runtime-layer: Deploys Blambda custom runtime layer
  --aws-region         <region> eu-west-1 AWS region
  --runtime-layer-name <name>   blambda   Name of custom runtime layer in AWS
  --bb-arch            <arch>   amd64     Architecture to target

deploy-deps-layer: Deploys dependencies layer
  --aws-region      <region> eu-west-1 AWS region
  --deps-layer-name <name>             Name of dependencies layer in AWS

clean: Removes work and target folders
```

I can create a Blambda runtime:

``` text
$ bb blambda build-runtime-layer --help
Usage: bb blambda build-runtime-layer <options>

Options:
  --target-dir <dir>     target  Build output directory
  --work-dir   <dir>     .work   Working directory
  --bb-version <version> 0.9.161 Babashka version
  --bb-arch    <arch>            Architecture to target

$ bb blambda build-runtime-layer --bb-arch arm64
Downloading https://github.com/babashka/babashka/releases/download/v0.9.161/babashka-0.9.161-linux-aarch64-static.tar.gz
Decompressing .work/babashka-0.9.161-linux-aarch64-static.tar.gz to .work
Adding file bootstrap
Adding file bootstrap.clj
Compressing custom runtime layer: ~/my-lambda/target/bb.zip
```

And deploy it:

``` text
$ bb blambda deploy-runtime-layer --bb-arch arm64
Publishing layer version for layer blambda
Published layer arn:aws:lambda:eu-west-1:289341159200:layer:blambda:1
```

Now, let's say I want my lambda to do S3 stuff using
[awyeah-api](https://github.com/grzm/awyeah-api). I'll create a `src` directory
and drop a `bb.edn` in there:

``` clojure
{:paths ["."]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.206"}
        com.cognitect.aws/s3 {:mvn/version "822.2.1109.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "0fa7dd51f801dba615e317651efda8c597465af6"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "433b0778e2c32f4bb5d0b48e5a33520bee28b906"}}}
```

I can now use Blambda to create a lambda layer containing all my dependencies:

``` text
$ bb blambda build-deps-layer
Missing required arguments: --deps-path

Usage: bb blambda build-deps-layer <options>

Options:
  --target-dir <dir>  target Build output directory
  --work-dir   <dir>  .work  Working directory
  --deps-path  <path>        Path to bb.edn or deps.edn containing lambda deps
```

Oops! Looks like I forgot the `--deps-path` argument. Let's try that again:

``` text
$ bb blambda build-deps-layer --deps-path src/bb.edn 
Cloning: https://github.com/grzm/awyeah-api
Downloading: com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.pom from central
Downloading: org/clojure/clojure/1.11.1/clojure-1.11.1.pom from central
Downloading: com/cognitect/aws/s3/822.2.1109.0/s3-822.2.1109.0.pom from central
Checking out: https://github.com/grzm/awyeah-api at 0fa7dd51f801dba615e317651efda8c597465af6
Cloning: https://github.com/babashka/spec.alpha
Checking out: https://github.com/babashka/spec.alpha at 433b0778e2c32f4bb5d0b48e5a33520bee28b906
Downloading: org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.pom from central
Downloading: org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.pom from central
Downloading: org/clojure/pom.contrib/1.1.0/pom.contrib-1.1.0.pom from central
Downloading: com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar from central
Downloading: com/cognitect/aws/s3/822.2.1109.0/s3-822.2.1109.0.jar from central
Downloading: org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.jar from central
Downloading: org/clojure/clojure/1.11.1/clojure-1.11.1.jar from central
Downloading: org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.jar from central
Classpath before transforming: src:~/my-lambda/.work/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Classpath after transforming: src:/opt/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Compressing custom runtime layer: ~/my-lambda/target/deps.zip
```

What that wall of text (sorry about that!) is saying is that Blambda is
downloading all of the dependencies my lambda has declared in `bb.edn` and
zipping them up into a layer, which I can deploy like this:

``` text
$ bb blambda deploy-deps-layer --deps-layer-name my-lambda-deps
Publishing layer version for layer my-lambda-deps
Published layer arn:aws:lambda:eu-west-1:289341159200:layer:my-lambda-deps:1
```

It is a little annoying to have to remember those arguments every time, so let's
see what we can do about that. If I go back to my top-level `bb.edn`, I can
specify some defaults for my lambda:

``` clojure
{:deps {net.jmglov/blambda
        #_"You use the newest SHA here:"
        {:git/url "https://github.com/jmglov/blambda.git"
         :git/sha "2453e15cf75c03b2b02de5ca89c76081bba40251"}}
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (def opts {:deps-path "src/bb.edn"
                   :deps-layer-name "my-lambda-deps"
                   :bb-arch "arm64"})
  blambda {:doc "Controls Blambda runtime and layers"
           :task (blambda/dispatch opts)}}}
```

By adding the `opts` there, I've saved myself the trouble of typing
`--bb-arch`, `--deps-path`, and `--deps-layer-name`:

``` text
$ bb blambda build-deps-layer
Classpath before transforming: src:~/my-lambda/.work/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Classpath after transforming: src:/opt/m2-repo/com/cognitect/aws/endpoints/1.1.12.206/endpoints-1.1.12.206.jar:...

Compressing custom runtime layer: ~/my-lambda/target/deps.zip
```

I can now create a lambda in the AWS console that uses Blambda and my deps
layer.

![AWS Lambda console showing create function dialog][create]

Now I need to add the custom runtime layer:

![AWS Lambda console showing add layer dialog with Blambda layer selected][runtime]

And the deps layer:

![AWS Lambda console showing add layer dialog with deps layer selected][deps]

Looks nice! Now how about some Clojure code that does something exciting?

We can delete the files that AWS has put there and replace them with a
`my_lambda.clj` that looks like this:

``` clojure
(ns my-lambda
  (:require [cheshire.core :as json]))

(defn handler [event _context]
  (let [body {:msg "Lambda handler invoked"
              :data {:event event}}]
    (prn body)
    {:status 200
     :body (json/generate-string body)}))
```

The Cheshire JSON library is included free of charge by
[Babashka](https://github.com/babashka/babashka), so we can play with JSON
despite not mentioning it in our `src/bb.edn`. Amazing!

We also need to change **Runtime settings > Handler** to `my-lambda/handler`,
then click the **Deploy** button to get our lambda out there in the world!

![AWS Lambda console showing the code described above][code]

After deploying, we excitedly click the **Test** button, only to be told that we
need to configure a test event! 😭 No matter, we'll just go with the hello-world
template and hope for the best.

![AWS Lambda console showing a simple JSON test event][test]

Now that we have a test event configured, let's click **Test** again... and
celebrate a job well done! 🎉

![AWS Lambda console showing a successful lambda execution][victory]

I had actually intended to explain the CLI stuff in this post, but this is
already a bit long and my dog is starting to give me meaningful looks, so I'd
better end this thrilling instalment of [Dogfooding Blambda](tags/blambda.html)
here and take him out for a walk.

[create]:[assets/2022-08-10-create-lambda.png "Click the button!" width=800px]
[runtime]:[assets/2022-08-10-add-runtime.png "Click the button!" width=800px]
[deps]:[assets/2022-08-10-add-deps.png "Click the button!" width=800px]
[code]:[assets/2022-08-10-code.png "Such succinctness!" width=800px]
[test]:[assets/2022-08-10-test-event.png "There's nothing like JSON to delight a crowd!" width=800px]
[victory]:[assets/2022-08-10-victory.png "Victory!" width=800px]
