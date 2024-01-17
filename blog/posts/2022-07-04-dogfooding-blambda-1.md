Title: Dogfooding Blambda! : revenge of the pod people
Tags: clojure,aws,blambda,s3,babashka,lambda
Date: 2022-07-04
Discuss: https://clojurians.slack.com/archives/C04PSBFMMDJ/

Today I finally tried to use [Blambda!](https://github.com/jmglov/blambda) for
something real: a log parser for my HTTP access logs that S3 and Cloudfront
write to [my logs bucket](2022-06-24-s3-https.html). You can follow along with
my fun on Github:
[jmglov/s3-log-parser](https://github.com/jmglov/s3-log-parser).

Here's what I'm trying to do:
1. Create a lambda function that downloads access logs from S3 for a certain date
   range, parses them, and then returns some useful information
2. Save that useful information to a database
3. Write another function that provides some cool analytics on traffic going to
   my blog and use a [Lambda Function
   URL](https://aws.amazon.com/blogs/aws/announcing-aws-lambda-function-urls-built-in-https-endpoints-for-single-function-microservices/)
   to serve it up over HTTPS

I'm sure this will change quite drastically as I go, but it seems like a fun
problem that will find the rough edges with Blambda!

Here's how I proceeded.

## Creating a lambda handler

In my project, I created a simple handler in
[`src/s3_log_parser.clj`](https://github.com/jmglov/s3-log-parser/blob/main/src/s3_log_parser.clj):

``` clojure
(ns s3-log-parser)

(defn handler [event context]
  (prn {:msg "Invoked with event",
        :data {:event event}})
  {})
```

This will just log the event it was invoked with and then return an empty map
(or JSON object, if you must).

Since this lambda will eventually interact with S3, I decided to bite the bullet
and include the [babashka-aws](https://github.com/babashka/pod-babashka-aws) pod. I created a [`src/bb.edn`](https://github.com/jmglov/s3-log-parser/blob/main/src/bb.edn) like this:

``` clojure
{:paths ["."]
 :pods {org.babashka/aws {:version "0.1.2"}}}
```

This `bb.edn` will be picked up by
[Babashka](https://github.com/babashka/babashka) when it is executing my lambda,
since Blambda! runs `bb` from the directory where my lambda archive is unpacked
(`$LAMBDA_TASK_ROOT`, for those of you familiar with building custom runtimes).

## Packaging my lambda

I created a top-level
[`bb.edn`](https://github.com/jmglov/s3-log-parser/blob/main/bb.edn), which is
used for defining Babashka tasks to build and deploy my function (not to be
confused with `src/bb.edn`, which will be used at lambda runtime). The build
task looks like this:

``` clojure
build {:doc "Builds lambda artifact"
       :requires ([clojure.java.shell :refer [sh]])
       :task (let [{:keys [target-dir work-dir]} (th/parse-args)
                   work-dir (str work-dir "/lambda")
                   src-dir "src"
                   lambda-zipfile (th/target-file target-dir "function.zip")]
               (doseq [dir [target-dir work-dir]]
                 (fs/create-dirs dir))

               (doseq [f ["bb.edn" "s3_log_parser.clj"]]
                 (println "Adding file" f)
                 (fs/delete-if-exists (format "%s/%s" work-dir f))
                 (fs/copy (format "%s/%s" src-dir f) work-dir))

               (println "Compressing lambda archive:" lambda-zipfile)
               (let [{:keys [exit err]}
                     (sh "zip" "-r" lambda-zipfile "."
                         :dir work-dir)]
                 (when (not= 0 exit)
                   (println "Error:" err))))}
```

You can read the source for the `th` namespace in
[`task_helper.clj`](https://github.com/jmglov/s3-log-parser/blob/main/task_helper.clj)
if you like, but basically, what the `build` task is doing is:
1. Creating work and target directories
2. Copying the `bb.edn` and `s3_log_parser.clj` files from the `src` directory
   to the work directory
3. Zipping all the files in the work directory into `target/function.zip`

## Deploying my lambda

My `deploy` task is pretty straightforward:

``` clojure
deploy {:doc "Deploys lambda using babashka-aws."
        :depends [build]
        :requires ([pod.babashka.aws :as aws])
        :task (let [{:keys [target-dir] :as args}
                    (th/parse-args)

                    lambda-zipfile (th/target-file target-dir "function.zip")
                    zipfile (fs/read-all-bytes lambda-zipfile)]
                (th/create-or-update-lambda (assoc args :zipfile zipfile)))}
```

It reads in `target/function.zip` and passes it along to
`task-helper/create-or-update-lambda`, which is a little more interesting:

``` clojure
(defn create-or-update-lambda [{:keys [aws-region bb-arch
                                       lambda-handler lambda-name lambda-role
                                       runtime-layer zipfile]
                                :as args}]
  (let [lambda (aws/client {:api :lambda
                            :region aws-region})
        _ (println "Checking to see if lambda exists:" lambda-name)
        lambda-exists? (-> (aws/invoke lambda {:op :GetFunction
                                               :request {:FunctionName lambda-name}})
                           (contains? :Configuration))]
    (if lambda-exists?
      (update-lambda lambda args)
      (create-lambda lambda args))))
```

If no lambda with the name we've specified exists, we call `create-lambda`:

``` clojure
(defn create-lambda [lambda
                     {:keys [aws-region bb-arch
                             lambda-handler lambda-name lambda-role
                             runtime-layer zipfile]}]
  (let [lambda-arch (if (= "amd64" bb-arch) "x86_64" "arm64")
        runtime (if (= "amd64" bb-arch) "provided" "provided.al2")
        sts (aws/client {:api :sts
                         :region aws-region})
        account-id (-> (aws/invoke sts {:op :GetCallerIdentity}) :Account)
        layer-arns (->> [runtime-layer]
                        (map #(format "arn:aws:lambda:%s:%s:layer:%s"
                                      aws-region account-id
                                      (latest-layer-version lambda %))))
        role-arn (format "arn:aws:iam::%s:role/%s"
                         account-id lambda-role)
        req {:FunctionName lambda-name
             :Runtime runtime
             :Role role-arn
             :Code {:ZipFile zipfile}
             :Handler lambda-handler
             :Layers layer-arns
             :Architectures [lambda-arch]}
        _ (println "Creating lambda:" (pr-str req))
        res (aws/invoke lambda {:op :CreateFunction
                                :request req})]
    (when (contains? res :cognitect.anomalies/category)
      (println "Error:" (pr-str res)))))
```

If you're interested in this `aws/client` and `aws/invoke` stuff, this is the
[aws-api](https://github.com/cognitect-labs/aws-api) library provided by the
babashka-aws pod.

`latest-layer-version` is a simple function that checks if our layer name
includes a version (like `blambda:5`), or if not, uses Lambda's
ListLayerVersions API to select the latest version:

``` clojure
(defn latest-layer-version [lambda layer-name]
  (if (string/includes? layer-name ":")
    layer-name
    (let [latest-version (->> (aws/invoke lambda {:op :ListLayerVersions
                                                  :request {:LayerName layer-name}})
                              :LayerVersions
                              (sort-by :Version)
                              last
                              :Version)]
      (format "%s:%s" layer-name latest-version))))
```

I can now deploy this by running `bb deploy` (I've pre-baked the IAM role
required for this lambda, but it's basically the same as the one from the
[Blambda! example](https://github.com/jmglov/blambda#using-blambda)).

## Roadblock the first

The problem is, when I invoke this lambda using a test event in the console, I
get an error:

```
Test Event Name
test

Response
{
  "errorMessage": "RequestId: 8292cbcf-2862-4189-97c2-757bc58a4ed8 Error: Runtime exited with error: exit status 1",
  "errorType": "Runtime.ExitError"
}

Function Logs
ashka.pods.impl.resolver$download.invokeStatic(resolver.clj:105)
at babashka.pods.impl.resolver$pod_manifest.invokeStatic(resolver.clj:123)
at babashka.pods.impl.resolver$resolve.invokeStatic(resolver.clj:175)
at babashka.pods.impl$resolve_pod.invokeStatic(impl.clj:327)
[...]
Exception in thread "main" java.io.FileNotFoundException: /home/sbx_user1051/.babashka/pods/repository/org.babashka/aws/0.1.2/manifest.edn (No such file or directory)
at com.oracle.svm.jni.JNIJavaCallWrappers.jniInvoke_VA_LIST_FileNotFoundException_constructor_970c509c6abfd3f98898b9a7521945418b90b270(JNIJavaCallWrappers.java:0)
[...]
END RequestId: 8292cbcf-2862-4189-97c2-757bc58a4ed8
REPORT RequestId: 8292cbcf-2862-4189-97c2-757bc58a4ed8	Duration: 594.19 ms	Billed Duration: 595 ms	Memory Size: 128 MB	Max Memory Used: 21 MB	
RequestId: 8292cbcf-2862-4189-97c2-757bc58a4ed8 Error: Runtime exited with error: exit status 1
Runtime.ExitError

Request ID
8292cbcf-2862-4189-97c2-757bc58a4ed8
```

Oops! Babashka is trying to load the babashka-aws pod that I specified in my
`src/bb.edn`, but I haven't provided that pod. I could use
`babashka.pods/load-pod` at runtime to grab the pod, but that would mean that my
lambda would have a slow cold start. A better idea is to pub the pod on the
lambda instance's filesystem, but how can we do that?

The hint is in this line:

```
Exception in thread "main" java.io.FileNotFoundException: /home/sbx_user1051/.babashka/pods/repository/org.babashka/aws/0.1.2/manifest.edn (No such file or directory)
```

If I can create a `.babashka` directory in the lambda instance's home directory,
Babashka should find any pods I put there. Of course, Lambda doesn't let you do
that, but it does let you put stuff in `/opt`, by using a layer. Searching on
Clojurians Slack yielded [borkdude](https://github.com/borkdude) mentioning an
environment variable, `$BABASHKA_PODS_DIR`, which Babashka will use for the
[pods repository](https://github.com/babashka/pods).

Now I have all the pieces I need. The first step is...

## Packaging pods into a layer

I added a `build-pods` task to my top-level `bb.edn`:

``` clojure
build-pods {:doc "Builds pods layer"
            :requires ([clojure.java.shell :refer [sh]])
            :task (let [{:keys [target-dir work-dir]} (th/parse-args)
                        work-dir (str work-dir "/pods")
                        pods-dir (str (fs/home) "/.babashka/pods")
                        pods-zipfile (th/target-file target-dir "pods.zip")]
                    (doseq [dir [target-dir work-dir]]
                      (fs/create-dirs dir))

                    (doseq [pod ["org.babashka/aws/0.1.2"]
                            :let [dst (format "%s/.babashka/pods/repository/%s" work-dir pod)]]
                      (when-not (fs/exists? dst)
                        (println "Adding pod" pod)
                        (fs/copy-tree (format "%s/repository/%s" pods-dir pod) dst)))

                    (println "Compressing pods layer" pods-zipfile
                             "from dir:" work-dir)
                    (let [{:keys [exit err]}
                          (sh "zip" "-r" pods-zipfile "."
                              :dir work-dir)]
                      (when (not= 0 exit)
                        (println "Error:" err))))}
```

What it's doing is copying the
`~/.babashka/pods/repository/org.babashka/aws/0.1.2` directory into the work
dir, then adding it to `target/pods.zip`.

Deploying the layer looks like this:

``` clojure
deploy-pods {:doc "Deploys pods layer using babashka-aws."
             :depends [build-pods]
             :requires ([pod.babashka.aws :as aws])
             :task (let [{:keys [pods-layer aws-region target-dir]} (th/parse-args)
                         pods-zipfile (th/target-file target-dir "pods.zip")
                         client (aws/client {:api :lambda
                                             :region aws-region})
                         zipfile (fs/read-all-bytes pods-zipfile)
                         _ (println "Publishing layer version for layer" pods-layer)
                         res (aws/invoke client {:op :PublishLayerVersion
                                                 :request {:LayerName pods-layer
                                                           :Content {:ZipFile zipfile}}})]
                     (if (:cognitect.anomalies/category res)
                       (prn "Error:" res)
                       (println "Published layer" (:LayerVersionArn res))))}
```

## Adding the pods layer to my lambda

Since I'm already using the Blambda! layer in my lambda, adding a new layer
only requires making minor changes to `task-helper/create-lambda`:
- Add `pods-layer` to the function arguments
  ``` clojure
(defn create-lambda [lambda
                     {:keys [aws-region bb-arch
                             lambda-handler lambda-name lambda-role
                             pods-layer runtime-layer zipfile]}]
  ;; ...
)
  ```
- Add `pods-layer` to `layer-arns`
  ``` clojure
(let [layer-arns (->> [runtime-layer]
                      (map #(format "arn:aws:lambda:%s:%s:layer:%s"
                                    aws-region account-id
                                    (latest-layer-version lambda %))))
      ;; ...
      ]
  ;; ...
)
```

## Making Blambda! find my pods

The only problem left is having Blambda! set `BABASHKA_PODS_DIR` when starting
`bb`. This is a simple matter of updating the
[`bootstrap`](https://github.com/jmglov/blambda/blob/main/bootstrap) script in
Blambda! itself:

``` sh
#!/bin/sh

export BABASHKA_PODS_DIR=/opt/.babashka/pods

cd $LAMBDA_TASK_ROOT
/opt/bb -cp $LAMBDA_TASK_ROOT /opt/bootstrap.clj
```

Now when I test my lambda, I get a much more satisfying result:

```
Test Event Name
test

Response
{}

Function Logs
START RequestId: e0d573c2-5afa-4567-af74-592f12efa094 Version: $LATEST
Loading babashka lambda handler:  s3-log-parser/handler
Starting babashka lambda event loop
{:msg "Invoked with event", :data {:event {:key1 "value1", :key2 "value2", :key3 "value3"}}}
END RequestId: e0d573c2-5afa-4567-af74-592f12efa094
REPORT RequestId: e0d573c2-5afa-4567-af74-592f12efa094	Duration: 109.60 ms	Billed Duration: 559 ms	Memory Size: 128 MB	Max Memory Used: 117 MB	Init Duration: 448.61 ms

Request ID
e0d573c2-5afa-4567-af74-592f12efa094
```

## Where to next?

There is so much wrong and gross here:
- I'm hardcoding the pods that my function needs, rather than resolving them
  from the function's `bb.edn`
- I'm assuming that the pod will be in the repo instead of loading it first
- This deployment stuff is kinda neat since it doesn't require external tools,
  but also kinda sucks because it doesn't provide all the goodness that external
  tools like [Terraform](https://www.terraform.io/) do
- This function doesn't actually do anything, so I don't know if the pod is
  actually working
  
Stay tuned for more thrilling adventures as I eat my own dogfood!
