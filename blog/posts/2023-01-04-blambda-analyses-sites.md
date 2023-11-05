Title: Blambda analyses sites
Date: 2023-01-04
Tags: clojure,blambda,aws
Description: In which Blambda is all grown up!
Image: assets/2023-01-04-preview.png
Image-Alt: Movie poster for Analyze This

I'm giving a talk on [Blambda](https://github.com/jmglov/blambda) later this
month at a local meetup group or two, and since I'm an experienced speaker, the
talk is completely prepared, so now I have three weeks to rehearse it... in an
alternate reality where I'm not a huge procrastinator, that is.

In this reality, I haven't even written an outline for the talk, despite
convincing myself that I was going to do that over the holiday break. I was able
to convince myself that before I write the outline, I should just hack on
Blambda a little more to make sure it actually works. 😅

A while back, I read a post on Cyprien Pannier's blog called [Serverless site
analytics with Clojure nbb and
AWS](https://www.loop-code-recur.io/simple-site-analytics-with-serverless-clojure/).
It was a fantastic post, except for one thing: it was based on the hated
[nbb](https://github.com/babashka/nbb), surely borkdude's greatest sin!

I decided I could make the world a better place by doing the same thing, but
using Babashka instead of the unholy nbb. Now that Blambda is all grown up and
has a [v0.1.0 release](https://github.com/jmglov/blambda/releases/tag/v0.1.0),
it was the work of but a moment (for a definition of "moment" that spans several
days) to implement a site analyser!

![Movie poster for Analyze This][analyse]

[analyse]: assets/2023-01-04-preview.png "Analyse this!" width=800px

If you're the impatient sort, you can just take a look at
[examples/site-analyser](https://github.com/jmglov/blambda/tree/v0.1.0/examples/site-analyser)
in the Blambda repo, but if you wanna see how the sausage is made, buckle up,
'cause Kansas is going bye-bye!

## Getting started

Any great Babashka project starts with an empty directory, so let's make one!

``` text
$ mkdir site-analyser
$ cd site-analyser
```

We'll now add a `bb.edn` so we can start playing with Blambda:

``` clojure
{:deps {net.jmglov/blambda
        {:git/url "https://github.com/jmglov/blambda.git"
         :git/tag "v0.1.0"
         :git/sha "b80ac1d"}}
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {}))

  blambda {:doc "Controls Blambda runtime and layers"
           :task (blambda/dispatch config)}}}
```

This is enough to get us up and running with Blambda! Let's ask for help to see
how to get going:

``` text
$ bb blambda help
Usage: bb blambda <subcommand> <options>

All subcommands support the options:
                                                                                                   --work-dir   <dir> .work  Working directory
  --target-dir <dir> target Build output directory

Subcommands:

build-runtime-layer: Builds Blambda custom runtime layer
  --bb-arch            <arch>    amd64   Architecture to target (use amd64 if you don't care)
  --runtime-layer-name <name>    blambda Name of custom runtime layer in AWS
  --bb-version         <version> 1.0.168 Babashka version

...
```

Might as well try to build the custom runtime layer, which is what allows
lambdas to be written in Babashka in the first place:

``` text
$ bb blambda build-runtime-layer

Building custom runtime layer: /tmp/site-analyser/target/blambda.zip
Downloading https://github.com/babashka/babashka/releases/download/v1.0.168/babashka-1.0.168-linux-amd64-static.tar.gz
Decompressing .work/babashka-1.0.168-linux-amd64-static.tar.gz to .work
Adding file: bootstrap
Adding file: bootstrap.clj
Compressing custom runtime layer: /tmp/site-analyser/target/blambda.zip
  adding: bb (deflated 70%)
  adding: bootstrap (deflated 53%)
  adding: bootstrap.clj (deflated 62%)

$ ls -sh target/
total 21M
21M blambda.zip
```

Cool, looks legit!

## Blasting off with Graviton2

AWS being AWS, of course they had to go and design their own CPU. 🙄

It's called [Graviton](https://aws.amazon.com/ec2/graviton/), and it's based on
the ARM architecture. A little over a year ago, it became possible to [run
lambda functions on
Graviton](https://aws.amazon.com/blogs/aws/aws-lambda-functions-powered-by-aws-graviton2-processor-run-your-functions-on-arm-and-get-up-to-34-better-price-performance/),
which AWS claims delivers "up to 19 percent better performance at 20 percent
lower cost". That's party I definitely wanna go to, and luckily for me, it just
so happens that Babashka runs on ARM! 🎉 I love borkdude so much that I can
almost forgive him for the dark alchemy that wrought nbb! Almost.

We can instruct Blambda to use the ARM version of Babashka by adding a key to
the `config` map in `bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"}))
  ...
```

Let's rebuild the runtime layer and see what's up:

``` text
$ bb blambda build-runtime-layer

Building custom runtime layer: /tmp/site-analyser/target/blambda.zip
Downloading https://github.com/babashka/babashka/releases/download/v1.0.168/babashka-1.0.168-linux-aarch64-static.tar.gz
Decompressing .work/babashka-1.0.168-linux-aarch64-static.tar.gz to .work
Adding file: bootstrap
Adding file: bootstrap.clj
Compressing custom runtime layer: /tmp/site-analyser/target/blambda.zip
updating: bb (deflated 73%)
updating: bootstrap (deflated 53%)
updating: bootstrap.clj (deflated 62%)
```

That `babashka-1.0.168-linux-aarch64-static.tar.gz` looks promising!

## Say hello to my little friend

OK, so we have a custom runtime. That's awesome and all, but without a lambda
function, a runtime is a bit passé, don't you think? Let's remedy this with the
simplest of lambdas, the infamous Hello World. Except let's make it say "Hello
Blambda" instead to make it more amazing!

All we need to accomplish this is a simple handler. Let's create a
`src/handler.clj` and drop the following into it:

``` clojure
(ns handler)

(defn handler [{:keys [name] :or {name "Blambda"} :as event} context]
  (prn {:msg "Invoked with event",
        :data {:event event}})
  {:greeting (str "Hello " name "!")})
```

Now we'll need to tell Blambda what our lambda function should be called, where
to find the sources, and what handler function to use. Back in `bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :source-files ["handler.clj"]}))
  ...
```

Now that we've done this, we can build the lambda:

``` text
$ bb blambda build-lambda

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
  adding: handler.clj (deflated 25%)

$ ls -sh target/
total 22M
 22M blambda.zip  4.0K site-analyser.zip
```

Amazing! I love the fact that the entire lambda artifact is only 4 KB!

## Changing the world with Terraform

Of course, this is still academic until we deploy the function to the world, so
let's stop messing about and do it! For the rest of this post, I am going to
assume that one of the following applies to you:
1. You have a 1.x version of [Terraform](https://www.terraform.io/) installed,
   or
2. You have Nix installed (or are running on NixOS) and have run `nix-shell -p
   terraform` in your site-analyser directory

Since one of those two options is true, let's generate ourselves some Terraform
config!

``` text
$ bb blambda terraform write-config
Writing lambda layer config: /tmp/site-analyser/target/blambda.tf
Writing lambda layer vars: /tmp/site-analyser/target/blambda.auto.tfvars
Writing lambda layers module: /tmp/site-analyser/target/modules/lambda_layer.tf

$ find target/
target/
target/site-analyser.zip
target/blambda.zip
target/modules
target/modules/lambda_layer.tf
target/blambda.auto.tfvars
target/blambda.tf
```

Note the `blambda.tf`, `blambda.auto.tfvars`, and `modules/lambda_layer.tf`
files there. If you're a Terraform aficionado, feel free to take a look at
these; I'll just give you the highlights here.

`blambda.tf` defines the following resources:
- `aws_lambda_function.lambda` - the lambda function itself
- `aws_cloudwatch_log_group.lambda` - a CloudWatch log group for the lambda
  function to log to
- `aws_iam_role.lambda` - the IAM role that the lambda function will assume
- `aws_iam_policy.lambda` - an IAM policy describing what the lambda function is
  allowed to do (in this case, just write logs to its own log group)
- `aws_iam_role_policy_attachment.lambda` - a virtual Terraform resource that
  represents the attachment of the policy to the role
- `module.runtime.aws_lambda_layer_version.layer` - the custom runtime layer

`blambda.auto.tfvars` sets various Terraform variables. The details are too
boring to relate, but you are welcome to look at the file if your curiosity
overwhelms you. 😉

`modules/lambda_layer.tf` defines a Terraform module that creates a lambda
layer. The reason it's in a module and not just inline in the `blambda.tf` will
become apparent later.

## Just deploy the thing already!

OK, now that I've gone into what is almost certainly too much detail on stuff
that you almost certainly don't care about, let's just deploy the function!


``` text
$ bb blambda terraform apply
Initializing modules...
- runtime in modules

Initializing the backend...

Initializing provider plugins...
- Finding latest version of hashicorp/aws...
- Installing hashicorp/aws v4.48.0...
- Installed hashicorp/aws v4.48.0 (signed by HashiCorp)

[...]

Terraform has been successfully initialized!

[...]

Terraform will perform the following actions:

  # aws_cloudwatch_log_group.lambda will be created
  + resource "aws_cloudwatch_log_group" "lambda" {

  # aws_iam_policy.lambda will be created
  + resource "aws_iam_policy" "lambda" {

  # aws_iam_role.lambda will be created
  + resource "aws_iam_role" "lambda" {
                                                                                                   # aws_iam_role_policy_attachment.lambda will be created
  + resource "aws_iam_role_policy_attachment" "lambda" {

  # aws_lambda_function.lambda will be created
  + resource "aws_lambda_function" "lambda" {

  # module.runtime.aws_lambda_layer_version.layer will be created
  + resource "aws_lambda_layer_version" "layer" {

[...]

Plan: 6 to add, 0 to change, 0 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value:
```

Let's be brave and type "yes" and then give the Enter key a resounding
smackaroo! We should see something along the lines of this:

``` text
aws_cloudwatch_log_group.lambda: Creating...
aws_iam_role.lambda: Creating...
module.runtime.aws_lambda_layer_version.layer: Creating...
aws_cloudwatch_log_group.lambda: Creation complete after 1s [id=/aws/lambda/site-analyser]
aws_iam_policy.lambda: Creating...
aws_iam_policy.lambda: Creation complete after 1s [id=arn:aws:iam::123456789100:policy/site-analyser]
aws_iam_role.lambda: Creation complete after 2s [id=site-analyser]
aws_iam_role_policy_attachment.lambda: Creating...
aws_iam_role_policy_attachment.lambda: Creation complete after 1s [id=site-analyser-20230103173233475200000001]
module.runtime.aws_lambda_layer_version.layer: Still creating... [10s elapsed]
module.runtime.aws_lambda_layer_version.layer: Still creating... [20s elapsed]
module.runtime.aws_lambda_layer_version.layer: Still creating... [30s elapsed]
module.runtime.aws_lambda_layer_version.layer: Creation complete after 31s [id=arn:aws:lambda:eu-west-1:123456789100:layer:blambda:30]
aws_lambda_function.lambda: Creating...
aws_lambda_function.lambda: Still creating... [10s elapsed]
aws_lambda_function.lambda: Creation complete after 11s [id=site-analyser]

Apply complete! Resources: 6 added, 0 changed, 0 destroyed.
```

OK, let's try invoking the function:

``` text
$ aws lambda invoke --function-name site-analyser /tmp/response.json
{
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
}

$ cat /tmp/response.json
{"greeting":"Hello Blambda!"}
```

According to the handler, we can also pass a name in:

``` text
$ aws lambda invoke --function-name site-analyser --payload '{"name": "Dear Reader"}' /tmp/response.json
{
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
}

$ cat /tmp/response.json 
{"greeting":"Hello Dear Reader!"}
```

Looks like we're live in the cloud!

## HTTP FTW!

It's pretty annoying to have to use the AWS CLI to invoke our function, what
with all of the writing the response to a file and all that jazz. Luckily,
[Lambda function
URLs](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html) offer us a
way out of this never-ending agony. All we need to do is add one simple
Terraform resource.

Let's create a `tf/main.tf` and define our function URL:

``` terraform
resource "aws_lambda_function_url" "lambda" {
  function_name = aws_lambda_function.lambda.function_name
  authorization_type = "NONE"
}

output "function_url" {
  value = aws_lambda_function_url.lambda.function_url
}
```

When using a function URL, the event passed to the lambda function looks a
little different. Referring to the [Request and response
payloads](https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html#urls-payloads)
page in the AWS Lambda developer guide, we hone in on the important bits:

**Request**

``` json
{
  "queryStringParameters": {
    "parameter1": "value1,value2",
    "parameter2": "value"
  },
  "requestContext": {
    "http": {
      "method": "POST",
      "path": "/my/path",
    }
  }
}
```

**Response**

``` json
{
  "statusCode": 201,
  "headers": {
    "Content-Type": "application/json",
    "My-Custom-Header": "Custom Value"
  },
  "body": "{ \"message\": \"Hello, world!\" }"
}
```

Let's update our handler to log the method and path and grab the optional `name`
from the query params. Our new `src/handler.clj` now looks like this:

``` clojure
(ns handler
  (:require [cheshire.core :as json]))

(defn log [msg data]
  (prn (assoc data :msg msg)))

(defn handler [{:keys [queryStringParameters requestContext] :as event} _context]
  (log "Invoked with event" {:event event})
  (let [{:keys [method path]} (:http requestContext)
        {:keys [name] :or {name "Blambda"}} queryStringParameters]
    (log (format "Request: %s %s" method path)
         {:method method, :path path, :name name})
    {:statusCode 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string {:greeting (str "Hello " name "!")})}))
```

The final step before we can deploy this gem is letting Blambda know that we
want to include some extra Terraform config. For this, we set the
`:extra-tf-config` key in `bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :source-files ["handler.clj"]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

Now that all of this is done, let's rebuild our lambda, regenerate our Terraform
config, and deploy away!

``` text
$ bb blambda build-lambda

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
updating: handler.clj (deflated 43%)

$ bb blambda terraform write-config
Copying Terraform config tf/main.tf
Writing lambda layer config: /tmp/site-analyser/target/blambda.tf
Writing lambda layer vars: /tmp/site-analyser/target/blambda.auto.tfvars
Writing lambda layers module: /tmp/site-analyser/target/modules/lambda_layer.tf

$ bb blambda terraform apply
                                                                                                 
Terraform will perform the following actions:

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {

  # aws_lambda_function_url.lambda will be created
  + resource "aws_lambda_function_url" "lambda" {

Plan: 1 to add, 1 to change, 0 to destroy.

Changes to Outputs:
  + function_url = (known after apply)

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_lambda_function.lambda: Modifications complete after 7s [id=site-analyser]
aws_lambda_function_url.lambda: Creating...
aws_lambda_function_url.lambda: Creation complete after 1s [id=site-analyser]

Apply complete! Resources: 1 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

Looks great! The `function_url` is the base URL we'll use to make HTTP requests
to our lambda. We can try it out with curl:

``` text
$ export BASE_URL=https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/

$ curl $BASE_URL
{"greeting":"Hello Blambda!"}

$ curl $BASE_URL?name='Dear%20Reader'
{"greeting":"Hello Dear Reader!"}
```

## Logging isn't just for lumberjacks

Before we move on, let's take a quick look at our logs. Lambda functions
automatically log anything printed to standard output to a log stream in a log
group named `/aws/lambda/{function_name}` (as long as they have permission to do
so, which Blambda takes care of for us in the default IAM policy). Let's see
what log streams we have in our `/aws/lambda/site-analyser` group:

``` text
$ aws logs describe-log-streams --log-group /aws/lambda/site-analyser \
    | jq '.logStreams | .[].log[18/1807]e'
"2023/01/03/[$LATEST]f8a5d5be9e0c4d34bcf6c8bb55e9c577"
"2023/01/04/[$LATEST]98a0ab46e2994cdda668124ccae610fc"
```

We have two streams since we've tested our lambda twice (requests around the
same time are batched into a single log stream). Let's pick the most recent one
and see what it says:

``` text
$ aws logs get-log-events \
  --log-group /aws/lambda/site-analyser \
  --log-stream '2023/01/04/[$LATEST]98a0ab46e2994cdda668124ccae610fc' \
  | jq -r '.events|.[].message'
Starting Babashka:
/opt/bb -cp /var/task /opt/bootstrap.clj
Loading babashka lambda handler: handler/handler

Starting babashka lambda event loop

START RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a Version: $LATEST

{:event {:version "2.0", :routeKey "$default", :rawPath "/", ...}

{:method "GET", :path "/", :name "Blambda", :msg "Request: GET /"}

END RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a

REPORT RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a  Duration: 56.87 ms      Billed Duration: 507 ms Memory Size: 512 MB     Max Memory Used: 125 MB Init Duration: 450.02 ms

START RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4 Version: $LATEST

{:event {:version "2.0", :routeKey "$default", :rawPath "/", ...}

{:method "GET", :path "/", :name "Dear Reader", :msg "Request: GET /"}

END RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4

REPORT RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4  Duration: 2.23 ms       Billed Duration: 3 ms   Memory Size: 512 MB     Max Memory Used: 125 MB
```

There are a few things of interest here. First of all, we can see the Blambda
custom runtime starting up:

``` text
Starting Babashka:
/opt/bb -cp /var/task /opt/bootstrap.clj
Loading babashka lambda handler: handler/handler

Starting babashka lambda event loop
```

This only happens on a so-called "cold start", which is when there is no lambda
instance available to serve an invocation request. We always have a cold start
on the first invocation of a lambda after it's deployed (i.e. on every code
change), and then the lambda will stay warm for about 15 minutes after each
invocation.

Next in our logs, we see the first test request we made:

``` text
START RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a Version: $LATEST

{:event {:version "2.0", :routeKey "$default", :rawPath "/", ...}

{:method "GET", :path "/", :name "Blambda", :msg "Request: GET /"}

END RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a

REPORT RequestId: 09a33775-0151-4d83-9a9b-b21b8add2b3a  Duration: 56.87 ms      Billed Duration: 507 ms Memory Size: 512 MB     Max Memory Used: 125 MB Init Duration: 450.02 ms
```

We can see the full event that is passed to the lambda by the function URL in
the first EDN log line (which we should probably switch to JSON for
compatibility will common log aggregation tools), then the log statement we
added for the method, path, and name parameter. Finally, we get a report on the
lambda invocation. We can see that it took 450 ms to initialise the runtime
(seems a bit long; maybe increasing the memory size of our function would help),
then 56.87 ms for the function invocation itself.

Let's compare that to the second invocation, the one where we added `name` to
the query parameters:

``` text
START RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4 Version: $LATEST

{:event {:version "2.0", :routeKey "$default", :rawPath "/", ...}

{:method "GET", :path "/", :name "Dear Reader", :msg "Request: GET /"}

END RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4

REPORT RequestId: 0fca5efe-2644-4ebd-80ce-ebb390ffcaf4  Duration: 2.23 ms       Billed Duration: 3 ms   Memory Size: 512 MB     Max Memory Used: 125 MB
```

Note that we don't even have an init duration in this invocation, since the
lambda was warm. Note also that the request duration was 2.23 ms!

Just for fun, let's make a few more requests and look at the durations.

``` text
$ for i in $(seq 0 9); do curl $BASE_URL?name="request%20$i"; echo; done
{"greeting":"Hello request 0!"}
{"greeting":"Hello request 1!"}
{"greeting":"Hello request 2!"}
{"greeting":"Hello request 3!"}
{"greeting":"Hello request 4!"}
{"greeting":"Hello request 5!"}
{"greeting":"Hello request 6!"}
{"greeting":"Hello request 7!"}
{"greeting":"Hello request 8!"}
{"greeting":"Hello request 9!"}

$ aws logs describe-log-streams \
  --log-group /aws/lambda/site-analyser \
  | jq '.logStreams | .[].logStreamName'
"2023/01/03/[$LATEST]f8a5d5be9e0c4d34bcf6c8bb55e9c577"
"2023/01/04/[$LATEST]6532afd4465240dcb3f105abe2bcc250"
"2023/01/04/[$LATEST]98a0ab46e2994cdda668124ccae610fc"
```

Hrm, `2023/01/04/[$LATEST]98a0ab46e2994cdda668124ccae610fc` was the stream we
looked at last time, so let's assume that
`2023/01/04/[$LATEST]6532afd4465240dcb3f105abe2bcc250` has our latest requests:

``` text
$ aws logs get-log-events \
  --log-group /aws/lambda/site-analyser \
  --log-stream '2023/01/04/[$LATEST][7/1936]465240dcb3f105abe2bcc250' \
  | jq -r '.events | .[].message' \
  | grep '^REPORT'
REPORT RequestId: 4ee5993e-6d21-45cd-9b05-b31ea34d993f  Duration: 54.32 ms      Billed Duration: 505 ms Memory Size: 512 MB     Max Memory Used: 125 MB Init Duration: 450.45 ms
REPORT RequestId: 490b82c7-bca1-4427-8d07-ece41444ce2c  Duration: 1.81 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 125 MB
REPORT RequestId: ca243a59-75b9-4192-aa91-76569765956a  Duration: 3.94 ms       Billed Duration: 4 ms   Memory Size: 512 MB     Max Memory Used: 126 MB
REPORT RequestId: 9d0981f8-3c48-45a5-bf8b-c37ed57e0f95  Duration: 1.77 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 126 MB
REPORT RequestId: 2d5cca3f-752d-4407-99cd-bbb89ca74983  Duration: 1.73 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 126 MB
REPORT RequestId: 674912af-b9e0-4308-b303-e5891a459ad1  Duration: 1.65 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 126 MB
REPORT RequestId: d8efbec2-de6e-491d-b4c6-ce58d02225f1  Duration: 1.67 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 126 MB
REPORT RequestId: c2a9246d-e3c4-40fa-9eb9-82fc200e6425  Duration: 1.64 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 127 MB
REPORT RequestId: cbc2f1cd-23cf-4b26-87d4-0272c097956c  Duration: 1.72 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 127 MB
REPORT RequestId: bae2e73b-4b21-4427-b0bd-359301722086  Duration: 1.73 ms       Billed Duration: 2 ms   Memory Size: 512 MB     Max Memory Used: 127 MB
```

Again, we see a cold start (because it apparently took me more than 15 minutes
to write the part of the post since my first two test requests), then 9 more
requests with durations mostly under 2 ms—dunno why there's a 3.94 ms outlier,
but this is hardly a scientific benchmark. 😉

## Getting down to brass tracks

OK, we've now got a lambda that can listen to HTTP requests. To turn it into the
site analyser that was promised at the beginning of this blog post, we'll define
a simple HTTP API:
- `POST /track?url={url}` - increment the number of views for the specified URL
- `GET /dashboard` - display a simple HTML dashboard showing the number of
  views of each URL for the past 7 days

In order to implement the `/track` endpoint, we'll need somewhere to store the
counters, and what better place than DynamoDB? We'll create a simple table with
the date as the partition key and the url as the range key, which we can add to
our `tf/main.tf` like so:

``` terraform
resource "aws_dynamodb_table" "site_analyser" {
  name = "site-analyser"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "date"
  range_key = "url"

  attribute {
    name = "date"
    type = "S"
  }

  attribute {
    name = "url"
    type = "S"
  }
}
```

We'll also need to give the lambda permissions to update and query this table,
which means we'll need to define a custom IAM policy. 😭

Oh well, let's bite the bullet and add it to `tf/main.tf`:

``` terraform
resource "aws_iam_role" "lambda" {
  name = "site-analyser-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "lambda" {
  name = "site-analyser-lambda"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.lambda.arn}:*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:Query",
          "dynamodb:UpdateItem",
        ]
        Resource = aws_dynamodb_table.site_analyser.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda" {
  role = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.lambda.arn
}
```

Since Blambda won't be automatically generating a policy for us, we'll need to
add a statement to the policy giving the lambda permission to write to
CloudWatch Logs:

``` terraform
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.lambda.arn}:*"
      },
      ...
```

and another one giving the lambda permissions to use the DynamoDB table:

``` terraform
    Statement = [
      ...
      {
        Effect = "Allow"
        Action = [
          "dynamodb:Query",
          "dynamodb:UpdateItem",
        ]
        Resource = aws_dynamodb_table.site_analyser.arn
      }
    ]
```

Finally, we need to instruct Blambda that we'll be providing our own IAM role by
adding the `:lambda-iam-role` key to `bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :lambda-iam-role "${aws_iam_role.lambda.arn}"
                       :source-files ["handler.clj"]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

Before we implement the tracker, let's make sure that the new Terraform stuff we
did all works:

``` text
$ bb blambda terraform write-config
Copying Terraform config tf/main.tf
Writing lambda layer config: /tmp/site-analyser/target/blambda.tf
Writing lambda layer vars: /tmp/site-analyser/target/blambda.auto.tfvars
Writing lambda layers module: /tmp/site-analyser/target/modules/lambda_layer.tf

$ bb blambda terraform apply
[...]
Terraform will perform the following actions:

  # aws_dynamodb_table.site_analyser will be created
  + resource "aws_dynamodb_table" "site_analyser" {

  # aws_iam_policy.lambda must be replaced
-/+ resource "aws_iam_policy" "lambda" {

  # aws_iam_role.lambda must be replaced
-/+ resource "aws_iam_role" "lambda" {

  # aws_iam_role_policy_attachment.lambda must be replaced
-/+ resource "aws_iam_role_policy_attachment" "lambda" {

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {

Plan: 4 to add, 1 to change, 3 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

aws_iam_role.lambda: Creating...
aws_dynamodb_table.site_analyser: Creating...
aws_iam_role.lambda: Creation complete after 2s [id=site-analyser-lambda]
aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_dynamodb_table.site_analyser: Creation complete after 7s [id=site-analyser]
aws_iam_policy.lambda: Creating...
aws_iam_policy.lambda: Creation complete after 1s [id=arn:aws:iam::289341159200:policy/site-analyser-lambda]
aws_iam_role_policy_attachment.lambda: Creating...
aws_iam_role_policy_attachment.lambda: Creation complete after 0s [id=site-analyser-lambda-20230104115714236700000001]
aws_lambda_function.lambda: Still modifying... [id=site-analyser, 10s elapsed]
aws_lambda_function.lambda: Still modifying... [id=site-analyser, 20s elapsed]
aws_lambda_function.lambda: Modifications complete after 22s [id=site-analyser]

Apply complete! Resources: 4 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

Lookin' good!

## Getting down to brass tracks for real this time

OK, now that we have a DynamoDB table and permissions to update it, let's
implement the `/track` endpoint. The first thing we'll need to do is add
[awyeah-api](https://github.com/grzm/awyeah-api) (a library which makes
Cognitect's [aws-api](https://github.com/cognitect-labs/aws-api) work with
Babashka) to talk to DynamoDB. We'll create a `src/bb.edn` and add the
following:

``` clojure
{:paths ["."]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.373"}
        com.cognitect.aws/dynamodb {:mvn/version "825.2.1262.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "0fa7dd51f801dba615e317651efda8c597465af6"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "433b0778e2c32f4bb5d0b48e5a33520bee28b906"}}}
```

To let Blambda know that it should build a lambda layer for the dependencies, we
need to add a `:deps-layer-name` key to the config in our top-level `bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :deps-layer-name "site-analyser-deps"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :lambda-iam-role "${aws_iam_role.lambda.arn}"
                       :source-files ["handler.clj"]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

Blambda will automatically look in `src/bb.edn` to find the dependencies to
include in the layer. Let's test this out by building the deps layer:

``` text
$ bb blambda build-deps-layer

Building dependencies layer: /tmp/site-analyser/target/site-analyser-deps.zip
Classpath before transforming: src:/tmp/site-analyser/.work/m2-repo/com/cognitect/aws/dynamodb/825.2.1262.0/...

Classpath after transforming: src:/opt/m2-repo/com/cognitect/aws/dynamodb/825.2.1262.0/dynamodb-825.2.1262.0.jar:...

Compressing dependencies layer: /tmp/site-analyser/target/site-analyser-deps.zip
  adding: gitlibs/ (stored 0%)
  adding: gitlibs/_repos/ (stored 0%)
[...]
  adding: m2-repo/com/cognitect/aws/dynamodb/825.2.1262.0/dynamodb-825.2.1262.0.pom.sha1 (deflated 3%)
  adding: deps-classpath (deflated 67%)
```

And since we have a new layer, we'll need to generate new Terraform config:

``` text
$ bb blambda terraform write-config
Copying Terraform config tf/main.tf
Writing lambda layer config: /tmp/site-analyser/target/blambda.tf
Writing lambda layer vars: /tmp/site-analyser/target/blambda.auto.tfvars
Writing lambda layers module: /tmp/site-analyser/target/modules/lambda_layer.tf
```

Let's now whip up a `src/page_views.clj`:

``` clojure
(ns page-views
  (:require [com.grzm.awyeah.client.api :as aws]))

(defn log [msg data]
  (prn (assoc data :msg msg)))

(defmacro ->map [& ks]
  (assert (every? symbol? ks))
  (zipmap (map keyword ks)
          ks))

(defn ->int [s]
  (Integer/parseUnsignedInt s))

(defn client [{:keys [aws-region] :as config}]
  (assoc config :dynamodb (aws/client {:api :dynamodb, :region aws-region})))

(defn validate-response [res]
  (when (:cognitect.anomalies/category res)
    (let [data (merge (select-keys res [:cognitect.anomalies/category])
                      {:err-msg (:Message res)
                       :err-type (:__type res)})]
      (log "DynamoDB request failed" data)
      (throw (ex-info "DynamoDB request failed" data))))
  res)

(defn increment! [{:keys [dynamodb views-table] :as client} date url]
  (let [req {:TableName views-table
             :Key {:date {:S date}
                   :url {:S url}}
             :UpdateExpression "ADD #views :increment"
             :ExpressionAttributeNames {"#views" "views"}
             :ExpressionAttributeValues {":increment" {:N "1"}}
             :ReturnValues "ALL_NEW"}
        _ (log "Incrementing page view counter"
               (->map date url req))
        res (-> (aws/invoke dynamodb {:op :UpdateItem
                                      :request req})
                validate-response)
        new-counter (-> res
                        (get-in [:Attributes :views :N])
                        ->int)
        ret (->map date url new-counter)]
    (log "Page view counter incremented"
         ret)
    ret))
```

## REPL-driven development, naturally

That's a bunch of code to have written without knowing it works, so let's act
like real Clojure developers and fire up a REPL:

``` text
$ cd src/

$ bb nrepl-server 0
Started nREPL server at 127.0.0.1:42733
For more info visit: https://book.babashka.org/#_nrepl
```

Once this is done, we can connect from our text editor (Emacs, I hope) and test
things out in a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment):

``` clojure
(comment

  (def c (client {:aws-region "eu-west-1", :views-table "site-analyser"}))

  (increment! c "2023-01-04" "https://example.com/test.html")
  ;; => {:date "2023-01-04", :url "https://example.com/test.html", :new-counter 1}

  (increment! c "2023-01-04" "https://example.com/test.html")
  ;; => {:date "2023-01-04", :url "https://example.com/test.html", :new-counter 2}

  )
```

Lookin' good! 😀

Let's populate the table with a bunch of data (this will take a little while):

``` clojure
(comment

  (doseq [date (map (partial format "2022-12-%02d") (range 1 32))
          url (map (partial format "https://example.com/page-%02d") (range 1 11))]
    (dotimes [_ (rand-int 5)]
      (increment! c date url)))
  ;; nil

  )
```

Just for fun, we can take a quick peek at what the data looks like in DynamoDB:

``` clojure
(comment

  (let [{:keys [dynamodb views-table]} c]
    (aws/invoke dynamodb {:op :Scan
                          :request {:TableName views-table
                                    :Limit 5}}))
  ;; => {:Items
  ;;     [{:views {:N "7"},
  ;;       :date {:S "2022-12-12"},
  ;;       :url {:S "https://example.com/page-01"}}
  ;;      {:views {:N "16"},
  ;;       :date {:S "2022-12-12"},
  ;;       :url {:S "https://example.com/page-02"}}
  ;;      {:views {:N "14"},
  ;;       :date {:S "2022-12-12"},
  ;;       :url {:S "https://example.com/page-03"}}
  ;;      {:views {:N "8"},
  ;;       :date {:S "2022-12-12"},
  ;;       :url {:S "https://example.com/page-05"}}
  ;;      {:views {:N "6"},
  ;;       :date {:S "2022-12-12"},
  ;;       :url {:S "https://example.com/page-06"}}],
  ;;     :Count 5,
  ;;     :ScannedCount 5,
  ;;     :LastEvaluatedKey
  ;;     {:url {:S "https://example.com/page-06"}, :date {:S "2022-12-12"}}}

  )
```

## You can't handle the track!

Now that we have the DynamoDB machinery in place, let's connect it to our
handler. But first, let's do a quick bit of refactoring so that we don't have to
duplicate the `log` function in both our `handler` and `page-views` namespaces.
We'll create a `src/util.clj` and add the following:

``` clojure
(ns util
  (:import (java.net URLDecoder)
           (java.nio.charset StandardCharsets)))

(defmacro ->map [& ks]
  (assert (every? symbol? ks))
  (zipmap (map keyword ks)
          ks))

(defn ->int [s]
  (Integer/parseUnsignedInt s))

(defn log
  ([msg]
   (log msg {}))
  ([msg data]
   (prn (assoc data :msg msg))))
```

We need to update `page_views.clj` to use this namespace:

``` clojure
(ns page-views
  (:require [com.grzm.awyeah.client.api :as aws]
            [util :refer [->map]]))

(defn client [{:keys [aws-region] :as config}]
  (assoc config :dynamodb (aws/client {:api :dynamodb, :region aws-region})))

(defn validate-response [res]
  (when (:cognitect.anomalies/category res)
    (let [data (merge (select-keys res [:cognitect.anomalies/category])
                      {:err-msg (:Message res)
                       :err-type (:__type res)})]
      (util/log "DynamoDB request failed" data)
      (throw (ex-info "DynamoDB request failed" data))))
  res)

(defn increment! [{:keys [dynamodb views-table] :as client} date url]
  (let [req {:TableName views-table
             :Key {:date {:S date}
                   :url {:S url}}
             :UpdateExpression "ADD #views :increment"
             :ExpressionAttributeNames {"#views" "views"}
             :ExpressionAttributeValues {":increment" {:N "1"}}
             :ReturnValues "ALL_NEW"}
        _ (util/log "Incrementing page view counter"
                    (->map date url req))
        res (-> (aws/invoke dynamodb {:op :UpdateItem
                                      :request req})
                validate-response)
        new-counter (-> res
                        (get-in [:Attributes :views :N])
                        util/->int)
        ret (->map date url new-counter)]
    (util/log "Page view counter incremented"
              ret)
    ret))
```

Now we can turn our eye to `handler.clj`. First, we pull in the `util` namespace
and use its `log` function:

``` clojure
(ns handler
  (:require [cheshire.core :as json]
            [util :refer [->map]]))

(defn handler [{:keys [queryStringParameters requestContext] :as event} _context]
  (util/log "Invoked with event" {:event event})
  (let [{:keys [method path]} (:http requestContext)
        {:keys [name] :or {name "Blambda"}} queryStringParameters]
    (util/log (format "Request: %s %s" method path)
              {:method method, :path path, :name name})
    {:statusCode 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string {:greeting (str "Hello " name "!")})}))
```

Now, let's remove the hello world stuff and add a `/track` endpoint. We expect
our clients to make an HTTP `POST` request to the `/track` path, so we can use a
simple pattern match in the handler for this:

``` clojure
(defn handler [{:keys [queryStringParameters requestContext] :as event} _context]
  (util/log "Invoked with event" {:event event})
  (let [{:keys [method path]} (:http requestContext)]
    (util/log (format "Request: %s %s" method path)
              {:method method, :path path, :name name})
    (case [method path]
      ["POST" "/track"]
      (let [{:keys [url]} queryStringParameters]
        (if url
          (do
            (util/log "Should be tracking a page view here" (->map url))
            {:statusCode 204})
          {:statusCode 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:msg "Missing required param: url"})}))

      {:statusCode 404
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:msg (format "Resource not found: %s" path)})})))
```

We can test this out in the REPL:

``` clojure
(comment

  (handler {:requestContext {:http {:method "POST" :path "/nope"}}} nil)
  ;; => {:statusCode 404,
  ;;     :headers {"Content-Type" "application/json"},
  ;;     :body "{\"msg\":\"Resource not found: /nope\"}"}

  (handler {:requestContext {:http {:method "GET" :path "/track"}}} nil)
  ;; => {:statusCode 404,
  ;;     :headers {"Content-Type" "application/json"},
  ;;     :body "{\"msg\":\"Resource not found: /track\"}"}

  (handler {:requestContext {:http {:method "POST" :path "/track"}}} nil)
  ;; => {:statusCode 400,
  ;;     :headers {"Content-Type" "application/json"},
  ;;     :body "{\"msg\":\"Missing required param: url\"}"}

  (handler {:requestContext {:http {:method "POST" :path "/track"}}
            :queryStringParameters {:url "https://example.com/test.html"}} nil)
  ;; => {:statusCode 204}

  )
```

Now we need to connect this to `page-views/increment!`, which in addition to the
URL, also requires a date. Before figuring out how to provide that, let's
extract the tracking code into a separate function so we don't keep adding stuff
to the `handler` function:

``` clojure
(defn track-visit! [{:keys [queryStringParameters] :as event}]
  (let [{:keys [url]} queryStringParameters]
    (if url
      (do
        (util/log "Should be tracking a page view here" (->map url))
        {:statusCode 204})
      {:statusCode 400
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:msg "Missing required param: url"})})))
```

Now we can simplify the `case` statement:

``` clojure
    (case [method path]
      ["POST" "/track"] (track-visit! event)

      {:statusCode 404
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:msg (format "Resource not found: %s" path)})})
```

Babashka provides the `java.time` classes, so we can get the current date using
`java.time.LocalDate`. Let's import it into our namespace then use it in our
shiny new `track-visit!` function:

``` clojure
(ns handler
  (:require [cheshire.core :as json]
            [util :refer [->map]])
  (:import (java.time LocalDate)))

(defn track-visit! [{:keys [queryStringParameters] :as event}]
  (let [{:keys [url]} queryStringParameters]
    (if url
      (let [date (str (LocalDate/now))]
        (util/log "Should be tracking a page view here" (->map date url))
        {:statusCode 204})
      (do
        (util/log "Missing required query param" {:param "url"})
        {:statusCode 400
         :body "Missing required query param: url"}))))
```

Let's test this out in the REPL:

``` clojure
(comment

  (handler {:requestContext {:http {:method "POST" :path "/track"}}
            :queryStringParameters {:url "https://example.com/test.html"}} nil)
  ;; => {:statusCode 201}

  )
```

You should see something like this printed to your REPL's output buffer:

``` clojure
{:event {:requestContext {:http {:method "POST", :path "/track"}}, :queryStringParameters {:url "https://example.com/test.html"}}, :msg "Invoked with event"}
{:method "POST", :path "/track", :msg "Request: POST /track"}
{:url "https://example.com/test.html", :msg "Should be tracking a page view here"}
```

## Wiring it all up

Finally, we need to connect the handler to `page-views/increment!`. `increment!`
expects us to pass it a page views client, which contains a DynamoDB client,
which will establish an HTTP connection when first used, which will take a
couple of milliseconds. We would like this HTTP connection to be shared across
invocations of our lambda so that we only need to establish it on a cold start
(or whenever the DynamoDB API feels like the connection has been idle too long
and decides to close it), so we'll use the trick of defining it outside our
handler function:

``` clojure
(ns handler
  (:require [cheshire.core :as json]
            [page-views]
            [util :refer [->map]])
  (:import (java.time LocalDate)))

(def client (page-views/client {:aws-region "eu-west-1"
                                :views-table "site-analyser"}))
```

Now we have everything we need! We'll replace our placeholder log line:

``` clojure
        (util/log "Should be tracking a page view here" (->map url))
```

with an actual call to `page-views/increment!`:

``` clojure
        (page-views/increment! client date url)
```

Let's test this one more time in the REPL before deploying it:

``` clojure
(comment

  (handler {:requestContext {:http {:method "POST" :path "/track"}}
            :queryStringParameters {:url "https://example.com/test.html"}} nil)
  ;; => {:statusCode 201}

  )
```

This time, we should see an actual DynamoDB query logged:

``` clojure
{:event {:requestContext {:http {:method "POST", :path "/track"}}, :queryStringParameters {:url "https://example.com/test.html"}}, :msg "Invoked with event"}
{:method "POST", :path "/track", :msg "Request: POST /track"}
{:date "2023-01-04", :url "https://example.com/test.html", :req {:TableName "site-analyser", :Key {:date {:S "2023-01-04"}, :url {:S "https://example.com/test.html"}}, :UpdateExpression "ADD #views :increment", :ExpressionAttributeNames {"#views" "views"}, :ExpressionAttributeValues {":increment" {:N "1"}}, :ReturnValues "ALL_NEW"}, :msg "Incrementing page view counter"}
{:date "2023-01-04", :url "https://example.com/test.html", :new-counter 3, :msg "Page view counter incremented"}
```

## Deploying the real deal

Let's recap what we've done:
1. Added awyeah-api as a dependency
2. Add two new namespaces: `page-views` and `util`
3. Updated our handler to actually use DynamoDB


Since we added more source files, we need to add them to the `:source-files`
list in the top-level `bb-edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :deps-layer-name "site-analyser-deps"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :lambda-iam-role "${aws_iam_role.lambda.arn}"
                       :source-files ["handler.clj" "page_views.clj" "util.clj"]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

Once this is done, we can rebuild our lambda and reploy:

``` text
$ bb blambda build-lambda

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Adding file: page_views.clj
Adding file: util.clj
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
updating: handler.clj (deflated 66%)
  adding: page_views.clj (deflated 59%)
  adding: util.clj (deflated 35%)

[nix-shell:/tmp/site-analyser]$ bb blambda terraform apply

Terraform will perform the following actions:

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {

  # module.deps.aws_lambda_layer_version.layer will be created
  + resource "aws_lambda_layer_version" "layer" {

Plan: 1 to add, 1 to change, 0 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

module.deps.aws_lambda_layer_version.layer: Creating...
module.deps.aws_lambda_layer_version.layer: Still creating... [10s elapsed]
module.deps.aws_lambda_layer_version.layer: Creation complete after 10s [id=arn:aws:lambda:eu-west-1:289341159200:layer:site-analyser-de
ps:1]
aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_lambda_function.lambda: Still modifying... [id=site-analyser, 10s elapsed]
aws_lambda_function.lambda: Modifications complete after 11s [id=site-analyser]

Apply complete! Resources: 1 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

Now that this is deployed, we can test it by tracking a view:

``` text
 curl -v -X POST $BASE_URL/track?url=https%3A%2F%2Fexample.com%2Ftest.html
*   Trying 54.220.150.207:443...
* Connected to kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws (54.220.150.207) port 443 (#0)
[...]
< HTTP/1.1 204 No Content
< Date: Wed, 04 Jan 2023 16:01:24 GMT
< Content-Type: application/json
< Connection: keep-alive
< x-amzn-RequestId: 2d5c6a9d-d3a4-4abb-9a57-c956ca3030f3
< X-Amzn-Trace-Id: root=1-63b5a2d4-58cb681c06672bb410efe80f;sampled=0
<
* Connection #0 to host kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws left intact
```

Looks like it worked!

## Configuration station

Before we forge on, let's deal with the annoying hardcoding of our client
config:

``` clojure
(def client (page-views/client {:aws-region "eu-west-1"
                                :views-table "site-analyser"}))
```

The normal way of configuring lamdbas is to set environment variables, so let's
do that:

```clojure
(defn get-env
  ([k]
   (or (System/getenv k)
       (let [msg (format "Missing env var: %s" k)]
         (throw (ex-info msg {:msg msg, :env-var k})))))
  ([k default]
   (or (System/getenv k) default)))

(def config {:aws-region (get-env "AWS_REGION" "eu-west-1")
             :views-table (get-env "VIEWS_TABLE")})

(def client (page-views/client config))
```

Now if we set the `VIEWS_TABLE` environment variable in our lambda config
(`AWS_REGION` is set by the lambda runtime itself), we're good to go. We can
tell Blambda to do this for us by adding a `:lambda-env-vars` to our top-level
`bb.edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :deps-layer-name "site-analyser-deps"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :lambda-iam-role "${aws_iam_role.lambda.arn}"
                       :lambda-env-vars ["VIEWS_TABLE=${aws_dynamodb_table.site_analyser.name}"]
                       :source-files ["handler.clj" "page_views.clj" "util.clj"]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

We'll set the name using the Terraform resource that we defined
(`aws_dynamodb_table.site_analyser`), so that if we decide to change the table
name, we'll only need to change it in `tf/main.tf`. The odd format for
`:lambda-env-vars` is to support specifying it from the command line, so just
hold your nose and move on.

Let's rebuild our lambda, regenerate our Terraform config, and redeploy:

``` text
$ bb blambda build-lambda                                                                        [54/1822]

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Adding file: page_views.clj
Adding file: util.clj
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
updating: handler.clj (deflated 64%)
updating: page_views.clj (deflated 59%)
updating: util.clj (deflated 35%)

$ bb blambda terraform write-config
Copying Terraform config tf/main.tf
Writing lambda layer config: /tmp/site-analyser/target/blambda.tf
Writing lambda layer vars: /tmp/site-analyser/target/blambda.auto.tfvars
Writing lambda layers module: /tmp/site-analyser/target/modules/lambda_layer.tf

$ bb blambda terraform apply

Terraform will perform the following actions:

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {
        # (19 unchanged attributes hidden)

      + environment {
          + variables = {
              + "VIEWS_TABLE" = "site-analyser"
            }
        }
    }

Plan: 0 to add, 1 to change, 0 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_lambda_function.lambda: Still modifying... [id=site-analyser, 10s elapsed]
aws_lambda_function.lambda: Modifications complete after 15s [id=site-analyser]

Apply complete! Resources: 0 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

## A dashing dashboard

The final piece of the puzzle is displaying the site analytics. We said that
`GET /dashboard` should serve up a nice HTML page, so let's add a route for this
to our handler:

``` clojure
    (case [method path]
      ["GET" "/dashboard"] (serve-dashboard event)
      ["POST" "/track"] (track-visit! event)

      {:statusCode 404
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:msg (format "Resource not found: %s" path)})})
```

Before we write the `serve-dashboard` function, let's think about how this
should work. Babashka ships with [Selmer](https://github.com/yogthos/Selmer), a
nice template system, so let's add a `src/index.html` template, copying
liberally from Cyprien Pannier's blog post:

``` html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bulma@0.9.3/css/bulma.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css" integrity="sha512-1ycn6IcaQQ40/MKBW2W4Rhis/DbILU74C1vSrLJxCq57o941Ym01SwNsOMqvEBFlcgUa6xLiPY/NS5R+E6ztJQ==" crossorigin="anonymous" referrerpolicy="no-referrer" />

    <script src="https://cdn.jsdelivr.net/npm/vega@5.21.0"></script>
    <script src="https://cdn.jsdelivr.net/npm/vega-lite@5.2.0"></script>
    <script src="https://cdn.jsdelivr.net/npm/vega-embed@6.20.2"></script>

    <title>Site Analytics - Powered by Blambda!</title>
  </head>
  <body>
    <section class="hero is-link block">
      <div class="hero-body has-text-centered">
        <p class="title" style="vertical-align:baseline">
          <span class="icon">
            <i class="fas fa-chart-pie"></i>
          </span>
          &nbsp;Site Analytics - Powered by Blambda!
        </p>
        <p class="subtitle">{{date-label}}</p>
      </div>
    </section>
    <div class="container is-max-desktop">
      <div class="box">
        <nav class="level is-mobile">
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">Total views</p>
              <p class="title">{{total-views}}</p>
            </div>
          </div>
        </nav>
        <div>
          <div id="{{chart-id}}" style="width:100%; height:300px"></div>
          <script type="text/javascript">
           vegaEmbed ('#{{chart-id}}', JSON.parse({{chart-spec|json|safe}}));
          </script>
        </div>
      </div>
      <div class="box">
        <h1 class="title is-3">Top URLs</h1>
        <table class="table is-fullwidth is-hoverable is-striped">
          <thead>
            <tr>
              <th>Rank</th>
              <th>URL</th>
              <th>Views</th>
            </tr>
          </thead>
          <tbody>
            {% for i in top-urls %}
            <tr>
              <td style="width: 20px">{{i.rank}}</td>
              <td><a href="{{i.url}}">{{i.url}}</a></td>
              <td style="width: 20px">{{i.views}}</td>
            </tr>
            {% endfor %}
          </tbody>
        </table>
      </div>
    </div>
  </body>
</html>
```

Just consider this stuff an incantation if you don't feel like reading it. 😅

Once we have the template in place, we can write the `serve-dashboard` that
renders it. First, we need to require Selmer in `src/handler.clj`:

``` clojure
(ns handler
  (:require [cheshire.core :as json]
            [selmer.parser :as selmer]
            [page-views]
            [util :refer [->map]])
  (:import (java.time LocalDate)))
```

Now, we can just load and render the template in `serve-dashboard`:

``` clojure
(defn serve-dashboard [_event]
  (util/log "Rendering dashboard")
  {:statusCode 200
   :headers {"Content-Type" "text/html"}
   :body (selmer/render (slurp "index.html") {})})
```

Since we've added `index.html` as a source file, we need to add it to the
`:source-files` list in the top-level `bb-edn`:

``` clojure
{:deps { ... }
 :tasks
 {:requires ([blambda.cli :as blambda])
  :init (do
          (def config {:bb-arch "arm64"
                       :deps-layer-name "site-analyser-deps"
                       :lambda-name "site-analyser"
                       :lambda-handler "handler/handler"
                       :lambda-iam-role "${aws_iam_role.lambda.arn}"
                       :source-files [;; Clojure sources
                                      "handler.clj"
                                      "page_views.clj"
                                      "util.clj"

                                      ;; HTML templates
                                      "index.html"
                                      ]
                       :extra-tf-config ["tf/main.tf"]}))
  ...
```

Let's rebuild and redeploy:

``` text
$ bb blambda build-lambda

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Adding file: page_views.clj
Adding file: util.clj
Adding file: index.html
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
updating: handler.clj (deflated 66%)
updating: page_views.clj (deflated 59%)
updating: util.clj (deflated 35%)
  adding: index.html (deflated 59%)

[nix-shell:/tmp/site-analyser]$ bb blambda terraform apply

Terraform will perform the following actions:

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {
Plan: 0 to add, 1 to change, 0 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_lambda_function.lambda: Modifications complete after 7s [id=site-analyser]

Apply complete! Resources: 0 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

Now we can visit
https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/dashboard
in a web browser and see something like this:

![Site analytics dashboard showing no data][empty]

[empty]: assets/2023-01-04-dashboard-empty.png "Boring dashboard" width=800px

This is definitely a bit on the boring side, so let's write some code to query
DynamoDB and supply the data to make the dashboard dashing!

## Paging Doctor AWS again

Whenever we fetch data from an AWS API, we need to handle pagination. Luckily, I
have already [blogged about this extensively](2022-10-02-page-2.html), so we can
just copy and paste from my S3 paging example to accomplish the same with
DynamoDB. Let's start by adding the handy `lazy-concat` helper function to
`src/util.clj`:

``` clojure
(defn lazy-concat [colls]
  (lazy-seq
   (when-first [c colls]
     (lazy-cat c (lazy-concat (rest colls))))))
```

Now, we can add some code to `src/page_views.clj` to query DynamoDB in a
page-friendly way:

``` clojure
(defn get-query-page [{:keys [dynamodb views-table] :as client}
                      date
                      {:keys [page-num LastEvaluatedKey] :as prev}]
  (when prev
    (util/log "Got page" prev))
  (when (or (nil? prev)
            LastEvaluatedKey)
    (let [page-num (inc (or page-num 0))
          req (merge
               {:TableName views-table
                :KeyConditionExpression "#date = :date"
                :ExpressionAttributeNames {"#date" "date"}
                :ExpressionAttributeValues {":date" {:S date}}}
               (when LastEvaluatedKey
                 {:ExclusiveStartKey LastEvaluatedKey}))
          _ (util/log "Querying page views" (->map date page-num req))
          res (-> (aws/invoke dynamodb {:op :Query
                                        :request req})
                  validate-response)
          _ (util/log "Got response" (->map res))]
      (assoc res :page-num page-num))))

(defn get-views [client date]
  (->> (iteration (partial get-query-page client date)
                  :vf :Items)
       util/lazy-concat
       (map (fn [{:keys [views date url]}]
              {:date (:S date)
               :url (:S url)
               :views (util/->int (:N views))}))))
```

We might as well test this out in our REPL whilst we're here:

``` clojure
(comment

  (def c (client {:aws-region "eu-west-1", :views-table "site-analyser"}))

  (get-views c "2022-12-25")
  ;; => ({:date "2022-12-25", :url "https://example.com/page-01", :views 5}
  ;;     {:date "2022-12-25", :url "https://example.com/page-03", :views 8}
  ;;     {:date "2022-12-25", :url "https://example.com/page-04", :views 15}
  ;;     {:date "2022-12-25", :url "https://example.com/page-05", :views 3}
  ;;     {:date "2022-12-25", :url "https://example.com/page-06", :views 12}
  ;;     {:date "2022-12-25", :url "https://example.com/page-07", :views 11}
  ;;     {:date "2022-12-25", :url "https://example.com/page-08", :views 15}
  ;;     {:date "2022-12-25", :url "https://example.com/page-09", :views 8})

  )
```

Looks good!

Now let's plug this into `src/handler.clj`! The Vega library we're using to
render our data will attach to a `<div>` in our HTML page, to which we'll give a
random ID. In order to facilitate this, let's import `java.util.UUID`:

``` clojure
(ns handler
  (:require [cheshire.core :as json]
            [selmer.parser :as selmer]
            [page-views]
            [util :refer [->map]])
  (:import (java.time LocalDate)
           (java.util UUID)))
```

And whilst we're at it, let's add a little more config to control how many days
of data and how many top URLs to show:

``` clojure
(def config {:aws-region (get-env "AWS_REGION" "eu-west-1")
             :views-table (get-env "VIEWS_TABLE")
             :num-days (util/->int (get-env "NUM_DAYS" "7"))
             :num-top-urls (util/->int (get-env "NUM_TOP_URLS" "10"))})
```

Now we're ready to write the `serve-dashboard` function:

``` clojure
(defn serve-dashboard [{:keys [queryStringParameters] :as event}]
  (let [date (:date queryStringParameters)
        dates (if date
                [date]
                (->> (range (:num-days config))
                     (map #(str (.minusDays (LocalDate/now) %)))))
        date-label (or date (format "last %d days" (:num-days config)))
        all-views (mapcat #(page-views/get-views client %) dates)
        total-views (reduce + (map :views all-views))
        top-urls (->> all-views
                      (group-by :url)
                      (map (fn [[url views]]
                             [url (reduce + (map :views views))]))
                      (sort-by second)
                      reverse
                      (take (:num-top-urls config))
                      (map-indexed (fn [i [url views]]
                                     (assoc (->map url views) :rank (inc i)))))
        chart-id (str "div-" (UUID/randomUUID))
        chart-data (->> all-views
                        (group-by :date)
                        (map (fn [[date rows]]
                               {:date date
                                :views (reduce + (map :views rows))}))
                        (sort-by :date))
        chart-spec (json/generate-string
                    {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                     :data {:values chart-data}
                     :mark {:type "bar"}
                     :width "container"
                     :height 300
                     :encoding {:x {:field "date"
                                    :type "nominal"
                                    :axis {:labelAngle -45}}
                                :y {:field "views"
                                    :type "quantitative"}}})
        tmpl-vars (->map date-label
                         total-views
                         top-urls
                         chart-id
                         chart-spec)]
    (util/log "Rendering dashboard" tmpl-vars)
    {:statusCode 200
     :headers {"Content-Type" "text/html"}
     :body (selmer/render (slurp "index.html")
                          tmpl-vars)}))
```

A quick build and deploy and now we'll be able to see some exciting data!

``` test
$ bb blambda build-lambda

Building lambda artifact: /tmp/site-analyser/target/site-analyser.zip
Adding file: handler.clj
Adding file: page_views.clj
Adding file: util.clj
Adding file: index.html
Compressing lambda: /tmp/site-analyser/target/site-analyser.zip
updating: handler.clj (deflated 66%)
updating: page_views.clj (deflated 65%)
updating: util.clj (deflated 42%)
updating: index.html (deflated 59%)

[nix-shell:/tmp/site-analyser]$ bb blambda terraform apply
Terraform will perform the following actions:

  # aws_lambda_function.lambda will be updated in-place
  ~ resource "aws_lambda_function" "lambda" {

Plan: 0 to add, 1 to change, 0 to destroy.

Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes

aws_lambda_function.lambda: Modifying... [id=site-analyser]
aws_lambda_function.lambda: Still modifying... [id=site-analyser, 10s elapsed]
aws_lambda_function.lambda: Modifications complete after 11s [id=site-analyser]

Apply complete! Resources: 0 added, 1 changed, 0 destroyed.

Outputs:

function_url = "https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/"
```

Visiting
https://kuceaiz55k7soeki4u5oy4w6uy0ntbky.lambda-url.eu-west-1.on.aws/dashboard
again tells a tale of joy!

![Site analytics dashboard lots of data][data]

[data]: assets/2023-01-04-dashboard-full.png "Dashing dashboard" width=800px

With this, let's declare our site analysed and all agree that Babashka is way
better than nbb. Hurrah! 🎉
