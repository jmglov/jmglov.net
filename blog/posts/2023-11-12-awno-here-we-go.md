Title: Awno! Here we go!
Date: 2023-11-12
Tags: clojure,awno,babashka
Description: In which I make awno-api work, through any means necessary
Image: assets/2023-11-12-abort.jpg
Image-Alt: A supercar driving out of the window of a skyscraper

## Admission of guilt

Between the writing of the previous post in this series and now, two things
happened:
1. I got distracted by other stuff
2. grzm [fixed the
   issue](https://github.com/grzm/awyeah-api/issues/8#issuecomment-1820183384)
   properly upstream! 🎉

I decided to go ahead and post this for the record, just in case there's
anything useful for anyone in there, and also to clear my palate (and my
conscience) for some new blogging. 😉

So here you go:

## Just for the record

In [last week's exciting instalment of Josh does something
inadvisable](2023-11-11-awno-api.html), we had finished smooshing
[awyeah-api](https://github.com/grzm/awyeah-api) into an unholy fork of
[aws-api](https://github.com/cognitect-labs/aws-api) to make something
completely horrific: [awno-api](https://github.com/jmglov/awno-api). And by
"finished", I mean "wrote some code and assumed that it will obviously work".

Of course, even at my most hubristic, I realise that assuming something will
work just because I wrote it is somewhat... irresponsible. So I guess we'd
better actually try it out before committing to main.

## Testing, testing... is this thing on?

awyeah-api has an intriguing
[bin/tests](https://github.com/grzm/awyeah-api/blob/main/bin/test), which we've
previously copied, so let's do a quick replace all of `com.grzm.awyeah` with
`net.jmglov.awno`:

``` clj
#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

cd "${repo_root}"
if [[ -z ${SKIP_BB+x} ]] ; then
    echo "bb tests"
    bb --main net.jmglov.awno.test/run-tests
else
    echo "skipping bb tests"
fi

echo
echo "clj tests"
clj -X:dev:test:clj
```

Then we'll need the actual `net.jmglov.awno.test` namespace, so we'll grab
[test/src/com/grzm/awyeah/test.cljc](https://github.com/grzm/awyeah-api/blob/main/test/src/com/grzm/awyeah/test.cljc):

``` text
cp ../awyeah-api/test/src/com/grzm/awyeah/test.cljc test/src/net/jmglov/awno/
```

Then do the usual replace all of `com.grzm.awyeah` with `net.jmglov.awno`:

``` clojure
(ns net.jmglov.awno.test
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as test]))

#?(:bb (taoensso.timbre/set-level! :info))

(def test-namespaces
  '[net.jmglov.awno.client.api.localstack-test
    net.jmglov.awno.client.api-test
    net.jmglov.awno.client.impl-test
    net.jmglov.awno.client.test-double-test
    net.jmglov.awno.config-test
    net.jmglov.awno.credentials-test
    net.jmglov.awno.ec2-metadata-utils-test
    net.jmglov.awno.endpoint-test
    net.jmglov.awno.interceptors-test
    net.jmglov.awno.protocols-test
    net.jmglov.awno.protocols.rest-test
    net.jmglov.awno.region-test
    net.jmglov.awno.retry-test
    net.jmglov.awno.shape-test
    ;; omitting net.jmglov.awno.signers-test
    ;; Requires org.apache.commons.io.input.BOMInputStream which I haven't figured out
    ;; how to port to something compatible with Babashka
    net.jmglov.awno.util-test])

(defn run-tests
  ([]
   (run-tests {:test-namespaces test-namespaces}))
  ([{nses :test-namespaces}]
   (dorun (map require nses))
   (let [res (apply test/run-tests nses)]
     (pprint/pprint res)
     (when (->> ((juxt :fail :error) res)
                (some #(pos? %)))
       (System/exit 1)))))
```

And then give it a whirl!

``` text
: awno-api; bin/test 
bb tests
----- Error --------------------------------------------------------------------
Type:     java.lang.Exception
Message:  Could not find namespace: net.jmglov.awno.client.api.localstack-test.
Location: /home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/test.cljc:32:4
```

Ah yes, another reason to hate [LocalStack](https://localstack.cloud/). 😠

LocalStack claims to be "A fully functional local cloud stack [which enables you
to] develop and test your cloud and serverless apps offline!" In my experience,
it is a somewhat functional local cloud stack which enables me to develop and
test my cloud and serverless apps offline—as long as I limit don't use any
services that aren't supported by LocalStack (to be fair, the [list of supported
services](https://docs.localstack.cloud/user-guide/aws/feature-coverage/) is
growing), and don't expect the local service to actually behave like the real
one. 😅

Before continuing with my shit-talking, let me just insert my standard
disclaimer here:

{thing-which-i-hate} is a perfectly serviceable piece of technology, which many people
find useful. I'm not any smarter than those people, and their choice to use
{thing-which-i-hate} is perfectly valid. All the vitriol I pour on
{thing-which-i-hate} is tongue-in-cheek and should be taken with a serious grain
of salt, in full knowledge that I often don't know what I'm talking about,
because my experience isn't representative of all use cases, and so on and so
forth.

OK, having gotten that out of the way, let's remove all references to the odious
LocalStack and move on with our lives!

``` clojure
(def test-namespaces
  '[net.jmglov.awno.client.api-test
    net.jmglov.awno.client.impl-test
    net.jmglov.awno.client.test-double-test
    net.jmglov.awno.config-test
    net.jmglov.awno.credentials-test
    net.jmglov.awno.ec2-metadata-utils-test
    net.jmglov.awno.endpoint-test
    net.jmglov.awno.interceptors-test
    net.jmglov.awno.protocols-test
    net.jmglov.awno.protocols.rest-test
    net.jmglov.awno.region-test
    net.jmglov.awno.retry-test
    net.jmglov.awno.shape-test
    ;; omitting net.jmglov.awno.signers-test
    ;; Requires org.apache.commons.io.input.BOMInputStream which I haven't figured out
    ;; how to port to something compatible with Babashka
    net.jmglov.awno.util-test])
```

If at first you don't succeed, dust yourself off and try again (you gotta dust
it off and try again):

``` text
: awno-api; bin/test 
bb tests
----- Error --------------------------------------------------------------------
Type:     java.lang.Exception
Message:  Could not find namespace: net.jmglov.awno.client.api-test.
Location: /home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/test.cljc:32:4
```

Oh come on!

In all the `git mv` nonsense, we probably just misplaced a file or two, so let's
see if we can track this test down:

``` text
: awno-api; find test/src/ -name api_test.clj
test/src/net/jmglov/awno/api_test.clj
```

Aha! There it is. Before getting too cocky, let's see which other test files
we're missing:

``` text
: awno-api; bb -e '
> (def base-dir "test/src")
(with-open [r (io/reader (format "%s/net/jmglov/awno/test.cljc" base-dir))]
  (let [namespaces
        (->
         (->> (line-seq r)
              (drop-while #(not (str/starts-with? % "(def test-namespaces")))
              (take-while not-empty)
              (str/join "\n")
              edn/read-string)
         (nth 3))
        filenames (->> namespaces
                       (map #(->> (-> %
                                      (str/replace "." "/")
                                      (str/replace "-" "_"))
                                  (format "%s/%s.clj" base-dir))))
        missing (remove fs/exists? filenames)]
    (println (str/join "\n" missing))))
'
```

Babashka helpfully tells us:

``` text
test/src/net/jmglov/awno/client/api_test.clj
test/src/net/jmglov/awno/client/impl_test.clj
test/src/net/jmglov/awno/client/test_double_test.clj
```

I bet we can even track down those missing files!

``` text
: awno-api; bb -e '
(def base-dir "test/src")
(with-open [r (io/reader (format "%s/net/jmglov/awno/test.cljc" base-dir))]
  (let [namespaces
        (->
         (->> (line-seq r)
              (drop-while #(not (str/starts-with? % "(def test-namespaces")))
              (take-while not-empty)
              (str/join "\n")
              edn/read-string)
         (nth 3))
        filenames (->> namespaces
                       (map #(->> (-> %
                                      (str/replace "." "/")
                                      (str/replace "-" "_"))
                                  (format "%s/%s.clj" base-dir))))
        missing (remove fs/exists? filenames)
        found (->> missing
                   (map (fn [mf]
                          [mf (->> (fs/file-name mf)
                                   (format "**/%s")
                                   (fs/glob base-dir)
                                   first
                                   str)])))]
    (doseq [[missing-file found-file] found]
      (println found-file "->" missing-file))))
'
```

And look what we have here:

``` text
test/src/net/jmglov/awno/api_test.clj -> test/src/net/jmglov/awno/client/api_test.clj
test/src/cognitect/client/impl_test.clj -> test/src/net/jmglov/awno/client/impl_test.clj
test/src/cognitect/client/test_double_test.clj -> test/src/net/jmglov/awno/client/test_double_test.clj
```

Yup, got them all. Now to put them where they go and fix the namespaces:

``` text
(def base-dir "test/src")
(with-open [r (io/reader (format "%s/net/jmglov/awno/test.cljc" base-dir))]
  (let [namespaces
        (->
         (->> (line-seq r)
              (drop-while #(not (str/starts-with? % "(def test-namespaces")))
              (take-while not-empty)
              (str/join "\n")
              edn/read-string)
         (nth 3))
        filenames (->> namespaces
                       (map (fn [ns*]
                              [ns*
                               (->> (-> ns*
                                        (str/replace "." "/")
                                        (str/replace "-" "_"))
                                    (format "%s/%s.clj" base-dir))])))
        missing (remove (fn [[_ filename]] (fs/exists? filename)) filenames)
        found (->> missing
                   (map (fn [[ns* filename]]
                          {:ns* ns*
                           :target filename
                           :source (->> (fs/file-name filename)
                                        (format "**/%s")
                                        (fs/glob base-dir)
                                        first
                                        str)})))]
    (doseq [{:keys [ns* source target]} found
            :let [fixed-ns
                  (with-open [r (io/reader source)]
                    (->> (line-seq r)
                         (map (fn [line]
                                (if (re-matches #"^[(]ns .+$" line)
                                  (str "(ns " ns*)
                                  line)))
                         (str/join "\n")))]]
      (spit source fixed-ns)
      (println "Moving" source "->" target)
      (fs/create-dirs (fs/parent target))
      (fs/move source target))))
```

Quoth Babashka:

``` text
Moving test/src/net/jmglov/awno/api_test.clj -> test/src/net/jmglov/awno/client/api_test.clj
Moving test/src/cognitect/client/impl_test.clj -> test/src/net/jmglov/awno/client/impl_test.clj
Moving test/src/cognitect/client/test_double_test.clj -> test/src/net/jmglov/awno/client/test_double_test.clj
```

Lets open up `test/src/net/jmglov/awno/client/api_test.clj` and see what we've
got:

``` clojure
(ns net.jmglov.awno.client.api-test
  (:require [clojure.datafy :as datafy]
            [clojure.test :as t :refer [deftest is testing]]
            [net.jmglov.awno.client.api :as aws]
            [net.jmglov.awno.client.protocol :as client.protocol]
            [net.jmglov.awno.client.shared :as shared]
            [net.jmglov.awno.http :as http]))

;; [...]
```

Wow! Looking good!

## Testing, testing, one two...

Having moved the furniture around, let's try running the tests again:

``` text
: awno-api; bin/test 
bb tests
----- Error --------------------------------------------------------------------
Type:     java.lang.Exception
Message:  Could not find namespace: net.jmglov.awno.dynaload.
Location: /home/jmglov/Documents/code/clojure/awno-api/src/net/jmglov/awno/client/validation.clj:6:3
```

OMG, it looks like there was another dynaload that we missed in our previous
babashkafication of aws-api. Let's open
`src/net/jmglov/awno/client/validation.clj` see what's going on.

It looks like dynaload is being used for the following three vars:

``` clojure
(def ^:private registry-ref (delay (dynaload/load-var 'clojure.spec.alpha/registry)))
(def ^:private valid?-ref (delay (dynaload/load-var 'clojure.spec.alpha/valid?)))
(def ^:private explain-data-ref (delay (dynaload/load-var 'clojure.spec.alpha/explain-data)))
```

According to
[porting-decisions.markdown](https://github.com/grzm/awyeah-api/blob/main/docs/porting-decisions.markdown):

> The aws-api library defines the `cognitect.dynaload/load-var` function to
> dynamically require and resolve the var referenced by a given symbol. Clojure
> 1.10 provides the same functionality with the `requiring-resolve` function.
> Given that `requiring-resolve` is compiled into the babashka image, I've chosen
> to replace `load-var` with `requiring-resolve` rather than relying on
> [sci](https://github.com/babashka/sci) to interpret `load-var` at run-time.

Cool, so let's replace all occurrences of `dynaload/load-var` with
`requiring-resolve`, then rip dynaload out of the `ns` form, leaving us with
this:

``` clojure
;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki net.jmglov.awno.client.validation
  "For internal use. Don't call directly."
  (:require [net.jmglov.awno.client.protocol :as client.protocol]
            [net.jmglov.awno.service :as service]))

(set! *warn-on-reflection* true)

(defn validate-requests?
  "For internal use. Don't call directly."
  [client]
  (some-> client client.protocol/-get-info :validate-requests? deref))

(def ^:private registry-ref (delay (requiring-resolve 'clojure.spec.alpha/registry)))
(defn registry
  "For internal use. Don't call directly."
  [& args] (apply @registry-ref args))

(def ^:private valid?-ref (delay (requiring-resolve 'clojure.spec.alpha/valid?)))
(defn valid?
  "For internal use. Don't call directly."
  [& args] (apply @valid?-ref args))

(def ^:private explain-data-ref (delay (requiring-resolve 'clojure.spec.alpha/explain-data)))
(defn explain-data
  "For internal use. Don't call directly."
  [& args] (apply @explain-data-ref args))

(defn request-spec
  "For internal use. Don't call directly."
  [service op]
  (when-let [spec (service/request-spec-key service op)]
    (when (contains? (-> (registry) keys set) spec)
      spec)))

(defn invalid-request-anomaly
  "For internal use. Don't call directly."
  [spec request]
  (assoc (explain-data spec request)
         :cognitect.anomalies/category :cognitect.anomalies/incorrect))

(defn unsupported-op-anomaly
  "For internal use. Don't call directly."
  [service op]
  {:cognitect.anomalies/category :cognitect.anomalies/unsupported
   :cognitect.anomalies/message "Operation not supported"
   :service (keyword (service/service-name service))
   :op op})
```

Let's go ahead and search the project for any other occurrences of dynaload that
we may have missed. Ah-hah! `net.jmglov.awno.client.api` is also dynaloading. We
can repeat the dance of replacing `dynaload/load-var` with `requiring-resolve`,
then removing it from the `ns` form, then evaluating the buffer:

``` text
clojure.lang.ExceptionInfo: defrecord/deftype currently only support protocol implementations, {:file "/home/jmglov/Documents/code/clojure/awno-api/src/net/jmglov/awno/client/impl.clj"}
found: clojure.lang.IObj
```

Yikes! Taking a look at `impl.clj`, this seems to be the problem:

``` clojure
(deftype Client [client-meta info]
  clojure.lang.IObj
  (meta [_] @client-meta)
  (withMeta [this m] (swap! client-meta merge m) this)

  ILookup
  (valAt [this k]
    (.valAt this k nil))

  (valAt [this k default]
    (case k
      :api
      (-> info :service :metadata :net.jmglov.awno/service-name)
      :region
      (some-> info :region-provider region/fetch)
      :endpoint
      (some-> info :endpoint-provider (endpoint/fetch (.valAt this :region)))
      :credentials
      (some-> info :credentials-provider credentials/fetch)
      :service
      (some-> info :service (select-keys [:metadata]))
      :http-client
      (:http-client info)
      default))

  client.protocol/Client
  (-get-info [_] info)

  ;; [...]
  )
```

`Client` implements the `clojure.lang.IObj` and `ILookup` interfaces, and
Babashka only supports implementing protocols.

Let's see how awyeah-api handles this by looking at
[src/com/grzm/awyeah/client/impl.clj](https://github.com/grzm/awyeah-api/blob/main/src/com/grzm/awyeah/client/impl.clj):

``` clojure
(defrecord Client [info]
  client.protocol/Client
  (-get-info [_] info)

  ;; [...]
  )

;; ->Client is intended for internal use
(alter-meta! #'->Client assoc :skip-wiki true)

(defn client [client-meta info]
  (let [region (some-> info :region-provider region/fetch)]
    (-> (with-meta (->Client info) @client-meta)
        (assoc :region region
               :endpoint (some-> info :endpoint-provider (endpoint/fetch region))
               :credentials (some-> info :credentials-provider credentials/fetch)
               :service (some-> info :service (select-keys [:metadata]))
               :http-client (:http-client info)))))
```

Interesting. We can do the same thing, transforming `deftype` into `defrecord`
and removing the `clojure.lang.IObj` and `ILookup` interface methods:

``` clojure
(defrecord Client [info]
  client.protocol/Client
  (-get-info [_] info)

  (-invoke [client op-map]
    (a/<!! (client.protocol/-invoke-async client op-map)))

  (-invoke-async [client {:keys [op request] :as op-map}]
    (let [result-chan (or (:ch op-map) (a/promise-chan))
          {:keys [service retriable? backoff]} (client.protocol/-get-info client)
          spec (and (validation/validate-requests? client) (validation/request-spec service op))]
      (cond
        (not (contains? (:operations service) (:op op-map)))
        (a/put! result-chan (validation/unsupported-op-anomaly service op))

        (and spec (not (validation/valid? spec request)))
        (a/put! result-chan (validation/invalid-request-anomaly spec request))

        :else
        ;; In case :body is an InputStream, ensure that we only read
        ;; it once by reading it before we send it to with-retry.
        (let [req (-> (aws.protocols/build-http-request service op-map)
                      (update :body util/->bbuf))]
          (retry/with-retry
            #(send-request client op-map req)
            result-chan
            (or (:retriable? op-map) retriable?)
            (or (:backoff op-map) backoff))))

      result-chan))

  (-stop [aws-client]
    (let [{:keys [http-client]} (client.protocol/-get-info aws-client)]
      (when-not (#'shared/shared-http-client? http-client)
        (http/stop http-client)))))
```

Now we need to add a `client` function to return a `Client` record to callers.
In the `deftype` version, fields are implemented like this:

``` clojure
  (valAt [this k default]
    (case k
      :api
      (-> info :service :metadata :net.jmglov.awno/service-name)
      :region
      (some-> info :region-provider region/fetch)
      :endpoint
      (some-> info :endpoint-provider (endpoint/fetch (.valAt this :region)))
      :credentials
      (some-> info :credentials-provider credentials/fetch)
      :service
      (some-> info :service (select-keys [:metadata]))
      :http-client
      (:http-client info)
      default))
```

Let's follow the lead of awyeah-api and use a Clojure map to do the same:

``` clojure
(defn client [client-meta info]
  (let [region (some-> info :region-provider region/fetch)]
    (-> (with-meta (->Client info) @client-meta)
        (assoc :api (-> info :service :metadata :cognitect.aws/service-name)
               :region region
               :endpoint (some-> info :endpoint-provider (endpoint/fetch region))
               :credentials (some-> info :credentials-provider credentials/fetch)
               :service (some-> info :service (select-keys [:metadata]))
               :http-client (:http-client info)))))
```

We also need to update the `-invoke` method of `Client` to call `-invoke-async`
as a function, not an instance method:

``` clojure
  (-invoke [client op-map]
    (a/<!! (client.protocol/-invoke-async client op-map)))
```

Note the `:api` key we added to the map, which isn't present in the awyeah-api
version. Taking a look at
[src/cognitect/aws/client/impl.clj](https://github.com/cognitect-labs/aws-api/blob/main/src/cognitect/aws/client/impl.clj)
in aws-api with `magit-blame`, we can see why:

``` text
  (valAt [this k default]
    (case k
David Chelimsky	2022-12-01 20:31 add keyword access to :api key on client
      :api
      (-> info :service :metadata :cognitect.aws/service-name)
Maria Clara Crespo	2022-09-12 11:23 introduce test double client
      :region
      (some-> info :region-provider region/fetch)
```

Since this change is clearly intentional, we ported it over, changing the
`:net.jmglov.awno/service-name` keyword (which resulted from our
`projectile-replace cognitect.aws -> net.jmglov.awno`) back to
`:cognitect.aws/service-name`.

And now that we've replaced the type with a record, we need to make sure it can
only be instantiated using our `client` function. Let's make `->Client` private:

``` clojure
;; ->Client is intended for internal use
(alter-meta! #'->Client assoc :skip-wiki true)
```

Now, we need to track down all callers of `->Client` outside this namespace. There
only turns out to be one, in `net.jmglov.awno.http-client`:

``` clojure
    (client/->Client
     (atom {'clojure.core.protocols/datafy (fn [c]
                                             (let [info (client.protocol/-get-info c)
                                                   region (region/fetch (:region-provider info))
                                                   endpoint (endpoint/fetch (:endpoint-provider info) region)]
                                               (-> info
                                                   (select-keys [:service])
                                                   (assoc :api (-> info :service :metadata :cognitect.aws/service-name))
                                                   (assoc :region region :endpoint endpoint)
                                                   (update :endpoint select-keys [:hostname :protocols :signatureVersions])
                                                   (update :service select-keys [:metadata])
                                                   (assoc :ops (ops c)))))})
     {:service              service
      :retriable?           (or retriable? retry/default-retriable?)
      :backoff              (or backoff retry/default-backoff)
      :http-client          http-client
      :endpoint-provider    endpoint-provider
      :region-provider      region-provider
      :credentials-provider credentials-provider
      :validate-requests?   (atom nil)})
```

Making the change is as easy as replacing `client/->Client` with
`client/client`! Oh yeah, and replacing that `:net.jmglov.awno/service-name`
with `:cognitect.aws/service-name`.

However, when we eval the buffer, we get another nasty dynaload-related
surprise:

```
clojure.lang.ExceptionInfo: Could not resolve symbol: dynaload/load-ns
{:type :sci/error,
 :file "/home/jmglov/Documents/code/clojure/awno-api/src/net/jmglov/awno/client/api.clj",
 :phase "analysis"}
```

Apparently there's more to dynaload than just `load-var`. And strangely enough,
`com.grzm.awyeah.client.api` contains a dynaload require:

``` clojure
(ns com.grzm.awyeah.client.api
  "API functions for using a client to interact with AWS services."
  (:require
   ;; [...]
   [com.grzm.awyeah.dynaload :as dynaload]
   ;; [...]
   [com.grzm.awyeah.signers]))
```

No mention of this was made in `porting-decisions.markdown`. 😭

Let's just copy `com.grzm.awyeah.dynaload` and jmglov-ify it:

``` clojure
;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki net.jmglov.awno.dynaload)

(set! *warn-on-reflection* true)

(defonce ^:private dynalock (Object.))

(defn load-ns [ns]
  (locking dynalock
    (require (symbol ns))))
```

With that done, `net.jmglov.awno.client.api` evaluates with no errors! 🏆

## Testing, testing, one two three...

Surely our tests will run now! Right?

``` text
: awno-api; bin/test
bb tests
----- Error --------------------------------------------------------------------
Type:     java.lang.Exception
Message:  Could not find namespace: clojure.test.check.clojure-test.
Location: /home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/impl_test.clj:3:3
```

Urk! Looks like awyeah-api didn't include property-based tests. Let's add
[test.check](https://github.com/clojure/test.check) to our `bb.edn` and see what
happens. And don't worry, we're only going to end up with `test.check` added to
our runtime dependencies when running `bb` directly, not when using `awno-api`
as a dependency, since `deps.edn` is used in that case, not `bb.edn`. This is
important for me, since my interest in awyeah-api is solely as a way to use AWS
stuff from [blambda](https://github.com/jmglov/blambda), I don't want extra
stuff taking up space in my deps layer. Not to mention having test dependencies
in your production code is just icky. 😅

So anyway, whacking

``` clojure
org.clojure/test.check {:mvn/version "1.1.1"}
```

into the `:deps` map then re-running the tests yields the following:

``` text
: awno-api; bin/test
bb tests
----- Error --------------------------------------------------------------------
Type:     java.lang.Exception
Message:  Could not find namespace: cognitect.client.impl-test.
Location: /home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/test_double_test.clj:3:3
```

Oops. Looks like `cognitect.client` is in need of some `projectile-replace`.
Replacing it with `net.jmglov.awno.client` should do the trick.

``` text
: awno-api; bin/test
bb tests
----- Error --------------------------------------------------------------------
Type:     clojure.lang.ExceptionInfo
Message:  defrecord/deftype currently only support protocol implementations, found: ILookup
Data:     {:type :sci/error, :line 23, :column 1, :file "/home/jmglov/Documents/code/clojure/awno-api/src/net/jmglov/awno/client/test_double.clj"}
Location: /home/jmglov/Documents/code/clojure/awno-api/src/net/jmglov/awno/client/test_double.clj:23:1
Phase:    macroexpand
```

Urk! At this point, I'm reminded of a funny Rich Hickey quote:

![Rich Hickey giving a talk, saying the following: I think we’re in this world I’d like to call Guard Rail Programming... I can make change because I have tests! Who does that? Who drives their car around, banging against the guard rails? Do the guard rails help you get to where you want to go?][guardrails]

Opening up `net.jmglov.awno.client.test-double`, we see something familiar:

``` clojure
(deftype Client [info handlers]
  ILookup
  (valAt [this k]
    (.valAt this k nil))

  (valAt [_this k default]
    (case k
      :api
      (-> info :service :metadata :net.jmglov.awno/service-name)
      :service
      (:service info)
      default))

  client.protocol/Client
  (-get-info [_] info)
  
  ;; [...]
  )
```

Why don't we repeat our tried and tested remedy of replacing `deftype` with
`defrecord`, then ripping out the `ILookup` interface and replacing it with a
map? That gives us this:

``` clojure
(deftype Client [info handlers]
  client.protocol/Client
  (-get-info [_] info)

  (-invoke [this {:keys [op request] :as op-map}]
    ;; [...]
    )

  (-invoke-async [this {:keys [ch] :as op-map}]
    ;; [...]
    )

  (-stop [_aws-client])
  
  TestDoubleClient
  (-instrument [client ops]
    ;; [...]
    ))

;; ->Client is intended for internal use
(alter-meta! #'->Client assoc :skip-wiki true)
(alter-meta! #'TestDoubleClient assoc :skip-wiki true)

(defn instrument
  "Given a test double client and a `:ops` map of operations to handlers,
   instruments the client with handlers. See `client` for more info about
   `:ops`."
  [client ops]
  (-instrument client ops))

(defn client
  "Given a map with :api and :ops (optional), returns a test double client that
  [...]
  - will not validate response payloads"
  [{:keys [api ops]}]
  (let [service (service/service-description (name api))]
    (doto (->Client {:api (-> service :metadata :cognitect.aws/service-name)
                     :service service
                     :validate-requests? (atom true)} (atom {}))
      (instrument ops))))
```

OK, **surely** the tests will run now!

``` text
: awno-api; bin/test
bb tests

Testing net.jmglov.awno.client.api-test

ERROR in (test-underlying-http-client) (/home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/api_test.clj:9)
defaults to shared client
expected: (= #{(shared/http-client)} (into #{(shared/http-client)} (->> clients (map (fn [c] (-> c client.protocol/-get-info :http-client))))))
  actual: clojure.lang.ExceptionInfo: Cannot find resource net.jmglov.awno/s3/service.edn.
{}
 at sci.lang.Var.invoke (lang.cljc:202)
    sci.impl.analyzer$return_call$reify__4543.eval (analyzer.cljc:1399)
[...]

Testing net.jmglov.awno.util-test

Ran 48 tests containing 675 assertions.
19 failures, 33 errors.
{:test 48, :pass 623, :fail 19, :error 33, :type :summary}
```

![A woman on a beach at sunrise with her head thrown back, saying "Victory"][victory]

Well, sorta.

[guardrails]: assets/2023-11-12-guardrails.png "Simple made snarky"
[victory]: assets/2023-11-11-victory.jpg "Never in doubt" width=800px

## What the what?

`bb test` produced 6544 lines of output whilst failing those 19 tests and
erroring out of a further 33. Let's strip away all the noise and see if we can
see what's actually happening here.

``` text
: awno-api; bin/test 2>&1 >/tmp/err.log
ERROR in (test-underlying-http-client) (/home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/api_test.clj:9)
defaults to shared client
expected: (= #{(shared/http-client)} (into #{(shared/http-client)} (->> clients (map (fn [c] (-> c client.protocol/-get-info :http-client))))))
  actual: clojure.lang.ExceptionInfo: Cannot find resource net.jmglov.awno/s3/service.edn.
--
[...]
--
ERROR in (raw-response-values) (/home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/test_double_test.clj:33)
Uncaught exception, not in assertion.
expected: nil
  actual: clojure.lang.ExceptionInfo: null
--
[...]
--
ERROR in (test-parse-date) (/home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/shape_test.clj:5)
iso8601 format handles presence and absence of fractional seconds
expected: (= #inst "2020-07-06T10:59:13.417-00:00" (shape/parse-date {:timestampFormat "iso8601"} "2020-07-06T10:59:13.417Z"))
  actual: java.time.format.DateTimeParseException: Text '2020-07-06T10:59:13.417Z' could not be parsed at index 19
```

OK, not as bad as it could be. There are only three classes of errors here,
repeated many times. The first one should be extremely simple to deal with. A
quick search in the project for `service.edn` yields only one hit, in the
`net.jmglov.awno.service` namespace:

```` clojure
(def base-ns "net.jmglov.awno")

(def base-resource-path "net.jmglov.awno")
```

Looks like my search and replace was a bit excessively exuberant. Let's try
restoring their Cognitectiness:

``` clojure
(def base-ns "cognitect.aws")

(def base-resource-path "cognitect/aws")
```

Before re-running all the tests, it would be quite lovely to give ourselves a
way to run tests for a subset of namespaces, instead of all the ones specified
in `net.jmglov.awno.test`. Actually, looking at `run-tests`, maybe we can!

``` clojure
(defn run-tests
  ([]
   (run-tests {:test-namespaces test-namespaces}))
  ([{nses :test-namespaces}]
   (dorun (map require nses))
   (let [res (apply test/run-tests nses)]
     (pprint/pprint res)
     (when (->> ((juxt :fail :error) res)
                (some #(pos? %)))
       (System/exit 1)))))
```

The 1-arity version of `run-tests` lets us pass a map with `:test-namespaces`,
and Babashka's `-x` flag uses [babashka-cli](https://book.babashka.org/#cli) to
let us pass function arguments as command line flags, so we should be able to do
this!

``` text
: awno-api; bb -x net.jmglov.awno.test/run-tests --test-namespaces net.jmglov.awno.client.api-test
----- Error --------------------------------------------------------------------
Type:     java.lang.IllegalArgumentException
Message:  Don't know how to create ISeq from: java.lang.Character
Location: /home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/test.cljc:32:4
```

Blerg! Looks like the value of the `:test-namespaces` key is a string, rather
than a list of strings. Luckily, babashka-cli lets you add metadata to functions
to [control parsing behaviour](https://github.com/babashka/cli#clojure-cli).
Let's sprinkle some on `run-tests`:

``` clojure
(defn run-tests
  {:org.babashka/cli {:coerce {:test-namespaces [:symbol]}}}
  ([]
   (run-tests {:test-namespaces test-namespaces}))
  ([{nses :test-namespaces}]
   (dorun (map require nses))
   (let [res (apply test/run-tests nses)]
     (pprint/pprint res)
     (when (->> ((juxt :fail :error) res)
                (some #(pos? %)))
       (System/exit 1)))))
```

Now babashka-cli will coerce `:test-namespaces` to a list of symbols! 😲

Let's try it out on a couple of test namespaces that were actually passing:

``` text
: awno-api; bb -x net.jmglov.awno.test/run-tests \
  --test-namespaces net.jmglov.awno.config-test net.jmglov.awno.credentials-test
{:test-namespaces [net.jmglov.awno.config-test net.jmglov.awno.credentials-test]}

Testing net.jmglov.awno.config-test

Testing net.jmglov.awno.credentials-test

Ran 9 tests containing 41 assertions.
0 failures, 0 errors.
{:test 9, :pass 41, :fail 0, :error 0, :type :summary}
```

This looks good, so let's give api-test another go:

``` text
: awno-api; bb -x net.jmglov.awno.test/run-tests \
  --test-namespaces net.jmglov.awno.client.api-test

Testing net.jmglov.awno.client.api-test

Ran 3 tests containing 11 assertions.
0 failures, 0 errors.
{:test 3, :pass 11, :fail 0, :error 0, :type :summary}
```

OK, so that fix worked! Let's take a look at the next class of errors.

``` text
ERROR in (raw-response-values) (/home/jmglov/Documents/code/clojure/awno-api/test/src/net/jmglov/awno/client/test_double_test.clj:33)
Uncaught exception, not in assertion.
expected: nil
  actual: clojure.lang.ExceptionInfo: null
```

## Abort!

It was at this point that I ran out of steam. I apologise for my lack of
dedication to the art of self-flagellation and will flagellate myself
accordingly.

![Abort!][abort]

[abort]: assets/2023-11-12-abort.jpg "A supercar driving out of the window of a skyscraper" width=800px
