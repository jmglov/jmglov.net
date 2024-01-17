Title: Dogfooding Blambda 5: To parse—perchance to dream
Date: 2022-09-01
Tags: clojure,blambda,aws
Description: In which Blambda and I get serious about access logs.
Image: assets/2022-09-02-preview.png
Image-Alt: A French Bulldog stands next to a bowl of food, staring mournfully at the camera
Discuss: https://clojurians.slack.com/archives/C04PSBFMMDJ/

In the last instalment of [Dogfooding Blambda](tags/blambda.html), we dove into
[the details of how the new command line interface
works](2022-08-11-dogfooding-blambda-cli-ier.html). This is all well and good,
but it doesn't directly help us along in our lofty goal of parsing access logs
for my blog. Let's stop yak shaving and get back into it!

## Talking to S3

The first order of business is figuring out how to grab the access logs from S3.
As previously noted, I decided to use
[awyeah-api](https://github.com/grzm/awyeah-api), so let's add it to our
`bb.edn`:

``` clojure
{:paths ["."]
 :deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.206"}
        com.cognitect.aws/s3 {:mvn/version "822.2.1109.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "0fa7dd51f801dba615e317651efda8c597465af6"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "433b0778e2c32f4bb5d0b48e5a33520bee28b906"}}}
```

After doing this, we need to build and deploy a new deps layer with Blambda:

``` text
$ bb blambda build-deps-layer --deps-path src/bb.edn
[...]
Compressing custom runtime layer: target/deps.zip

$ bb blambda deploy-deps-layer --deps-layer-name s3-log-parser-deps
Publishing layer version for layer s3-log-parser-deps
Published layer arn:aws:lambda:eu-west-1:123456789100:layer:s3-log-parser-deps:2
```

If you'll cast your mind back a few weeks to last time we were messing around
with a lamdbda handler, you may recall that we started with something like this:

``` clojure
(ns s3-log-parser)

(defn handler [event context]
  (prn {:msg "Invoked with event",
        :data {:event event}})
  {"statusCode" 200
   "body" "Hello, Blambda!"})
```

Again, no judgement—we've gotta start somewhere—but this doesn't have much to do
with S3. Let's remedy that!

``` clojure
(ns s3-log-parser
  (:require [com.grzm.awyeah.client.api :as aws]))

(comment

  ;; Don't print this!
  (def s3 (aws/client {:api :s3, :region "eu-west-1"}))
  ;; => #'s3-log-parser/s3

  ;; Would be so useful but there are too many ops for S3 😭
  (aws/ops s3)

  ;; Prints instead of returning an object; check your REPL buffer
  (aws/doc s3 :ListObjectsV2)
  
  )
```

After requiring the awyeah-api client in our namespace, we'll open up a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment)
and start evaluating forms right in our source buffer! This of course assumes
that you've started a
[Babashka](https://github.com/babashka/babashka) REPL by doing something along
the lines of

``` text
bb nrepl-server
```

and you're using something along the lines of
[CIDER](https://docs.cider.mx/cider/index.html) and you've told your editor
(Emacs, I hope) to connect to the Babashka REPL. The next step is to evaluate
the buffer (**C-c C-k** in CIDER, which runs `cider-load-buffer`) so that the
namespace is loaded.

Once you've done that stuff, putting your cursor at the end of a form and
hitting **C-c C-v f c e** (`cider-pprint-eval-last-sexp-to-comment`)—or the
equivalent in your editor—should result in the form being evaluated and the
result being printed back to your code buffer. That's what the

``` clojure
;; => #'s3-log-parser/s3
```

stuff is in my code blocks. Whenever you see `;; => `, you'll know that is the
result of evaluating a form. It's a really nice way to do REPL-driven
development; you can do little experiments right next to the bit of code you're
working on, and it's easy to copy and paste bits of those experiments into
functions and so on.

The first thing we're doing inside the comment is creating an S3 client using
the
[aws/client](https://cognitect-labs.github.io/aws-api/cognitect.aws.client.api-api.html#cognitect.aws.client.api/client)
function. The reason I have that little note not to print the result is that
`aws/client` returns this big data structure describing all of the stuff the
client can do, which in the case of S3 takes about 2000 lines to print, which
takes CIDER a few seconds to do and really gunks up my code buffer. So instead
of **C-c C-v f c e**, I do **C-c C-e** (`cider-eval-last-sexp`), which evaluates
the form and prints the result in a temporary popup thingy instead of as a
comment back into my code buffer.

The
[aws/ops](https://cognitect-labs.github.io/aws-api/cognitect.aws.client.api-api.html#cognitect.aws.client.api/ops)
function is also really useful for clients that are a bit smaller than S3's. It
prints out all of the operations a client can perform, along with the parameters
and return values of the operation. Again, it is too much to print here. 😭

The
[aws/doc](https://cognitect-labs.github.io/aws-api/cognitect.aws.client.api-api.html#cognitect.aws.client.api/doc)
function can help us out, though. It takes a client and an operation and prints
out a nice documentation string. If I move my cursor to the end of the form and
do a nice little **C-c C-e**, I get the following printed into my REPL buffer:

``` text
-------------------------
ListObjectsV2

<p>Returns some or all (up to 1,000) of the objects in a bucket with each request.
You can use the request parameters as selection criteria to return a subset of
the objects in a bucket. A <code>200 OK</code> response can contain valid or
invalid XML. Make sure to design your application to parse the contents of the
[...]
</p> </li> </ul>

-------------------------
Request

{:Prefix string,
 :StartAfter string,
 :Bucket string,
 :EncodingType [:one-of ["url"]],
 :Delimiter string,
 :FetchOwner boolean,
 :RequestPayer [:one-of ["requester"]],
 :ContinuationToken string,
 :MaxKeys integer,
 :ExpectedBucketOwner string}

Required

[:Bucket]

-------------------------
Response

{:Prefix string,
 :StartAfter string,
 :EncodingType [:one-of ["url"]],
 :Delimiter string,
 :NextContinuationToken string,
 :CommonPrefixes [:seq-of {:Prefix string}],
 :ContinuationToken string,
 :Contents
 [:seq-of
  {:Key string,
   :LastModified timestamp,
   :ETag string,
   :ChecksumAlgorithm
   [:seq-of [:one-of ["CRC32" "CRC32C" "SHA1" "SHA256"]]],
   :Size integer,
   :StorageClass
   [:one-of
    ["STANDARD"
     "REDUCED_REDUNDANCY"
     "GLACIER"
     "STANDARD_IA"
     "ONEZONE_IA"
     "INTELLIGENT_TIERING"
     "DEEP_ARCHIVE"
     "OUTPOSTS"
     "GLACIER_IR"]],
   :Owner {:DisplayName string, :ID string}}],
 :MaxKeys integer,
 :IsTruncated boolean,
 :Name string,
 :KeyCount integer}
```

OK, that's pretty cool. Most of the time, however, I find it easier to just use
the [AWS API
documentation](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html).

Anyway, we've now figured out that we want to use the `ListObjectsV2` operation,
and we know from the docs that it needs a bucket and can take a key prefix to
return only objects in a certain folder hierarchy, as it were. We can also add a
maximum number of objects to return for testing purposes.

At this point, we have all the information we need to write a function to list
S3 objects with a certain prefix, so we could go ahead and slap it in our code.
However, I don't ever trust myself to have read the docs correctly, so I always
just try things out in my REPL first to see what happens:

``` clojure
(comment

  (aws/invoke s3 {:op :ListObjectsV2
                  :request {:Bucket "logs.jmglov.net"
                            :Prefix "logs/2022-06-26"
                            :MaxKeys 2}})
  ;; => {:Prefix "logs/2022-06-26",
  ;;     :NextContinuationToken
  ;;     "1ClCd7jr58LZ6u1Ts2UAEknPY2/jktCdH8LS3lGtB7oJskG+l1K4+97f+KCSZ6AhI69gyHv9phN8L8nKnbkqTxc/NT/GWdz9A",
  ;;     :Contents
  ;;     [{:Key "logs/2022-06-26-00-14-34-7526D4F869A33078",
  ;;       :LastModified #inst "2022-06-26T00:14:35.000-00:00",
  ;;       :ETag "\"815876abf870fa05421183daa60a0513\"",
  ;;       :Size 366,
  ;;       :StorageClass "STANDARD"}
  ;;      {:Key "logs/2022-06-26-00-14-36-F1E7815BC3A92960",
  ;;       :LastModified #inst "2022-06-26T00:14:37.000-00:00",
  ;;       :ETag "\"91b2ceef95e28d72384ff55b77453a71\"",
  ;;       :Size 383,
  ;;       :StorageClass "STANDARD"}],
  ;;     :MaxKeys 2,
  ;;     :IsTruncated true,
  ;;     :Name "logs.jmglov.net",
  ;;     :KeyCount 2}
  
  )
```

Nice! I got the request right on the first try, and now we know what the
response looks like. The power of REPL-driven development is evident here, in
that I'm playing with real data, not copying and pasting from an example
response or something like that. Documentation sometimes lies, but real services
don't.

OK, so now we know how to use `ListObjectsV2`, so let's turn this stuff into a
proper function that returns all of the log objects for a specific date. Let's
also remove the hard-coded stuff and put it in a `config` map:

``` clojure
(def config {:region "eu-west-1"
             :s3-bucket "logs.jmglov.net"
             :s3-prefix "logs/"})

(defn list-logs [date]
  (let [{:keys [region s3-bucket s3-prefix]} config
        request {:Bucket s3-bucket
                 :Prefix (format "%s%s" s3-prefix date)}
        response (aws/invoke s3 {:op :ListObjectsV2
                                 :request request})]
    (prn {:msg "Listing log files", :data request})
    (prn {:msg "Response", :data response})
    (->> response
         :Contents
         (map :Key))))
```

Having written a shiny new function, we'll want to prove to ourselves that it
works by testing it out. We could write a unit test (OK, it's not really a unit
test, but you get what I mean), but that would require setting up test machinery
which we currently don't have, which sounds quite distracting. How's about we
embrace the REPL and just test stuff out in a comment block right below the
function we've written?

The first step is loading the new code into our environment, which we can do by
evaluating the buffer again (**C-c C-k** or your editor's equivalent). Having
done that, we write a function call and do the eval and print dance (**C-c C-v f
c e** or your editor's equivalent):

``` clojure
(comment

  (list-logs "2022-06-28")
  ;; => ("logs/2022-06-28-00-12-36-5E32B8C5369C818E"
  ;;     "logs/2022-06-28-00-15-28-989878A1E585B6F5")

  )
```

Awesome, the function works as designed! 🎉

## Talking back

Now that we know how to list objects, let's make the lambda do the business!
That requires us to do the following:
1. Modify our `config` map to load configuration from the environment
2. Print a nice little log statement that lets us know that the Lambda is cold
   starting (any code that is outside the handler function will only run when a
   new lambda instance is started; after that, the namespace is already loaded,
   so the runtime will just call the handler function for subsequent requests)
3. Create an S3 client (again, outside the handler function so the S3 client
   will persist across requests; this is a common pattern for expensive
   operations like creating clients or populating caches or whatever)
4. Making our handler function call `list-logs`, then JSON-encode the result
   into the lambda's response body 

Here's what that looks like:

``` clojure
(ns s3-log-parser
  (:require [com.grzm.awyeah.client.api :as aws]
            [cheshire.core :as json]))

(def config
  {:region (System/getenv "AWS_REGION")
   :s3-bucket (System/getenv "S3_BUCKET")
   :s3-prefix (System/getenv "S3_PREFIX")})

(prn {:msg "Lambda starting", :data {:config config}})

(def s3 (aws/client {:api :s3, :region (:region config)}))

(defn list-logs [date]
  (let [{:keys [region s3-bucket s3-prefix]} config
        request {:Bucket s3-bucket
                 :Prefix (format "%s%s" s3-prefix date)}
        response (aws/invoke s3 {:op :ListObjectsV2
                                 :request request})]
    (prn {:msg "Listing log files", :data request})
    (prn {:msg "Response", :data response})
    (->> response
         :Contents
         (map :Key))))

(defn handler [event _context]
  (prn {:msg "Invoked with event", :data {:event event}})
  (let [logs (list-logs "2022-06-26")]
    {"statusCode" 200
     "body" (json/encode {:logs logs})}))
```

Again, let's prove this works before actually deploying a lambda. We'll evaluate
the buffer to pick up the new handler, but now we might have a slight issue:

``` clojure
(comment

  config
  ;; => {:region nil, :s3-bucket nil, :s3-prefix nil}

  )
```

The `config` map is being initialised from environment variables that we haven't
set. We could set the environment variables and then restart our REPL, but that
doesn't feel like the Clojure way. Why don't we just re-define `config` and the
S3 client in our comment block instead?

``` clojure
(comment

  (def config {:region "eu-west-1"
               :s3-bucket "logs.jmglov.net"
               :s3-prefix "logs/"})
  ;; => #<Var@6c7f283b:
  ;;      {:region "eu-west-1", :s3-bucket "logs.jmglov.net", :s3-prefix "logs/"}>

  ;; Don't print this! Use C-c C-e instead.
  (def s3 (aws/client {:api :s3, :region (:region config)}))

  )
```

Now we can actually test the handler function in our REPL:

``` clojure
(comment

  (handler {} {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-06-26-00-14-34-7526D4F869A33078\",\"logs/2022-06-26-00-14-36-F1E7815BC3A92960\"]}"}

  )
```

Looks like it works! Let's try it out as an actual lambda!

If we open the [AWS Lambda
console](https://eu-west-1.console.aws.amazon.com/lambda/#/functions) and click
on our s3-log-parser function, we now need to do three things:
1. Update the configuration to use the new version of the s3-log-parser-deps
   layer that we previously deployed
2. Set the environment variables that our code is expecting
3. Update the code itself

For the first step, let's take a look at the **Function overview** section in
the console:

![Screenshot of the Function overview section in the Lambda
console](assets/2022-09-02-overview.png "Click on Layers" width=800px class=border)

If we click on the "Layers" icon and then **Edit**, we can see that our function
is currently using two layers: the blambda runtime and the deps layer:

![Screenshot of the Edit layers page in the Lambda console](assets/2022-09-02-layers.png "Change deps layer version and save" width=800px class=border)

We need to change the version of the `s3-log-parser-deps` to 2 and then click
**Save**.

Now that our deps are updated, let's set the environment variables. If we click
on the **Configuration** tab just under the function overview, we see
**Environment variables** on the left hand side. Clicking that reveals that we
have no environment variables currently set:

![Screenshot of the Configuration tab in the Lambda
console](assets/2022-09-02-env.png "Click Edit" width=800px class=border)

Let's click the **Edit** button and add the `S3_BUCKET` and `S3_PREFIX`
environment variables (`AWS_REGION` is set by the Lambda instance itself, so we
don't need to set it):

![Screenshot of the Edit environment variables page in the Lambda
console](assets/2022-09-02-env-filled.png "Click Save" width=800px class=border)

And now it's time for the code itself. Let's click on the **Code** tab, then
just copy and paste the contents of our editor's code buffer over top of the
code in `s3_log_parser.clj`:

![Screenshot of the Code source tab in the Lambda
console](assets/2022-09-02-code1.png "Click Deploy" width=800px class=border)

The really cool thing about Rich comments is that we don't need to remove them
from our source code, since they're not evaluated when loading the namespace!

Now we click **Deploy** to get the new code out there, and there's only one
thing standing between us and glory: configuring a test event. In order to do
this, we need to click the dropdown on the **Test** button, then select **Create
new event**. We can fill in something like "test" for the **Event name**, leave
the **Event sharing settings** to the default of "Private", and then click on
the **Template** dropdown and select "API Gateway AWS Proxy" (since we intend
this lambda to be invoked using a [function
URL](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html), which uses
the same input payload as AWS Gateway):

![Screenshot of the Configure test event window in the Lambda
console](assets/2022-09-02-test-event.png "Click Save" width=800px class=border)

If we scroll down and click **Save**, we're in business! Now we can click the
**Test** button and revel in our success:

![Screenshot of the Execution results tab in the Lambda
console](assets/2022-09-02-test1.png "Victory!" width=800px class=border)

## Accepting input

As amazing as all this is, it is a little limiting that our lambda only lists
logs for 2022-06-26. How about we accept the date as a query string parameter
instead? Looking at the [AWS Lambda proxy integration
payload](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html),
we can see that the query parameters are exposed in a `queryStringParameters`
object in the event. Let's grab that and use it as the `date` parameter to `list-logs`:

``` clojure
(defn handler [event _context]
  (prn {:msg "Invoked with event", :data {:event event}})
  (let [date (get-in event ["queryStringParameters" "date"])
        logs (list-logs date)]
    {"statusCode" 200
     "body" (json/encode {:logs logs})}))

(comment

  (def event {"queryStringParameters" {"date" "2022-09-02"}})
  ;; => #<Var@4262c861: {"queryStringParameters" {"date" "2022-09-02"}}>

  (handler event {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-09-02-00-12-42-F789B0CC4E3D8814\",\"logs/2022-09-02-00-13-14-534A4D7BB857B5D3\"]}"}

  )
```

This whole `(prn {:msg ...})` thing is getting a bit tiring, so let's add a
`log` function instead. We might as well also add the timestamp to the log
message whilst we're at it:

``` clojure
(ns s3-log-parser
  (:require [cheshire.core :as json]
            [com.grzm.awyeah.client.api :as aws])
  (:import (java.time Instant)))

(defn log [msg data]
  (prn {:msg msg
        :data data
        :timestamp (str (Instant/now))}))
```

OK, this is moving in the right direction, but what if someone sends in the date
in the wrong format?

``` clojure
(comment

  (handler {"queryStringParameters" {"date" "2/9/2022"}} {})
  ;; => {"statusCode" 200, "body" "{\"logs\":[]}"}

  )
```

I mean, this is technically correct, but it would be much more friendly to
actually let the caller know that they did something wrong. Let's parse the date
to be sure it's valid before handing it off to `list-logs`:

```clojure
(ns s3-log-parser
  (:require [cheshire.core :as json]
            [com.grzm.awyeah.client.api :as aws])
  (:import (java.time Instant
                      LocalDate)))

(defn get-date [date-str]
  (try
    (LocalDate/parse date-str)
    (catch Exception _
      (let [msg (format "Invalid date: %s" date-str)
            data {:date date-str}]
        (log msg data)
        (throw (ex-info msg data))))))

(comment

  (get-date "2/9/2022")
  ;; => : Invalid date: 2/9/2022 s3-log-parser 

  )
```

Cool. There's another annoyance here, though: we're logging the error and then
throwing an exception with the exact same data. Let's create a function to do
this for us, and then we can be more concise in `get-date`:

``` clojure
(defn error [msg data]
  (log msg data)
  (throw (ex-info msg data)))

(defn get-date [date-str]
  (try
    (LocalDate/parse date-str)
    (catch Exception _
      (error (format "Invalid date: %s" date-str) {:date date-str}))))

(comment

  (get-date "2022-06-28")
  ;; => #object[java.time.LocalDate 0x502a28d3 "2022-06-28"]

  (get-date "2/9/2022")
  ;; => : Invalid date: nope s3-log-parser 

  )
```

If we think a little about our API, it seems probable that someone could call
it without providing a date parameter, in which case it seems reasonable to get
today's logs. Let's update `get-date` accordingly:

``` clojure
(defn get-date [date-str]
  (if date-str
    (try
      (LocalDate/parse date-str)
      (catch Exception _
        (error (format "Invalid date: %s" date-str) {:date date-str})))
    (LocalDate/now)))

(comment

  (get-date nil)
  ;; => #object[java.time.LocalDate 0x1b2b5155 "2022-09-02"]

  )
```

Now that `get-date` can throw an exception, we should handle that by returning a
400:

``` clojure
(defn handler [event _context]
  (log "Invoked with event" {:event event})
  (try
    (let [date (get-date (get-in event ["queryStringParameters" "date"]))
          logs (list-logs date)]
      {"statusCode" 200
       "body" (json/encode {:logs logs})})
     (catch Exception e
       (log (ex-message e) (ex-data e))
       {"statusCode" 400
        "body" (ex-message e)}))))

(comment

  (handler {"queryStringParameters" {"date" "2022-06-28"}} {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-06-28-00-12-36-5E32B8C5369C818E\",\"logs/2022-06-28-00-15-28-989878A1E585B6F5\"]}"}

  (handler {} {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-09-01-00-12-48-36034402F760D842\",\"logs/2022-09-01-00-13-47-48D19FF5B34710F9\"]}"}

  )
```

## Gettin' loggy wit' it

Alright, we now have a lambda that can list logs. However, that's not really
what we set out to do; we actually wanted to parse the logs and return the data
contained therein. Of course, a necessary precursor to parsing the logs is
actually retrieving them from S3. Looking at the [S3 API
documentation](https://docs.aws.amazon.com/AmazonS3/latest/API/API_Operations.html),
the
[GetObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html)
operation looks promising. Let's try it out!

``` clojure
(comment

  (aws/invoke s3 {:op :GetObject
                  :request {:Bucket (:s3-bucket config)
                            :Key "logs/2022-06-28-00-15-28-989878A1E585B6F5"}})
  ;; => {:LastModified #inst "2022-06-28T00:15:29.000-00:00",
  ;;     :ETag "\"59877f6538514fe39b6874d9220c10a6\"",
  ;;     :Metadata {},
  ;;     :ServerSideEncryption "AES256",
  ;;     :ContentLength 379,
  ;;     :ContentType "text/plain",
  ;;     :AcceptRanges "bytes",
  ;;     :Body
  ;;     #object[java.io.BufferedInputStream 0x61636398 "java.io.BufferedInputStream@61636398"]}

  )
```

OK, so `GetObject` returns the contents of the object as a
`BufferedInputStream`. In order to do something reasonable with that, we need to
wrap it in some sort of reader, which we can do with [clojure.java.io/reader](https://clojuredocs.org/clojure.java.io/reader):

``` clojure
(ns s3-log-parser
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [com.grzm.awyeah.client.api :as aws])
  (:import (java.time Instant
                      LocalDate)))

(comment
  (->> (aws/invoke s3 {:op :GetObject
                       :request {:Bucket (:s3-bucket config)
                                 :Key "logs/2022-06-28-00-15-28-989878A1E585B6F5"}})
       :Body
       io/reader)
  ;; => #object[java.io.BufferedReader 0x7aac737b "java.io.BufferedReader@7aac737b"]

  (->> (aws/invoke s3 {:op :GetObject
                       :request {:Bucket (:s3-bucket config)
                                 :Key "logs/2022-06-28-00-15-28-989878A1E585B6F5"}})
       :Body
       io/reader
       slurp)
  ;; => "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:44:08 +0000] 64.252.68.192 - DC5849267MERJ0J5 WEBSITE.GET.OBJECT blog/atom.xml \"GET /blog/atom.xml HTTP/1.1\" 304 - - 87380 25 - \"-\" \"Amazon CloudFront\" - xm6O1t0x9H8Opu44YrFM/PxX/SUFizd4pS1/FJ/oJqdywpkzcmqx/N7Kv2eMF1Ij+32rM17h/HQ= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -\n"

  )
```

Reading the contents into a string with
[slurp](https://clojuredocs.org/clojure.core/slurp) is cool, but it would be
nicer if we can get a sequence of lines. There's a helpful function in
clojure.core called [line-seq](https://clojuredocs.org/clojure.core/line-seq),
which does just that. Even better, it can also consume from a reader!

``` clojure
(comment

  (->> (aws/invoke s3 {:op :GetObject
                       :request {:Bucket (:s3-bucket config)
                                 :Key "logs/2022-06-28-00-15-28-989878A1E585B6F5"}})
       :Body
       io/reader
       line-seq)
  ;; => ("022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:44:08 +0000] 64.252.68.192 - DC5849267MERJ0J5 WEBSITE.GET.OBJECT blog/atom.xml \"GET /blog/atom.xml HTTP/1.1\" 304 - - 87380 25 - \"-\" \"Amazon CloudFront\" - xm6O1t0x9H8Opu44YrFM/PxX/SUFizd4pS1/FJ/oJqdywpkzcmqx/N7Kv2eMF1Ij+32rM17h/HQ= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -")

  )
```

Now that we've learned how to grab lines from a log, let's put it in a function:

``` clojure
(defn get-log-lines [s3-key]
  (->> (aws/invoke s3 {:op :GetObject
                       :request {:Bucket (:s3-bucket config)
                                 :Key s3-key}})
       :Body
       io/reader
       line-seq))

(comment

  (get-log-lines "logs/2022-06-28-00-15-28-989878A1E585B6F5")
  ;; => ("022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:44:08 +0000] 64.252.68.192 - DC5849267MERJ0J5 WEBSITE.GET.OBJECT blog/atom.xml \"GET /blog/atom.xml HTTP/1.1\" 304 - - 87380 25 - \"-\" \"Amazon CloudFront\" - xm6O1t0x9H8Opu44YrFM/PxX/SUFizd4pS1/FJ/oJqdywpkzcmqx/N7Kv2eMF1Ij+32rM17h/HQ= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -")

  )
```

Now we can glue together what we've done so far to get all the log lines for a
specific date!

``` clojure
(comment

  (->> (list-logs "2022-06-28")
       (mapcat get-log-lines))
  ;; => ("022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -"
  ;;     "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:44:08 +0000] 64.252.68.192 - DC5849267MERJ0J5 WEBSITE.GET.OBJECT blog/atom.xml \"GET /blog/atom.xml HTTP/1.1\" 304 - - 87380 25 - \"-\" \"Amazon CloudFront\" - xm6O1t0x9H8Opu44YrFM/PxX/SUFizd4pS1/FJ/oJqdywpkzcmqx/N7Kv2eMF1Ij+32rM17h/HQ= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -")

  )
```

Now we're getting somewhere!

## Errors and taxes

Of course, we haven't taken into account the fact that calls to S3 can fail, and
of course anything that **can** fail **will** eventually fail. Let's see what
happens if we feed `GetObject` a key that doesn't exist:

``` clojure
(comment

  (aws/invoke s3 {:op :GetObject
                  :request {:Bucket (:s3-bucket config)
                            :Key "NOPE!"}})
  ;; => {:Error
  ;;     {:HostIdAttrs {},
  ;;      :KeyAttrs {},
  ;;      :Message "The specified key does not exist.",
  ;;      :Key "NOPE!",
  ;;      :CodeAttrs {},
  ;;      :RequestIdAttrs {},
  ;;      :HostId
  ;;      "Qfz+CzbvxySHBLdd1vIfX8rd8dpkgl1fnlxLTWSHGdtt77jqh/n9EJNzHiqKhZeqqMWP+ZUMRBQ=",
  ;;      :MessageAttrs {},
  ;;      :RequestId "0TESGW38EP6NQ6XK",
  ;;      :Code "NoSuchKey"},
  ;;     :ErrorAttrs {},
  ;;     :cognitect.anomalies/category :cognitect.anomalies/not-found}

  )
```

OK, this is straightforward enough to handle. We'll just check for an `:Error`
key in the response and call our `error` function to log and throw:

``` clojure
(defn get-log-lines [s3-key]
  (let [resp (aws/invoke s3 {:op :GetObject
                             :request {:Bucket (:s3-bucket config)
                                       :Key s3-key}})]
    (if (:Error resp)
      (error (-> resp :Error :Message) (:Error resp))
      (->> resp
           :Body
           io/reader
           line-seq))))

(comment

  (get-log-lines "NOPE!")
  ;; => : The specified key does not exist. s3-log-parser

  )
```

It is a little distracting to have this if / else conditional in the middle of
our otherwise pristine `get-log-lines` function, however. Let's pull error
handling out into a separate function:

``` clojure
(defn handle-error [{err :Error :as resp}]
  (if err
    (error (:Message err) err)
    resp))

(comment

  (handle-error
   {:Error
    {:HostIdAttrs {},
     :KeyAttrs {},
     :Message "The specified key does not exist.",
     :Key "NOPE!",
     :CodeAttrs {},
     :RequestIdAttrs {},
     :HostId
     "Qfz+CzbvxySHBLdd1vIfX8rd8dpkgl1fnlxLTWSHGdtt77jqh/n9EJNzHiqKhZeqqMWP+ZUMRBQ=",
     :MessageAttrs {},
     :RequestId "0TESGW38EP6NQ6XK",
     :Code "NoSuchKey"},
    :ErrorAttrs {},
    :cognitect.anomalies/category :cognitect.anomalies/not-found})
  ;; => : The specified key does not exist. s3-log-parser

  (handle-error {:foo 1})
  ;; => {:foo 1}

  )
```

Now we can keep `get-log-lines` focused on the happy path:

``` clojure
(defn get-log-lines [s3-key]
  (->> (aws/invoke s3 {:op :GetObject
                       :request {:Bucket (:s3-bucket config)
                                 :Key s3-key}})
       handle-error
       :Body
       io/reader
       line-seq))
```

## Picking up the pieces

We now have what we need to read logs, so let's update the handler to do just
that:

``` clojure
(defn handler [event _context]
  (log "Invoked with event" {:event event})
  (try
    (let [date (get-date (get-in event ["queryStringParameters" "date"]))
          logs (list-logs date)
          log-lines (mapcat get-log-lines logs)]
      {"statusCode" 200
       "body" (json/encode {:logs logs
                            :lines log-lines})})
     (catch Exception e
       (log (ex-message e) (ex-data e))
       {"statusCode" 400
        "body" (ex-message e)}))))

(comment

  (handler {} {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-09-01-00-12-48-36034402F760D842\",\"logs/2022-09-01-00-13-47-48D19FF5B34710F9\"],\"lines\":[\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [31/Aug/2022:23:40:32 +0000] 64.252.89.133 - BC0KZGTDKR1QP07G WEBSITE.HEAD.OBJECT blog/2022-08-26-doing-software-wrong.html \\\"HEAD /blog/2022-08-26-doing-software-wrong.html HTTP/1.1\\\" 304 - - 7536 34 - \\\"-\\\" \\\"Amazon CloudFront\\\" - ApxkRhuJl/C73ZiR70xT6Stn2b1RkIcDPMnapeH5kWWQ+mT41qXfNeLaqpMc3j+5WCnrqoJH8N0= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -\",\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [31/Aug/2022:23:14:14 +0000] 64.252.73.234 - M987YV3YB7DHBWA3 WEBSITE.GET.OBJECT index.html \\\"GET / HTTP/1.1\\\" 304 - - 3459 18 - \\\"-\\\" \\\"Amazon CloudFront\\\" - 14MYY/WyblbL1WgULsK86Cwwtn+tHCOgs+Y98xIkC/EIwkqMeN/SWpBsF6x2gC1Tir7DYRc/+Zk= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -\"]}"}


  )
```

It's neat to be able to test the handler in the REPL, but it is a little
annoying to have to always invoke it like `(handler {} {})` if we just want an
empty event. Let's use Clojure's multi-arity functions to define a `handler/0`
and a `handler/1` (the way you reference functions of different arities in
Erlang is simply the best; read `handler/0` as "a function `handler` taking 0
arguments", and `handler/1` as "a function `handler` taking 1 argument").

``` clojure
(defn handler
  ([]
   (handler {} {}))
  ([event]
   (handler event {}))
  ([event _context]
   (log "Invoked with event" {:event event})
   (try
     (let [date (get-date (get-in event ["queryStringParameters" "date"]))
           logs (list-logs date)
           log-lines (mapcat get-log-lines logs)]
       {"statusCode" 200
        "body" (json/encode {:logs logs
                             :lines log-lines})})
     (catch Exception e
       (log (ex-message e) (ex-data e))
       {"statusCode" 400
        "body" (ex-message e)}))))

(comment

  (handler)
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"logs\":[\"logs/2022-09-01-00-12-48-36034402F760D842\",\"logs/2022-09-01-00-13-47-48D19FF5B34710F9\"],\"lines\":[\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [31/Aug/2022:23:40:32 +0000] 64.252.89.133 - BC0KZGTDKR1QP07G WEBSITE.HEAD.OBJECT blog/2022-08-26-doing-software-wrong.html \\\"HEAD /blog/2022-08-26-doing-software-wrong.html HTTP/1.1\\\" 304 - - 7536 34 - \\\"-\\\" \\\"Amazon CloudFront\\\" - ApxkRhuJl/C73ZiR70xT6Stn2b1RkIcDPMnapeH5kWWQ+mT41qXfNeLaqpMc3j+5WCnrqoJH8N0= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -\",\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [31/Aug/2022:23:14:14 +0000] 64.252.73.234 - M987YV3YB7DHBWA3 WEBSITE.GET.OBJECT index.html \\\"GET / HTTP/1.1\\\" 304 - - 3459 18 - \\\"-\\\" \\\"Amazon CloudFront\\\" - 14MYY/WyblbL1WgULsK86Cwwtn+tHCOgs+Y98xIkC/EIwkqMeN/SWpBsF6x2gC1Tir7DYRc/+Zk= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -\"]}"}

  )
```

OK, we now have something worth pasting into the Lambda console and testing out!

![Screenshot of the AWS Lambda console showing a successful test
result](assets/2022-09-02-test2.png "The sweet smell of victory!" width=800px class=border)

## What does it all mean?

There's one final thing to do: actually parse the log lines. If we take a look
at the [Amazon S3 server access log
format](https://docs.aws.amazon.com/AmazonS3/latest/userguide/LogFormat.html)
documentation, we can see how we need to parse this thing. And for parsing
strings, the obvious place to turn is a regular expression!

``` clojure
(comment

  (re-seq #"^(\S+) (\S+) \[([^]]+)\] (\S+) (\S+) (\S+) (\S+) (\S+) \"([^\"]+)\" (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) \"([^\"]+)\" \"([^\"]+)\" (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) (\S+).*$"
          "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -")
  ;; => (["022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -"
  ;;      "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"
  ;;      "jmglov.net"
  ;;      "27/Jun/2022:23:30:23 +0000"
  ;;      "64.252.88.38"
  ;;      "-"
  ;;      "DA5918VPJFK9A654"
  ;;      "WEBSITE.GET.OBJECT"
  ;;      "blog/2022-06-21-todo-list.html"
  ;;      "GET /blog/2022-06-21-todo-list.html HTTP/1.1"
  ;;      "304"
  ;;      "-"
  ;;      "-"
  ;;      "5447"
  ;;      "32"
  ;;      "-"
  ;;      "-"
  ;;      "Amazon CloudFront"
  ;;      "-"
  ;;      "ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI="
  ;;      "-"
  ;;      "-"
  ;;      "-"
  ;;      "jmglov.net.s3-website-eu-west-1.amazonaws.com"
  ;;      "-"
  ;;      "-"])

  )
```

Amazing! But also not amazing. Regular expressions get a bad name for being
cryptic and brittle, so let's see if we can use the power of Clojure to make
this one accessible and resilient!

We can define a variable that holds both the name of the field and the pattern
used to match it:

``` clojure
(ns s3-log-parser
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]
            [com.grzm.awyeah.client.api :as aws])
  (:import (java.time Instant
                      LocalDate)))

(comment

  (def log-fields
    (->> [:bucket-owner "(\\S+)"
          :bucket "(\\S+)"
          :time "\\[([^]]+)\\]"
          :remote-ip "(\\S+)"
          :requester "(\\S+)"
          :request-id "(\\S+)"
          :operation "(\\S+)"
          :key "(\\S+)"
          :request-uri "\"([^\"]+)\""
          :http-status "(\\S+)"
          :error-code "(\\S+)"
          :bytes-sent "(\\S+)"
          :object-size "(\\S+)"
          :total-time "(\\S+)"
          :turn-around-time "(\\S+)"
          :referer "\"([^\"]+)\""
          :user-agent "\"([^\"]+)\""
          :version-id "(\\S+)"
          :host-id "(\\S+)"
          :signature-version "(\\S+)"
          :cipher-suite "(\\S+)"
          :authentication-type "(\\S+)"
          :host-header "(\\S+)"
          :tls-version "(\\S+)"
          :access-point-arn "(\\S+)"]
         (partition-all 2)))
  ;; => #<Var@4b3cc742:
  ;;      ((:bucket-owner "(\\S+)")
  ;;       (:bucket "(\\S+)")
  ;;       (:time "\\[([^]]+)\\]")
  ;;       (:remote-ip "(\\S+)")
  ;;       (:requester "(\\S+)")
  ;;       (:request-id "(\\S+)")
  ;;       (:operation "(\\S+)")
  ;;       (:key "(\\S+)")
  ;;       (:request-uri "\"([^\"]+)\"")
  ;;       (:http-status "(\\S+)")
  ;;       (:error-code "(\\S+)")
  ;;       (:bytes-sent "(\\S+)")
  ;;       (:object-size "(\\S+)")
  ;;       (:total-time "(\\S+)")
  ;;       (:turn-around-time "(\\S+)")
  ;;       (:referer "\"([^\"]+)\"")
  ;;       (:user-agent "\"([^\"]+)\"")
  ;;       (:version-id "(\\S+)")
  ;;       (:host-id "(\\S+)")
  ;;       (:signature-version "(\\S+)")
  ;;       (:cipher-suite "(\\S+)")
  ;;       (:authentication-type "(\\S+)")
  ;;       (:host-header "(\\S+)")
  ;;       (:tls-version "(\\S+)")
  ;;       (:access-point-arn "(\\S+)"))>

  )
```

Having done this, let's build the regex from the individual patterns:

``` clojure
(comment

  (def log-regex (->> log-fields
                      (map second)
                      (str/join " ")
                      (format "^%s(.*)$")
                      re-pattern))
  ;; => #<Var@487539b9:
  ;;      #"^(\S+) (\S+) \[([^]]+)\] (\S+) (\S+) (\S+) (\S+) (\S+) \"([^\"]+)\" (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) \"([^\"]+)\" \"([^\"]+)\" (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) (\S+) (\S+)(.*)$">

  (re-seq log-regex
          "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -")
  ;; => (["022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -"
  ;;      "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"
  ;;      "jmglov.net"
  ;;      "27/Jun/2022:23:30:23 +0000"
  ;;      "64.252.88.38"
  ;;      "-"
  ;;      "DA5918VPJFK9A654"
  ;;      "WEBSITE.GET.OBJECT"
  ;;      "blog/2022-06-21-todo-list.html"
  ;;      "GET /blog/2022-06-21-todo-list.html HTTP/1.1"
  ;;      "304"
  ;;      "-"
  ;;      "-"
  ;;      "5447"
  ;;      "32"
  ;;      "-"
  ;;      "-"
  ;;      "Amazon CloudFront"
  ;;      "-"
  ;;      "ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI="
  ;;      "-"
  ;;      "-"
  ;;      "-"
  ;;      "jmglov.net.s3-website-eu-west-1.amazonaws.com"
  ;;      "-"
  ;;      "-"
  ;;      ""])

  )
```

Now we can grab the names of the fields and build a map by zipping them together
with the matched groups from the regex:

``` clojure
(comment

  (def log-keys (map first log-fields))
  ;; => #<Var@abab7a:
  ;;      (:bucket-owner
  ;;       :bucket
  ;;       :time
  ;;       :remote-ip
  ;;       :requester
  ;;       :request-id
  ;;       :operation
  ;;       :key
  ;;       :request-uri
  ;;       :http-status
  ;;       :error-code
  ;;       :bytes-sent
  ;;       :object-size
  ;;       :total-time
  ;;       :turn-around-time
  ;;       :referer
  ;;       :user-agent
  ;;       :version-id
  ;;       :host-id
  ;;       :signature-version
  ;;       :cipher-suite
  ;;       :authentication-type
  ;;       :host-header
  ;;       :tls-version
  ;;       :access-point-arn)>

  (->> "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -"
       (re-seq log-regex)
       first
       (drop 1)
       (zipmap log-keys))
  ;; => {:tls-version "-",
  ;;     :request-uri "GET /blog/2022-06-21-todo-list.html HTTP/1.1",
  ;;     :access-point-arn "-",
  ;;     :request-id "DA5918VPJFK9A654",
  ;;     :referer "-",
  ;;     :user-agent "Amazon CloudFront",
  ;;     :remote-ip "64.252.88.38",
  ;;     :key "blog/2022-06-21-todo-list.html",
  ;;     :host-header "jmglov.net.s3-website-eu-west-1.amazonaws.com",
  ;;     :version-id "-",
  ;;     :time "27/Jun/2022:23:30:23 +0000",
  ;;     :operation "WEBSITE.GET.OBJECT",
  ;;     :object-size "5447",
  ;;     :authentication-type "-",
  ;;     :error-code "-",
  ;;     :bytes-sent "-",
  ;;     :requester "-",
  ;;     :http-status "304",
  ;;     :turn-around-time "-",
  ;;     :signature-version "-",
  ;;     :total-time "32",
  ;;     :host-id
  ;;     "ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI=",
  ;;     :cipher-suite "-",
  ;;     :bucket "jmglov.net",
  ;;     :bucket-owner
  ;;     "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}

  )
```

There is one slight annoyance here, which is that we have all of these fields
with values of "-". According to the docs, a "-" means there's no value for that
field, so let's replace this with `nil`, which is Clojure's way of saying "no
value here":

``` clojure
(comment

  (->> "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188 jmglov.net [27/Jun/2022:23:30:23 +0000] 64.252.88.38 - DA5918VPJFK9A654 WEBSITE.GET.OBJECT blog/2022-06-21-todo-list.html \"GET /blog/2022-06-21-todo-list.html HTTP/1.1\" 304 - - 5447 32 - \"-\" \"Amazon CloudFront\" - ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI= - - - jmglov.net.s3-website-eu-west-1.amazonaws.com - -"
       (re-seq log-regex)
       first
       (drop 1)
       (map #(if (= "-" %) nil %))
       (zipmap log-keys))
  ;; => {:tls-version nil,
  ;;     :request-uri "GET /blog/2022-06-21-todo-list.html HTTP/1.1",
  ;;     :access-point-arn nil,
  ;;     :request-id "DA5918VPJFK9A654",
  ;;     :referer nil,
  ;;     :user-agent "Amazon CloudFront",
  ;;     :remote-ip "64.252.88.38",
  ;;     :key "blog/2022-06-21-todo-list.html",
  ;;     :host-header "jmglov.net.s3-website-eu-west-1.amazonaws.com",
  ;;     :version-id nil,
  ;;     :time "27/Jun/2022:23:30:23 +0000",
  ;;     :operation "WEBSITE.GET.OBJECT",
  ;;     :object-size "5447",
  ;;     :authentication-type nil,
  ;;     :error-code nil,
  ;;     :bytes-sent nil,
  ;;     :requester nil,
  ;;     :http-status "304",
  ;;     :turn-around-time nil,
  ;;     :signature-version nil,
  ;;     :total-time "32",
  ;;     :host-id
  ;;     "ukDRnMomesiUakfwW1nSFaoxoQQ/Nn14Dv+4helmPEcIkaIxEFfPojLviLC9vdbiER5zyB/ZOxI=",
  ;;     :cipher-suite nil,
  ;;     :bucket "jmglov.net",
  ;;     :bucket-owner
  ;;     "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}

  )
```

Much nicer!

## Cleaning up

OK, now that we've written all of this parsing code, we need to put it
somewhere, and our name namespace doesn't feel right. Let's create a new file to
hold all of the parsing logic, `parser.clj`:

``` clojure
(ns parser
  (:require [clojure.string :as str]))

(def log-fields
  (->> [:bucket-owner "(\\S+)"
        :bucket "(\\S+)"
        :time "\\[([^]]+)\\]"
        :remote-ip "(\\S+)"
        :requester "(\\S+)"
        :request-id "(\\S+)"
        :operation "(\\S+)"
        :key "(\\S+)"
        :request-uri "\"([^\"]+)\""
        :http-status "(\\S+)"
        :error-code "(\\S+)"
        :bytes-sent "(\\S+)"
        :object-size "(\\S+)"
        :total-time "(\\S+)"
        :turn-around-time "(\\S+)"
        :referer "\"([^\"]+)\""
        :user-agent "\"([^\"]+)\""
        :version-id "(\\S+)"
        :host-id "(\\S+)"
        :signature-version "(\\S+)"
        :cipher-suite "(\\S+)"
        :authentication-type "(\\S+)"
        :host-header "(\\S+)"
        :tls-version "(\\S+)"
        :access-point-arn "(\\S+)"]
       (partition-all 2)))

(def log-keys (map first log-fields))
(def log-regex (->> log-fields
                    (map second)
                    (str/join " ")
                    (format "^%s(.*)$")
                    re-pattern))

(defn parse-line [log-line]
  (->> log-line
       (re-seq log-regex)
       first
       (drop 1)
       (map #(if (= "-" %) nil %))
       (zipmap log-keys)))
```

Now we can use this stuff in the handler, back in `s3_log_parser.clj`:

``` clojure
(ns s3-log-parser
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [com.grzm.awyeah.client.api :as aws]
            [parser])
  (:import (java.time Instant
                      LocalDate)))

(defn handler
  ([]
   (handler {} {}))
  ([event]
   (handler event {}))
  ([event _context]
   (log "Invoked with event" {:event event})
   (try
     (let [date (get-date (get-in event ["queryStringParameters" "date"]))
           logs (list-logs date)
           log-entries (->> logs
                            (mapcat get-log-lines)
                            (map parser/parse-line))
           body {:date (str date), :logs logs, :entries log-entries}]
       (log "Successfully parsed logs" body)
       {"statusCode" 200
        "body" (json/encode body)})
     (catch Exception e
       (log (ex-message e) (ex-data e))
       {"statusCode" 400
        "body" (ex-message e)}))))

(comment

  (handler {})
  ;; => {"statusCode" 200,
  ;;     "body"
  ;;     "{\"date\":\"2022-09-02\",\"logs\":[\"logs/2022-09-02-00-12-48-36034402F760D842\",\"logs/2022-09-02-00-13-47-48D19FF5B34710F9\"],\"entries\":[{\"request-uri\":\"HEAD /blog/2022-08-26-doing-software-wrong.html HTTP/1.1\",\"request-id\":\"BC0KZGTDKR1QP07G\",\"user-agent\":\"Amazon CloudFront\",\"remote-ip\":\"64.252.89.133\",\"key\":\"blog/2022-08-26-doing-software-wrong.html\",\"host-header\":\"jmglov.net.s3-website-eu-west-1.amazonaws.com\",\"time\":\"31/Aug/2022:23:40:32 +0000\",\"operation\":\"WEBSITE.HEAD.OBJECT\",\"object-size\":\"7536\",\"http-status\":\"304\",\"total-time\":\"34\",\"host-id\":\"ApxkRhuJl/C73ZiR70xT6Stn2b1RkIcDPMnapeH5kWWQ+mT41qXfNeLaqpMc3j+5WCnrqoJH8N0=\",\"bucket\":\"jmglov.net\",\"bucket-owner\":\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188\"},{\"request-uri\":\"GET / HTTP/1.1\",\"request-id\":\"M987YV3YB7DHBWA3\",\"user-agent\":\"Amazon CloudFront\",\"remote-ip\":\"64.252.73.234\",\"key\":\"index.html\",\"host-header\":\"jmglov.net.s3-website-eu-west-1.amazonaws.com\",\"time\":\"31/Aug/2022:23:14:14 +0000\",\"operation\":\"WEBSITE.GET.OBJECT\",\"object-size\":\"3459\",\"http-status\":\"304\",\"total-time\":\"18\",\"host-id\":\"14MYY/WyblbL1WgULsK86Cwwtn+tHCOgs+Y98xIkC/EIwkqMeN/SWpBsF6x2gC1Tir7DYRc/+Zk=\",\"bucket\":\"jmglov.net\",\"bucket-owner\":\"022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188\"}]}"}

  )
```

Now that everything looks good, we can go back to the Lambda console and try it
out. We'll need to add our `parser.clj` file to the code, which we can do by
using the **File > New File** menu option in the **Code source** section of the
console, pasting in the contents of `parser.clj`, and then doing **File > Save
As...** and entering "parser.clj" as the filename. We now need to paste the
contents of `s3_log_parser.clj` into that file in the console, then press the
**Deploy** button to put the new code out into the world.

Finally, we can take a deep breath, position the mouse cursor over the **Test**
button, close our eyes, and click... victory!

![Screenshot of the AWS Lambda console showing a test result with parsed
lines](assets/2022-09-02-parsed.png "Now what to do with this stuff?" width=800px)

Of course, this is a really clunky and error prone way to deploy code. In the
next instalment of Dogfooding Blambda, we'll build a production grade deployment
framework.

But for now, let's rest on our laurels so I can go eat some lunch. 😉
