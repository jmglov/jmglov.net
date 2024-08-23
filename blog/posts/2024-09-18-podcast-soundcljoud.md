Title: Building a podcast with Clojure
Date: 2024-09-18
Tags: clojure,babashka,scittle,clonejure,clojurescript,soundcljoud
Description: In which I use Clojure to illustrate the benefits of organising tech workers
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1726658139071349
Image: assets/2024-09-18-podcast-soundcljoud-preview.png
Image-Alt: The words "Organising Tech in Sweden" superimposed on raised fists with a Swedish flag with a circuit board pattern in the background


In addition to spending far too much of my time doing silly things with Clojure
and then even farther too much of my time writing about doing silly things with
Clojure, I spend some of my time thinking about, talking about, and
participating in labour organising here in Sweden. As I was talking about unions
and such to Ray one day, no doubt six tangents into one of my usual rambling
explorations of an idea, he interrupted my flow. "Stop!" he said, "for I have a
plan so cunning you could pin a tail on it and call it a weasel!" Curiosity
piqued, I enquired as to the nature of said plan. "We should make a podcast," he
continued, "and on this podcast, we should talk about tech workers and why it
makes sense for them to unionise. And we should focus on Sweden, since it's a
fairly unique labour market, plus you know interesting people who we could
interview."

"Ray," I rejoined, "that truly is a plan of weasel-grade cunning. I have but one
suggestion that will turn this good idea into a great one." "And what," quoth
he, "pray tell, is that suggestion?" "Babashka! Scittle! Clojure!" I exclaimed,
so full of excitement I was having troubling supporting my proper nouns with
clauses of explanatory power. "We could use all of this amazing technology for
all of the heavy lifting around making a podcast! We could [build a website
using S3 static hosting](2022-06-24-s3-https.html), then we could use an
approach similar to [how I built my
blog](https://jmglov.net/blog/tags/blog.html) to create pages for episodes with
show notes and transcripts and all that good stuff!"

So it was agreed, and thus [Organising Tech in Sweden](https://orgtech.se) came
to be.

![The words 'Organising Tech in Sweden' superimposed on raised fists with a Swedish flag with a circuit board pattern in the background][preview]
[preview]: assets/2024-09-18-podcast-soundcljoud-orgtech.png "Divided we beg, united we bargain!" width=512px

## Building the website

As with any of my recent projects, my first step is always to create a directory
and drop a [Scittle](https://github.com/babashka/scittle/)-enabled `bb.edn` in
it:

``` text
: ~; mkdir ~/code/orgtech-se
: ~; cd !$
```

**bb.edn**

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:git/sha "e203166a020509d126149ff8046489857ce5c89f"}}
 :tasks
 {http-server {:doc "Starts http server for serving static files"
               :requires ([babashka.http-server :as http])
               :task (do (http/serve {:port 1341 :dir "public"})
                         (println "Serving static assets at http://localhost:1341"))}

  browser-nrepl {:doc "Start browser nREPL"
                 :requires ([sci.nrepl.browser-server :as bp])
                 :task (bp/start! {})}

  -dev {:depends [http-server browser-nrepl]}

  dev {:task (do (run '-dev {:parallel true})
                 (deref (promise)))}}}
```

Since this is a static website, we can create a static `public/index.html` for
it, with the [usual favicon](2022-07-05-hacking-blog-favicon.html) and [social
sharing](2022-08-17-hacking-blog-sharing.html) stuff:

``` html
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>Organising Tech in Sweden Podcast</title>

  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width" />

  <link rel="stylesheet" href="/css/main.css">

  <!-- Favicon from https://realfavicongenerator.net/ -->
  <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
  <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
  <link rel="manifest" href="/site.webmanifest">
  <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#5bbad5">
  <meta name="msapplication-TileColor" content="#da532c">
  <meta name="theme-color" content="#ffffff">

  <!-- Social sharing (Facebook, Twitter, LinkedIn, etc.) -->
  <meta name="title" content="Organising Tech in Sweden">
  <meta name="twitter:title" content="Organising Tech in Sweden">
  <meta property="og:title" content="Organising Tech in Sweden">
  <meta property="og:type" content="website">

  <meta name="description" content="A limited podcast series exploring union organising in Swedish tech companies">
  <meta name="twitter:description"
    content="A limited podcast series exploring union organising in Swedish tech companies">
  <meta property="og:description" content="A limited podcast series exploring union organising in Swedish tech companies">

  <meta name="twitter:url" content="https://orgtech.se/">
  <meta property="og:url" content="https://orgtech.se/">

  <meta name="twitter:image" content="https://orgtech.se/img/orgtech-se-preview.jpg">
  <meta name="twitter:card" content="summary_large_image">
  <meta property="og:image" content="https://orgtech.se/img/orgtech-se-preview.jpg">
  <meta property="og:image:alt"
    content="Podcast logo: 'Organising Tech in Sweden' superimposed on raised fists with a Swedish flag with a circuit board pattern in the background">
</head>

<body>
  <div id="wrapper">
    <div id="left-side">
      <div id="cover-image">
        <img src="/img/orgtech-se-cover.jpg"
          title="Organising Tech in Sweden"
          alt="Podcast logo: 'Organising Tech in Sweden' superimposed on raised fists with a Swedish flag with a circuit board pattern in the background" />
      </div>
      <div id="aggregators-1">
        <div id="apple">
          <a class="apple-button"
            href="https://podcasts.apple.com/us/podcast/organising-tech-in-sweden/id1766442275?itsct=podcast_box_badge&amp;itscg=30200&amp;ls=1">
            <img src="https://tools.applemediaservices.com/api/badges/listen-on-apple-podcasts/badge/en-us?size=250x83&amp;releaseDate=1725494400"
              title="Listen on Apple Podcasts"
              alt="Listen on Apple Podcasts"
              class="apple-button">
          </a>
        </div>
        <div id="spotify">
          <a href="https://open.spotify.com/show/53psoLoX187axvmgb80l1x">
            <img src="/img/spotify-podcast-badge-blk-grn-330x80.svg"
              title="Listen on Spotify"
              alt="Listen on Spotify">
          </a>
        </div>
      </div>
      <div id="aggregators-2">
        <div id="podbean">
          <a href="https://www.podbean.com/podcast-detail/2r2tz-31b053/Organising-Tech-in-Sweden-Podcast"
            rel="noopener noreferrer" target="_blank">
            <img src="https://pbcdn1.podbean.com/fs1/site/images/badges/w600_1.png"
              title="Listen on Podbean"
              alt="Listen on Podbean">
          </a>
        </div>
      </div>
    </div>
    <div id="main">
      <div id="header">
        <h1 id="title" class="header">Episode 1 is out now!</h1>
        <!-- <h1 id="title" class="header"><a href="episodes/">Episodes</a></h1> -->
        <div id="socials">
          <a href="https://x.com/orgtech_se">
            <img src="/img/twitter-color-svgrepo-com.svg"
              title="Follow us on Twitter!"
              alt="Twitter logo" />
          </a>
          <a href="https://bsky.app/profile/orgtech-se.bsky.social">
            <img src="/img/bluesky-logo.svg"
              title="Follow us on Bluesky!"
              alt="Bluesky logo" />
          </a>
        </div>
      </div>
      <div class="text">
        <p>
          Organising Tech in Sweden is a limited podcast series exploring union
          organising in Swedish tech companies. Join us as we sit down with some
          of the people involved in the campaigns to win collective bargaining
          rights at two of Sweden's tech unicorns, Klarna and Spotify.
        </p>
        <div id="production-info">
          <div>
            <p>
              Listen to our latest episode:<br />
              🔊 <a href="/episodes/ep01-klarna-part1">Organising Klarna - Part 1</a>
            </p>
            <p>
              Produced by Hakuna Matata Produktion
            </p>
            <p>
              Cover art by <a href="https://anyakjordan.com/">Anya K. Jordan</a>
              <a href="https://bsky.app/profile/anyakjordan.bsky.social">@anyakjordan.bsky.social</a>
            </p>
            <p>
              Theme music by <a href="https://soundcloud.com/ptzery">Ptzery</a>
            </p>
          </div>
          <div id="hmp-logo">
            <img src="/img/hakuna-matata-produktion.png"
              title="Hakuna Matata Produktion"
              alt="Hakuna Matata Produktion logo">
          </div>
        </div>
      </div>
    </div>
  </div>
  <div id="news">
    <h1>News</h1>
    <h2>Episode 1 is out!</h2>
    <p>🔊 <a href="/episodes/ep01-klarna-part1">Organising Klarna - Part 1</a></p>
    <p>
      We kick off Organising Tech in Sweden in style by recounting the story of
      how a collective bargaining agreement (CBA) was won at Klarna, a major
      Swedish fintech. In fact, Klarna was the first unicorn in Sweden to be
      unionised (and probably the first unicorn in Europe as well)!
    </p>
    <p>
      To hear all about how this went down, your co-hosts Josh and Ray are joined by
      Thomas, the founder of the Klarna Unionen Club (a union "local", to use
      terminology that might be more familiar to US listeners); Sen, the chair of
      the club who won the bargaining agreement against the odds; and Kim, a former
      Klarna employee with extensive knowledge of Swedish labour law and market
      policy.
    </p>
  </div>
</body>

</html>
```

We can grab all the nice images and such from the interwebs:

``` text
: ~/code/orgtech-se; curl \
  https://orgtech.se/orgtech-se-favicon-and-img.tar.gz \
  | tar xvz -C public
```

And of course we need to make it nice and responsive so it looks good both on a
computer screen and a mobile phone screen. Let's create `public/css/main.css`
and drop some stylish styles therein:

``` css
body {
  font:
    1.2em Helvetica,
    Arial,
    sans-serif;
  margin: 20px;
  padding: 0;
}

body > div {
  max-width: 100%;
  margin-left: auto;
  margin-right: auto;
}

img {
  max-width: 100%;
}

a {
  text-decoration: none;
  &:hover {
    text-decoration: underline;
  }
}

#header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

h1 {
  font-weight: bold;
  font-size: larger;
}

#socials {
  display: flex;
  gap: 10px;
}

#socials img {
  max-width: 32px;
  &:hover {
    transform: scale(1.1);
  }
}

@media screen and (min-width: 600px) {

  body > div {
    max-width: 800px;
    margin-top: 1em;
  }

  #wrapper {
    display: flex;
  }

  #cover-image {
    margin-right: 20px;
    max-width: 40%;
  }

}
```

Now we can fire up a local webserver:

``` text
: ~/code/orgtech-se; bb dev
Serving assets at http://localhost:1341
Serving static assets at http://localhost:1341
nREPL server started on port 1339...
Websocket server started on 1340...
```

Gaze ye now upon the glories of http://localhost:1341!

![The words 'Organising Tech in Sweden' superimposed on raised fists with a Swedish flag with a circuit board pattern in the background][index]
[index]: assets/2024-09-18-podcast-soundcljoud-index.png "What even is a computer?" width=800px border=1

## Publishing the website

We of course have already registered a domain and done the intricate dance of
[setting up S3 static website hosting and CloudFront and all of
that](2022-06-24-s3-https.html), so all we need to do to publish our website is
copy some files into our S3 bucket. And of course, what better way to do this
than with a [Babashka task](https://book.babashka.org/#tasks)?

As avid REPL-drivers, we want to use our REPL for task development as well, so
the first thing we do is create a `tasks.clj` with a boring `publish` function
in it:

``` clojure
(ns tasks)

(defn publish [{:keys [website-bucket out-dir] :as opts}]
  (println (format "Publishing %s/ to s3://%s/"
                   out-dir website-bucket)))
```

Now we need to hook that up to `bb.edn` by setting the classpath appropriately,
pulling in our new `tasks` namespace, defining some options, and adding a
`publish` task:

``` clojure
{:deps { ... }
 :paths ["."]
 :tasks
 {:requires ([tasks])
  :init (def opts
          {:website-bucket "orgtech.se"
           :out-dir "public"})
  ;; ...
  publish (tasks/publish opts)}}
```

We can now test this:

``` text
: ~/code/orgtech-se; bb publish
Publishing public/ to s3://orgtech.se/
```

Jumping back to `tasks.clj`, we fire up a trusty
[CIDER](https://docs.cider.mx/cider/index.html) REPL with a **C-c M-j**
(`cider-jack-in-clj`) flourish, followed by **C-c C-k** (`cider-load-buffer`) to
evaluate the buffer (readers following along with an
[inferior](https://www.vim.org/) [text](https://www.jetbrains.com/idea/)
[editor](https://code.visualstudio.com/) will have to perform whatever complex
ritual necessary to start a REPL and connect to it and then evaluate the "file"
or whatever your text editor calls the thing you're editing).

Thus equipped, we can open up a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment),
define some `opts`, and evaluate our `publish` function:

``` clojure
(comment

  (def opts {:website-bucket "orgtech.se"
             :out-dir "public"})  ; C-c C-v f c e
  ;; => #'tasks/opts

  (publish opts)  ; C-c C-e

  )
```

Our REPL buffer now looks something like this:

``` text
Started nREPL server at 127.0.0.1:44571
For more info visit: https://book.babashka.org/#_nrepl
;; Connected to nREPL server - nrepl://127.0.0.1:44571
;; CIDER 1.12.0 (Split), babashka.nrepl 0.0.6-SNAPSHOT
;; Babashka 1.3.188
;;     Docs: (doc function-name)
;;           (find-doc part-of-name)
;;   Source: (source function-name)
;;  Javadoc: (javadoc java-object-or-class)
;;     Exit: <C-c C-q>
;;  Results: Stored in vars *1, *2, *3, an exception in *e;
;;  Startup: /home/jmglov/.nix-profile/bin/bb nrepl-server localhost:0
Publishing public/ to s3://orgtech.se/
user> 
```

OK, now it's time to figure out how to do the actual copying of files to S3. We
could of course use the spectacular
[awyeah-api](https://github.com/grzm/awyeah-api) to do stuff to AWS right from
our Clojure code, but that smacks of effort. 🤔

Fortunately, we remember that Babashka was originally conceived as a replacement
for Bash shell scripting (I mean, the "bash" is right there in the name, so
that's kind of a major clue), and we know that there's an [AWS command line
tool](https://aws.amazon.com/cli/) that knows how to sync stuff from a local
directory to S3:

``` text
: ~/code/orgtech-se; aws s3 sync help
SYNC()                                                                  SYNC()

NAME
       sync -

DESCRIPTION
       Syncs  directories  and S3 prefixes. Recursively copies new and updated
       files from the source directory to the destination. Only creates  fold-
       ers in the destination if they contain one or more files.

SYNOPSIS
            sync
          <LocalPath> <S3Uri> or <S3Uri> <LocalPath> or <S3Uri> <S3Uri>

[...]

EXAMPLES

       The following sync command syncs objects from a local diretory  to  the
       specified  prefix and bucket by uploading the local files to s3.  A lo-
       cal file will require uploading if the size of the local file  is  dif-
       ferent  than  the  size of the s3 object, the last modified time of the
       local file is newer than the last modified time of the  s3  object,  or
       the  local  file  does not exist under the specified bucket and prefix.
       In this example, the user syncs the bucket mybucket to the  local  cur-
       rent  directory.   The  local  current  directory  contains  the  files
       test.txt and test2.txt.  The bucket mybucket contains no objects:

          aws s3 sync . s3://mybucket

       Output:

          upload: test.txt to s3://mybucket/test.txt
          upload: test2.txt to s3://mybucket/test2.txt

[...]
```

This looks like just the thing we need, so let's use the power of
[babashka.process](https://github.com/babashka/process) to invoke `aws s3 sync`:

``` clojure
(ns tasks
  (:require [babashka.process :as p]))

(defn publish [{:keys [website-bucket out-dir] :as opts}]
  (let [sync-cmd ["aws s3 sync"
                  (format "%s/" out-dir)
                  (format "s3://%s/" website-bucket)]]
    (apply println sync-cmd)
    (apply p/shell sync-cmd)))

(comment

  (def opts {:website-bucket "orgtech.se"
             :out-dir "public"})  ; C-c C-v f c e
  ;; => #'tasks/opts

  (publish opts)  ; C-c C-e

  )
```

After a brief delay, our REPL buffer now helpfully tells us:

``` text
aws s3 sync public/ s3://orgtech.se/
user> 
```

And if we have a look in that there bucket, we see some files:

``` text
: ~/code/orgtech-se; aws s3 ls --recursive s3://orgtech.se/
2024-08-23 10:40:05     102613 android-chrome-192x192.png
2024-08-23 10:40:05     337153 android-chrome-512x512.png
2024-08-23 10:40:05      96934 apple-touch-icon.png
2024-08-23 10:40:05        246 browserconfig.xml
2024-08-23 10:40:05        720 css/main.css
2024-08-23 10:40:05      47189 favicon-16x16.png
2024-08-23 10:40:05      48597 favicon-32x32.png
2024-08-23 10:40:05      12014 favicon.ico
2024-08-23 10:40:05        745 img/bluesky-logo.svg
2024-08-23 10:40:05    3231594 img/orgtech-se-cover.jpg
2024-08-23 10:40:05     513884 img/orgtech-se-preview.jpg
2024-08-23 10:40:05       1943 img/twitter-color-svgrepo-com.svg
2024-08-23 10:40:05       1933 img/volume.png
2024-08-23 10:40:05       3074 index.html
2024-08-23 10:40:05      33084 mstile-150x150.png
2024-08-23 10:40:05        426 site.webmanifest
```

And now browsing to [https://orgtech.se/](https://orgtech.se/) reveals a lovely
little website that looks just like the one on http://localhost:1341. 🎉

Let's change the header in `public/index.html` to test out the syncing:

``` html
        <h1 id="title" class="header">Coming Thursday, 12 September!</h1>
```

Before we YOLO eval our `publish` function again, we notice that `aws s3 sync`
has a lovely little `--dryrun` option, which doesn't actually do the stuff but
rather prints out what stuff it would do. Let's implement this!

**tasks.clj**

``` clojure
(defn publish [{:keys [website-bucket out-dir dryrun]
                :as opts}]
  (let [sync-cmd (concat ["aws s3 sync"]
                         (when dryrun ["--dryrun"])
                         [(format "%s/" out-dir)
                          (format "s3://%s/" website-bucket)])]
    (apply println sync-cmd)
    (apply p/shell sync-cmd)))

(comment

  (publish (assoc opts :dryrun true))  ; C-c C-e

  )
```

The REPL window helpfully says:

``` text
aws s3 sync --dryrun public/ s3://orgtech.se/
user> 
```

but we don't see the output of the `aws s3 sync` command itself. This is due to
the REPL not capturing stdout for the subprocess, I guess. We can handle this
thusly:

``` clojure
(ns tasks
  (:require [babashka.process :as p]))

(defn shell [& args]
  (let [p (apply p/shell {:out :string
                        :err :string
                        :continue true}
                 args)]
    (println (:out p))
    (when-not (zero? (:exit p))
      (println (:err p)))
    p))

(defn publish [{:keys [website-bucket out-dir dryrun]
                :as opts}]
  (let [sync-cmd (concat ["aws s3 sync"]
                         (when dryrun ["--dryrun"])
                         [(format "%s/" out-dir)
                          (format "s3://%s/" website-bucket)])]
    (apply println sync-cmd)
    (apply shell sync-cmd)))

(comment

  (publish (assoc opts :dryrun true))  ; C-c C-e

  )
```

And now the REPL sez:

``` text
aws s3 sync --dryrun public/ s3://orgtech.se/
(dryrun) upload: public/index.html to s3://orgtech.se/index.html

user> 
```

This is what we expect to see: only `index.html` will be uploaded, since it's
the only thing that has changed.

It would be nice to run this from the command line, but we currently have no way
of passing the `dryrun` option through short of adding it to the `opts` map in
`bb.edn`. Fortunately for us, there's
[babashka-cli](https://github.com/babashka/cli), which does all sorts of awesome
command-line parsing! Let's put it to work:

``` clojure
(ns tasks
  (:require [babashka.cli :as cli]
            [babashka.process :as p]))

;; ...

(comment

  (cli/parse-opts ["--website-bucket" "orgtech.se"
                   "--out-dir" "public"
                   "--dryrun"])
  ;; => {:website-bucket "orgtech.se", :out-dir "public", :dryrun true}

  )
```

Now we can use `parse-opts` in our `publish` function like so:

``` clojure
(defn publish [default-opts]
  (let [{:keys [website-bucket out-dir dryrun]
         :as opts} (merge default-opts
                          (cli/parse-opts *command-line-args*))
        sync-cmd (concat ["aws s3 sync"]
                         (when dryrun ["--dryrun"])
                         [(format "%s/" out-dir)
                          (format "s3://%s/" website-bucket)])]
    (apply println sync-cmd)
    (apply shell sync-cmd)))
```

Running this from the command line, we get the desired result:

``` text
: ~/code/orgtech-se; bb publish --dryrun
aws s3 sync --dryrun public/ s3://orgtech.se/
(dryrun) upload: public/index.html to s3://orgtech.se/index.html
```

And if we omit the `--dryrun` arg:

``` text
: ~/code/orgtech-se; bb publish --dryrun
aws s3 sync --dryrun public/ s3://orgtech.se/
upload: public/index.html to s3://orgtech.se/index.html
```

Amazing!

## Do you invalidate parking?

If we open [https://orgtech.se/index.html](https://orgtech.se/index.html) in a
browser and view source, however, we get a nasty surprise: the lovely newline we
added at the end of the file isn't there! What in the world is going on here?

Well, it turns out that one of the primary functions of a CDN (Content
Distribution Network) like CloudFront is to cache responses so every request
that hits an endpoint doesn't have to go all the way back to the origin (in this
case, our S3 bucket) to serve the response. So we've fallen prey to #2 in the
list of the 4 hardest problems in Computer Science:

1. Naming things
2. Caching things
3. Off by one errors

What to do, what to do?

Luckily for us, CloudFront gives us a way to invalidate the cache so the first
request for a given endpoint re-fetches from the origin. Even more luckily for
us, the AWS CLI surfaces this:

``` text
: ~/code/orgtech; aws cloudfront create-invalidation help
CREATE-INVALIDATION()                                    CREATE-INVALIDATION()

NAME
       create-invalidation -

DESCRIPTION
       Create a new invalidation.

       See also: AWS API Documentation

SYNOPSIS
            create-invalidation
          --distribution-id <value>
          [--paths <value>]
[...]

OPTIONS
       --distribution-id (string)  The distribution's id.
       --paths  (string)           The space-separated  paths to be invalidated.
[...]
```

So what we can do is create an invalidation right after syncing to the S3 bucket
in our `publish` function. In order to do this, we'll need a distribution ID.
Let's ask CloudFront about the distributions we have:

``` text
: ~/code/orgtech; aws cloudfront list-distributions \
  | bb -i '(let [ds (-> (str/join "\n" *input*)
                    (json/parse-string true)
                    (get-in [:DistributionList :Items]))]
             (map (juxt #(get-in % [:Aliases :Items 0]) :Id) ds))'
(["www.jmglov.net" "F2ABC12UVWXYZ9"]
 ["politechspod.com" "F7E33IJKLMN0P6"]
 ["www.orgtech.se" "FDCBA42RSTUV3"])
```

This looks like the one we're after:

``` text
["www.orgtech.se" "FDCBA42RSTUV3"]
```

Let's go ahead and add the distribution ID to our `bb.edn`:

``` clojure
{ ; ...
 {:requires ([tasks])
  :init (def opts
          {:website-bucket "orgtech.se"
           :out-dir "public"
           :distribution-id "FDCBA42RSTUV3"})
  ;; ...
  }}
```

Now we can use this in `tasks.clj`:

``` clojure
(defn publish [default-opts]
  (let [{:keys [website-bucket out-dir distribution-id dryrun]
         :as opts} (merge default-opts
                          (cli/parse-opts *command-line-args*))
        sync-cmd (concat ["aws s3 sync"]
                         (when dryrun ["--dryrun"])
                         [(format "%s/" out-dir)
                          (format "s3://%s/" website-bucket)])
        invalidate-cmd ["aws cloudfront create-invalidation"
                        "--distribution-id" distribution-id
                        "--paths" :???]]
        ;; ...
      ))
```

OK, now where can we get our paths? Well, recall that `aws s3 sync --dryrun`
helpfully outputs what is to be done:

``` text
aws s3 sync --dryrun public/ s3://orgtech.se/
(dryrun) upload: public/index.html to s3://orgtech.se/index.html
```

Let's consume this from Babashka to grab the paths! First, we'll dirty the
dishes:

``` text
: orgtech-se; touch public/index.html public/css/main.css 

: orgtech-se; aws s3 sync --dryrun public/ s3://orgtech.se/
(dryrun) upload: public/css/main.css to s3://orgtech.se/css/main.css
(dryrun) upload: public/index.html to s3://orgtech.se/index.html
```

And then parse that output in our `tasks.clj`:

``` clojure
(ns tasks
  (:require ; ...
            [clojure.string :as str]))

(comment

  (def default-opts {:website-bucket "orgtech.se"
                     :out-dir "public"
                     :distribution-id "FDCBA42RSTUV3"})  ; C-c C-v f c e
  ;; => #'tasks/default-opts

  (->> (shell "aws s3 sync --dryrun public/ s3://orgtech.se/")
       :out
       str/split-lines
       (map #(str/replace % #"^[(]dryrun[)] upload: public(/\S+) to .+$" "$1")))
  ;; => ("/css/main.css" "/index.html")

  )
```

Now that we know how to determine which files have changed, let's plug this into
our `publish` function to add to the `aws cloudfront create-invalidation`
command:

``` clojure
(defn publish [default-opts]
  (let [{:keys [website-bucket out-dir distribution-id dryrun]
         :as opts} (merge default-opts
                          (cli/parse-opts *command-line-args*))
        sync-cmd (concat ["aws s3 sync"]
                         (when dryrun ["--dryrun"])
                         [(format "%s/" out-dir)
                          (format "s3://%s/" website-bucket)])
        ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
        paths-re (re-pattern (format "^[(]dryrun[)] upload: %s(/\\S+) to .+$"
                                     out-dir))
        invalidate-cmd (concat ["aws cloudfront create-invalidation"
                                "--distribution-id" distribution-id
                                "--paths"]
                               (->> (apply shell (concat sync-cmd ["--dryrun"]))
                                    :out
                                    str/split-lines
                                    (map #(str/replace % paths-re "$1"))))
        ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆
        ]
    (apply println sync-cmd)
    (apply shell sync-cmd)
    ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇
    (apply println invalidate-cmd)
    (when-not dryrun
      (apply shell invalidate-cmd))
    ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆
    ))

(comment

  (publish (assoc default-opts :dryrun true)) ; C-c C-e

  )
```

Our REPL buffer duly notes:

``` text
aws s3 sync --dryrun public/ s3://orgtech.se/
(dryrun) upload: public/css/main.css to s3://orgtech.se/css/main.css
(dryrun) upload: public/index.html to s3://orgtech.se/index.html

aws cloudfront create-invalidation --distribution-id FDCBA42RSTUV3
                                   --paths /css/main.css /index.html
```

Looks good, so let's try it for realz:

``` text
: ~/code/orgtech; bb publish
aws s3 sync public/ s3://orgtech.se/
aws cloudfront create-invalidation --distribution-id FDCBA42RSTUV3
                                   --paths /css/main.css /index.html
{
    "Location": "https://cloudfront.amazonaws.com/2020-05-31/distribution/FDCBA42RSTUV3/invalidation/ICECSBHVIW089I89RLYODUBMXI",
    "Invalidation": {
        "Id": "ICECSBHVIW089I89RLYODUBMXI",
        "Status": "InProgress",
        "CreateTime": "2024-08-25T07:14:55.130Z",
        "InvalidationBatch": {
            "Paths": {
                "Quantity": 2,
                "Items": [
                    "/css/main.css",
                    "/index.html"
                ]
            },
            "CallerReference": "cli-1724570094-253923"
        }
    }
}
```

This is promising. Let's refill our coffee and then check to see if the
invalidation has finishing invalidating:

``` text
: ~/code/orgtech; aws cloudfront get-invalidation \
  --distribution-id FDCBA42RSTUV3 \
  --id ICECSBHVIW089I89RLYODUBMXI
{
    "Invalidation": {
        "Id": "ICECSBHVIW089I89RLYODUBMXI",
        "Status": "Completed",
        "CreateTime": "2024-08-25T07:14:55.130Z",
        "InvalidationBatch": {
            "Paths": {
                "Quantity": 2,
                "Items": [
                    "/css/main.css",
                    "/index.html"
                ]
            },
            "CallerReference": "cli-1724570094-253923"
        }
    }
}
```

If we now Shift-reload the page in our browser, we'll see the wonderful new
header! 🎉

## Transcription made easy

Now that we have an amazing website and a way to publish it, let's record an
episode and then make a nice trailer to get people pumped up! We'll use
[Zencastr](https://zencastr.com/) to do this, which produces a lovely MP3 for us
as well as a transcript. For now, we have the following files on disk:

``` text
: organising-tech-in-sweden; tree
.
├── bb.edn
├── ep00-trailer
│   ├── otis-ep00-trailer.mp3
│   └── otis-ep0-trailer_transcription.txt
├── ep01-klarna-part1
│   ├── otis-ep01-klarna-part1.mp3
│   └── otis-ep01-klarna-part1_transcription.txt
├── public
│   ├── ...
│   └── index.html
└── tasks.clj
```

Zencastr's transcripts are, um, functional, shall we say, but any machine
transcription tool will require a human who speaks the actual language being
transcribed (in this case, English) to clean things up. Luckily, there's an
amazing free (as in source **and** as in beer!) browser-based tool called
[oTranscribe](https://otranscribe.com/) that lets us listen to our lovely audio
whilst editing the transcript, with keyboard shortcuts for pausing and resuming
playback, rewinding and fast forwarding, adjusting playback speed, etc.

To unlock all this goodness, we'll need to convert our boring Zencastr
transcripts, which look like this:

``` text
00:02.00
jmglov
Already and we are live now. So welcome everyone to organizing tech in Sweden
I am here my name is Josh I'm here with a ah. Cast of characters that will
delight in a maze and I will introduce them here in a minute but before we get
to the cool people. Let me introduce. My co-host Ray joining us all the way from
Belgium Ray you want to say hey.

00:30.61
Ray
Yeah on uncool Belgium hello everyone? Well it's a bit warmer than Sweden now.
But okay I yeah you were talking about being oh yeah, okay fine.

00:42.78
jmglov
Yeah, all right? So we are here like I said to talk about organizing tech in
Sweden and um, basically what we want to do is introduce. Folks who might not
know much about Sweden other than Ekea is from here and chocolate. Oh no wait.
That's Switzerland for some reason and the us. Oh you know Belgium sure sure.
Sure. Um.

[...]
```

into amazing OTR (oTranscribe's file format) ones, which look like some HTML
stuffed into some JSON.

To do this converting, we could use
[Transcribble](https://github.com/jmglov/transcribble), which I wrote a while
back and forgot to blog about. Or we could just open up
[https://otranscribe.com/](https://otranscribe.com/) in our browser and click
the big blue "Start transcribing" button, then click the "Choose audio (or
video) file" button and choose our
`~/code/orgtech-se/ep01-klarna-part1/otis-ep01-klarna-part1.mp3` file, and then
paste in our Zencastr transcript, warts and all. If we click the Play button (or
hit Esc, which is oTranscribe's play/pause keyboard shortcut), our episode will
start playing, and we can hit Ctrl+J to add a timestamp to the transcript when
we hear me say "So, welcome everyone to Organising Tech in Sweden". After much listening and editing, which we will just handwave away here, we now
have a pristine transcript!

![The oTranscribe transcription UI][otr]
[otr]: assets/2024-09-18-podcast-soundcljoud-otr.png "Cleaning this up is an exercise for the reader" width=800px border=1

We'll now click the "Export" button to pop up the "Download transcript as..."
dialog, select "oTranscribe format (.otr)", and save as a new
`~/code/orgtech-se/ep01-klarna-part1/otis-ep01-klarna-part1.otr` file.

## Feed me some episodes

Now that we have some files, we need to stuff those in a feed. Luckily, [we have
some experience with podcast
feeds](2024-07-09-soundcljoud.html#faking_a_podcast_with_selmer). Using
[Selmer](https://github.com/yogthos/Selmer) to write the feed worked out pretty
nicely then, so let's elect to do the same thing again. In fact, since we
already did the hard work of creating code that knows how to write an RSS file
for a music album, why don't we see if we can modify it a bit to support
podcasts as well?

Let's pop over to `~/code/soundcljoud/processor/main.clj` and remind
ourselves how we turned an album into an RSS feed:

``` clojure
(defn process-album [opts dir]
  (let [info (album-info opts dir)
        tmpdir (fs/create-temp-dir {:prefix "soundcljoud."})
        info (update info :tracks (partial map #(process-track % tmpdir)))]
    (spit (fs/file tmpdir "album.rss") (rss/album-feed opts info))
    (assoc info :out-dir tmpdir)))
```

In this case, we fetched Discogs metadata for the album in the `album-info`
function, created a temporary directory, did some transcoding in
`process-track`, then used `rss/album-feed` to apply a Selmer template to our
album metadata. Opening up the `soundcljoud.rss` namespace, we see that the
`album-feed` function is extremely specific to music albums:

``` clojure
(defn album-feed [opts album-info]
  (let [template (-> (io/resource "album-feed.rss") slurp)]
    (selmer/render template
                   (-> album-info
                       (update :tracks
                               (partial map #(update % :mp3-filename
                                                     fs/file-name)))
                       (assoc :date (now))))))
```

Whilst there's no obvious way to repurpose it, we can follow the same basic
pattern:

1. Load the template
2. Massage the "info" about the ~~album~~ podcast as needed
3. Render the template with our "info"

Let's sketch out a `podcast-feed` function:

``` clojure
(defn podcast-feed [opts podcast-info]  ;; ❓ podcast-info how?
  (let [template :???]  ;; ❓ where do we get this?
    (->> podcast-info
         ;; ❓ maybe some massaging here?
         (selmer/render template))))
```

The first question is where we get the `podcast-info`. We got `album-info` from
Discogs, but since Discogs presumably knows nothing about our podcast (and why
would it?), let's create a static `~/code/orgtech/podcast.edn` file instead,
fill it with whatever data our podcast feed will need (I guess it's time to
rhyme), and read in the EDN before calling this function.

Having made that decision, we must now ask ourselves where we will get our
podcast feed template from. In the case of albums, we provided a template as a
resource directly from Soundcljoud, so why don't we do that again?

``` clojure
(defn podcast-feed [opts podcast-info]
  (let [template (-> (io/resource "podcast-feed.rss") slurp)]
    (->> podcast-info
         ;; ❓ maybe some massaging here?
         (selmer/render template))))
```

In order to know what if any massaging `podcast-info` will need, we'll need to
create the template and the `podcast.edn` file and see where the gaps are. Let's
consult Apple's handy [A Podcaster’s Guide to
RSS](https://help.apple.com/itc/podcasts_connect/#/itcb54353390) and start
writing `resources/podcast-feed.rss`. First, we need the standard feed skeleton:

``` xml
<?xml version='1.0' encoding='UTF-8'?>
<rss version="2.0"
     xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
     xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <!-- TBD: some stuff here -->
  </channel>
</rss>
```

Now to start populating the contents `<channel>` tag. According to Apple, we
need the following:

| Show tags           | Usage                                      | Parent tag  |
|---------------------|--------------------------------------------|-------------|
| `<title>`           | The show title.                            | `<channel>` |
| `<description>`     | The show description.                      | `<channel>` |
| `<itunes:image>`    | The artwork for the show.                  | `<channel>` |
| `<language>`        | The language spoken on the show.           | `<channel>` |
| `<itunes:explicit>` | The podcast parental advisory information. | `<channel>` |
| `<itunes:category>` | The show category information.             | `<channel>` |

This is straightforward enough (except for `<itunes:category>`, which we'll come
back to):

``` xml
  <channel>
    <title>{{podcast.title}}</title>
    <description>{{podcast.description|safe}}</description>
    <itunes:image href="{{base-url}}{{podcast.image}}"/>
    <language>{{podcast.language}}</language>
    <itunes:explicit>{{podcast.explicit}}</itunes:explicit>
  </channel>
```

By the way, that `{{podcast.description|safe}}` thingy is a Selmer filter that
[exempts the variable from being
HTML-escaped](https://github.com/yogthos/Selmer?tab=readme-ov-file#safe). Since
our description text goes in the body of the `<description>` tag, we don't want
things like "rock & roll" getting rendered as "rock &amp; roll", because that
would be yucky.

Now we need to add that data to our `podcast.edn`:

``` clojure
{:base-url "https://orgtech.se"
 :podcast {:title "Organising Tech in Sweden"
           :description "Organising Tech in Sweden is a limited podcast series exploring union organising in Swedish tech companies. Join us as we sit down with some of the people involved in the campaigns to win collective bargaining rights at two of Sweden's tech unicorns, Klarna and Spotify."
           :image "/img/orgtech-se-cover.jpg"
           :language "en"
           :explicit true}}
```

As we were writing `podcast.edn`, we realised that `podcast-info` was actually
the data in the EDN file under the `:podcast` key, so we are really just
providing an `opts`:

``` clojure
(defn podcast-feed [opts]
  (let [template (-> (io/resource "podcast-feed.rss") slurp)]
    (->> opts
         ;; ❓ maybe some massaging here?
         (selmer/render template))))
```

Let's give this a go in our REPL:

``` clojure
(comment

  (require '[clojure.edn :as edn])
  ;; => nil

  (def opts (-> (slurp "/home/jmglov/code/orgtech-se/podcast.edn")
                (edn/read-string)))
  ;; => #'soundcljoud.rss/opts

  opts
  ;; => {:base-url "https://orgtech.se",
  ;;     :podcast
  ;;     {:title "Organising Tech in Sweden",
  ;;      :description
  ;;      "Organising Tech in Sweden is a...",
  ;;      :image "/img/orgtech-se-cover.jpg",
  ;;      :language "en",
  ;;      :explicit true}}

  (podcast-feed opts)
  ;; => "<?xml version='1.0' encoding='UTF-8'?>
  ;;     <rss version=\"2.0\"
  ;;          xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"
  ;;          xmlns:atom=\"http://www.w3.org/2005/Atom\">
  ;;       <channel>
  ;;         <title>Organising Tech in Sweden</title>
  ;;         <description>Organising Tech in Sweden is a...</description>
  ;;         <itunes:image href=\"https://orgtech.se/img/orgtech-se-cover.jpg\"/>
  ;;         <language>en</language>
  ;;         <itunes:explicit>true</itunes:explicit>
  ;;       </channel>
  ;;     </rss>"

  )
```

Let's now come back to that tricky `<itunes:category>` tag. Referring back to
the Apple docs, we see:

> For a complete list of categories and subcategories, see Apple Podcast
> categories.
>
> Select the category that best reflects the content of your show. If available,
> you can also define a subcategory.
>
> Single category:
>
>   `<itunes:category text="History" />`
> 
> Category with subcategory:
> 
>   `<itunes:category text="Society &amp; Culture">`
>   `  <itunes:category text="Documentary" />`
>   `</itunes:category>`

We can add categories to our `podcast-feed.rss` template using Selmer's
[for](https://github.com/yogthos/Selmer?tab=readme-ov-file#for) tag:

``` xml
  <channel>
    <!-- ... -->
{% for category in podcast.categories %}
    <itunes:category text="{{category.text}}">
{% for subcategory in category.subcategories %}
      <itunes:category text="{{subcategory.text}}" />
{% endfor %}
    </itunes:category>
{% endfor %}
  </channel>
```

And now we need to pick a category or two and add them to our `podcast.edn`. The
[Apple Podcasts
categories](https://podcasters.apple.com/support/1691-apple-podcasts-categories)
page lists the options, of which we choose:
- Technology (which has no subcategories)
- News, with subcategory Politics

Expressing this in EDN, we get:

``` clojure
{:base-url "https://orgtech.se"
 :podcast { ; ...
           :categories [{:text "Technology"}
                        {:text "News"
                         :subcategories [{:text "Politics"}]}]}}
```

And our REPL shows us what we'd expect to see:

``` clojure
(comment

  (def opts (-> (slurp "/home/jmglov/code/orgtech-se/podcast.edn")
                (edn/read-string)))
  ;; => #'soundcljoud.rss/opts

  (podcast-feed opts)
  ;; => "<?xml version='1.0' encoding='UTF-8'?>
  ;;     <rss version=\"2.0\"
  ;;          xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"
  ;;          xmlns:atom=\"http://www.w3.org/2005/Atom\">
  ;;       <channel>
  ;;         ...
  ;;         <itunes:category text=\"Technology\">
  ;;         </itunes:category>
  ;;         <itunes:category text=\"News\">
  ;;           <itunes:category text=\"Politics\" />
  ;;         </itunes:category>
  ;;       </channel>
  ;;     </rss>"

  )
```

Having sorted our required tags, let's take a look at Apple's recommended and
"situational" tags (which we'll just treat as "recommended"):

| Show tags           | Usage                                        | Parent tag  |
|---------------------|----------------------------------------------|-------------|
| `<itunes:author>`   | The group responsible for creating the show. | `<channel>` |
| `<link>`            | The website associated with a podcast.       | `<channel>` |
| `<itunes:title>`    | The show title specific for Apple Podcasts.  | `<channel>` |
| `<itunes:type>`     | The type of show. Its values can be one of the following: | |
| | • **Episodic**. Episodes are intended to be consumed without any specific order. | |
| | • **Serial**. Episodes are intended to be consumed in sequential order. | |
| `<copyright>`       | The show copyright details.                  | `<channel>` |

Again, this is quite straightforward to add to our template:

``` xml
  <channel>
    <!-- ... -->
    <itunes:author>{{podcast.author}}</itunes:author>
    <link>{{base-url}}</link>
    <itunes:title>{{podcast.title}}</itunes:title>
    <itunes:type>{{podcast.type}}</itunes:type>
    <copyright>{{podcast.copyright}}</copyright>
  </channel>
```

and to our `podcast.edn`:

``` clojure
{:base-url "https://orgtech.se"
 :podcast { ; ...
           :author "Organising Tech in Sweden"
           :type "Serial"
           :copyright "All rights reserved, Organising Tech in Sweden"}}
```

Testing things out in our REPL, we see what we expect to see. 🙂

Now it's time to add some episodes! Here are the Apple Podcast required,
recommended, and situational tags for episodes:

| Show tags           | Usage                                        | Parent tag  |
|---------------------|----------------------------------------------|-------------|
| `<title>`           | An episode title.                            | `<item>`    |
| `<enclosure>`       | The episode content, file size, and file type information. The `<enclosure>` tag has three attributes: | `<item>` |
| | • **URL**. The URL attribute points to your podcast media file. | |
| | • **Length**. The length attribute is the file size in bytes. | |
| | • **Type**. The type attribute provides the correct category for the type of file. | |
| `<guid>`            | The episode’s globally unique identifier ([GUID](https://cyber.harvard.edu/rss/rss.html#ltguidgtSubelementOfLtitemgt)) | `<item>` |
| `<pubDate>`         | The date and time when an episode was released. Format the date using the [RFC 2822](http://www.faqs.org/rfcs/rfc2822.html) specifications. For example: `Sat, 01 Apr 2023 19:00:00 GMT`. | `<item>` |
| `<description>`     | An episode description.                      | `<item>`    |
| `<itunes:duration>` | The duration of an episode. Different duration formats are accepted however it is recommended to convert the length of the episode into seconds. | `<item>` |
| `<link>`            | An episode link URL.                         | `<item>`    |
| `<itunes:explicit>` | The podcast parental advisory information.   | `<item>`    |
| `<itunes:title>`    | The show title specific for Apple Podcasts.  | `<item>`    |
| `<itunes:episode>`  | An episode number.                           | `<item>`    |
| `<itunes:episodeType>` | The episode type.                         | `<item>`    |
| | • **Full**. Specify full when you are submitting the complete content of your show. | |
| | • **Trailer**. Specify trailer when you are submitting a short, promotional piece of content that represents a preview of your current show. | |
| | • **Bonus**. Specify bonus when you are submitting extra content for your show (for example, behind the scenes information or interviews with the cast) or cross-promotional content for another show. | |
| `<itunes:transcript>` | A link to the episode transcript in the Closed Caption format. | `<item>` |

Unfortunately, Transcribble doesn't yet support VTT or SRT transcripts, so we
can't provide the transcript directly in iTunes. What we will do instead is
display the OTR transcript that we previously prepared in oTranscribe on our
episode page (which is yet to be written, but we'll get there in the end). In
order to do this, let's add a custom `<transcriptUrl>` tag.

Let's start with our template as usual:

``` xml
  <channel>
    <!-- ... -->
{% for episode in episodes %}
    <item>
      <title>{{episode.title}}</title>
      <enclosure
          url="{{base-url}}{{episode.path}}/{{episode.audio-file}}"
          length="{{episode.audio-filesize}}"
          type="{{episode.mime-type}}" />
      <guid>{{base-url}}{{episode.path}}/{{episode.audio-file}}</guid>
      <pubDate>{{episode.date}}</pubDate>
      <description><![CDATA[{{episode.description|safe}}]]></description>
      <itunes:duration>{{episode.duration}}</itunes:duration>
      <link>{{base-url}}{{episode.path}}</link>
      <itunes:title>{{episode.title}}</itunes:title>
      {% if episode.number %}<itunes:episode>{{episode.number}}</itunes:episode>{% endif %}
      <itunes:episodeType>{{episode.type}}</itunes:episodeType>
      <transcriptUrl>{{base-url}}{{episode.path}}/{{episode.transcript-file}}</transcriptUrl>
    </item>
{% endfor %}
  </channel>
```

And now we know what episodes need to look like in our `podcast.edn` file:

``` clojure
{ ; ...
 :episodes
 [{:number 0
   :date "Thu, 5 Sep 2024 00:00:00 +0000"
   :type "Trailer"
   :title "Trailer"
   :summary "Union organising seems to be in the air these days, as tech workers wake up and realise that they are, in fact, workers."
   :description "
<p>
  Union organising seems to be in the air these days, as tech workers wake up and
  realise that they are, in fact, workers. Here in Sweden, it's no exception.
  Join us as we sit down with some of the people involved in organising two of
  Sweden's foremost tech unicorns, Klarna and Spotify. This is Organising Tech in
  Sweden.
</p>
<p class=\"soundcljoud-hidden\">
  To view full show notes, including transcripts, please visit the
  <a href=\"{{base-url}}{{episode.path}}/\">episode page</a>.
</p>
<p>
  Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>
  <a href=\"https://bsky.app/profile/anyakjordan.bsky.social\">@anyakjordan.bsky.social</a>
</p>
<p>
  Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>
</p>"
   :path "/episodes/ep00-trailer"
   :audio-file "otis-ep00-trailer.mp3"
   :transcript-file "otis-ep00-trailer.otr"
   :explicit false
   :mime-type "audio/mpeg"}
  {:number 1
   :date "Thu, 12 Sep 2024 00:00:00 +0000"
   :type "Full"
   :title "Organising Klarna - Part 1"
   :summary "A conversation with three of the organisers behind the successful campaign to win a Collective Bargaining Agreement at Klarna"
   :description "
<p>
  We kick off Organising Tech in Sweden in style by recounting the story of how
  a collective bargaining agreement (CBA) was won at Klarna, a major Swedish
  fintech. In fact, Klarna was the first unicorn in Sweden to be unionised (and
  probably the first unicorn in Europe as well)!
</p>
<p>
  To hear all about how this went down, your co-hosts Josh and Ray are joined by
  Thomas, the founder of the Klarna Unionen Club (a union \"local\", to use
  terminology that might be more familiar to US listeners); Sen, the chair of
  the club who won the bargaining agreement against the odds; and Kim, a former
  Klarna employee with extensive knowledge of Swedish labour law and market
  policy.
</p>
<p>
  This is part 1 of the conversation, which will be concluded in Episode 2.
</p>
<p class=\"soundcljoud-hidden\">
  To view full show notes, including transcripts, please visit the
  <a href=\"{{base-url}}{{episode.path}}/\">episode page</a>.
</p>
<p>
  Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>
  <a href=\"https://bsky.app/profile/anyakjordan.bsky.social\">@anyakjordan.bsky.social</a>
</p>
<p>
  Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>
</p>"
   :path "/episodes/ep01-klarna-part1"
   :audio-file "otis-ep01-klarna-part1.mp3"
   :transcript-file "otis-ep01-klarna-part1.otr"
   :explicit false
   :mime-type "audio/mpeg"}
  {:preview? true
   :number 2
   :date "Thu, 19 Sep 2024 00:00:00 +0000"
   :type "Full"
   :title "Organising Klarna - Part 2"
   :summary "The conclusion of our conversation with three of the organisers behind the successful campaign to win a Collective Bargaining Agreement at Klarna"
   :description "
<p>
  We finish our conversation with Sen, Thomas, and Kim about how a collective
  bargaining agreement (CBA) was won at Klarna. In this episode, we cover the
  impact of immigrant workers on organising, the impact of organising on
  organisers, and the impact of strikes on negotiations. All of this and a happy
  ending too!
</p>
<p class=\"soundcljoud-hidden\">
  To view full show notes, including transcripts, please visit the
  <a href=\"{{base-url}}{{episode.path}}/\">episode page</a>.
</p>
<p>
  Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>
  <a href=\"https://bsky.app/profile/anyakjordan.bsky.social\">@anyakjordan.bsky.social</a>
</p>
<p>
  Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>
</p>"
   :path "/episodes/ep02-klarna-part2"
   :audio-file "otis-ep02-klarna-part2.mp3"
   :transcript-file "otis-ep02-klarna-part2.otr"
   :explicit false
   :mime-type "audio/mpeg"}]}
```

Testing this in our REPL...

``` clojure
(comment

  (def opts (-> (slurp "/home/jmglov/code/orgtech-se/podcast.edn")
                (edn/read-string)))
  ;; => #'soundcljoud.rss/opts

  (podcast-feed opts)
  ;; => java.lang.NullPointerException soundcljoud.rss /home/jmglov/code/soundcljoud/processor/src/soundcljoud/rss.clj:34:26

  )
```

...we get an unpleasant surprise. 😮

This is a bit annoying to debug, but we can surmise that one of the template
variables in the episode template must be missing. Doing a little visual
inspection identifies the culprit:

``` xml
      <enclosure
          url="{{base-url}}{{episode.path}}/{{episode.audio-file}}"
          length="{{episode.audio-filesize}}"
          type="{{episode.mime-type}}" />
```

We don't have `audio-filesize` in our episode data structure. 😢

## Deep tissue massage

All is not lost, however. Let's cast our minds back to the definition of the
`podcast-feed` function:

``` clojure
(defn podcast-feed [opts]
  (let [template (-> (io/resource "podcast-feed.rss") slurp)]
    (->> opts
         ;; ❓ maybe some massaging here?
         (selmer/render template))))
```

The answer to the question "maybe some massaging here?" now reveals itself to be
"Yes. Yes! A thousand times yes!" We also know at least one massage technique
we're going to need to use, namely setting the `audio-filesize` key for each
episode. Let's start out by giving ourselves a way to update episodes:

``` clojure
(defn update-episode [opts episode]
  episode)

(defn update-episodes [opts]
  (update opts :episodes #(map (partial update-episode opts) %)))

(defn podcast-feed [opts]
  (let [template (-> (io/resource "podcast-feed.rss") slurp)]
    (->> opts
         update-episodes
         (selmer/render template))))
```

Now we can figure out how to add the filesize to each episode. As usual,
Babashka's got us covered! Checking out the babashka.fs API documentation, we
find a function called
[babashka.fs/size](https://github.com/babashka/fs/blob/master/API.md#babashka.fs/size):

> **size**
>
> `(size f)`
>
> Returns the size of a file (in bytes).

Let's mess around a bit in the REPL:

``` clojure
(comment

  (def base-dir "/home/jmglov/code/orgtech-se")
  ;; => #'soundcljoud.rss/base-dir

  (def opts (-> (slurp (fs/file base-dir "podcast.edn"))
                (edn/read-string)
                (assoc :base-dir base-dir)))

  (let [episode (-> opts :episodes first)
        filename (format "%s%s/%s"
                         base-dir (:path episode) (:audio-file episode))]
    (fs/size filename))
  ;; => java.nio.file.NoSuchFileException: 
  ;; /home/jmglov/code/orgtech-se/episodes/ep00-trailer/otis-ep00-trailer.mp3 
  ;; /home/jmglov/code/soundcljoud/processor/src/soundcljoud/rss.clj:4:5

  )
```

Oops! Seems like we've traded one problem for another. 😬

On disk, the files are laid out like this:

``` text
: organising-tech-in-sweden; tree
.
├── bb.edn
├── ep00-trailer
│   ├── otis-ep00-trailer.mp3
│   └── otis-ep0-trailer_transcription.txt
├── ep01-klarna-part1
│   ├── otis-ep01-klarna-part1.mp3
│   └── otis-ep01-klarna-part1_transcription.txt
├── ep02-klarna-part2
│   ├── otis-ep02-klarna-part2.mp3
│   └── otis-ep02-klarna-part2_transcription.txt
├── public
│   ├── ...
│   └── index.html
└── tasks.clj
```

But we are looking for the audio file in the path in which it should exist on
the server, which makes sense from an RSS feed perspective, which should use
paths corresponding to the published site. Our `publish` task uses `aws s3 sync`
to publish everything in our `public/` directory, so if we drop the MP3s there,
they will get put in the correct place on the S3 website. For now, let's cheat
by using our REPL to put the files where they need to go:

``` clojure
(comment

  (def opts (-> (slurp (fs/file base-dir "podcast.edn"))
                (edn/read-string)
                (assoc :base-dir base-dir
                       :out-dir "public")))
  ;; => #'soundcljoud.rss/opts

  (doseq [episode (:episodes opts)
          :let [filename (format "%s/%s%s/%s"
                                 (:base-dir opts) (:out-dir opts)
                                 (:path episode) (:audio-file episode))
                src-filename (fs/file dir
                                      (fs/file-name (:path episode))
                                      (:audio-file episode))]]
    (when-not (fs/exists? filename)
      (fs/create-dirs (fs/parent filename))
      (fs/copy src-filename filename)))
  ;; => nil

  )
```

OK, this will do for now. Let's grab this code, clean it up a bit, and shove it
into our `update-episode` function:

``` clojure
(defn update-episode [{:keys [base-dir out-dir] :as opts}
                      {:keys [audio-file path] :as episode}]
  (assoc episode :audio-filesize
         (fs/size (format "%s/%s%s/%s" base-dir out-dir path audio-file))))
```

Before testing this out in the REPL, we should add the `:out-dir` key to our
`podcast.edn` so we don't rely on the caller to add it to `opts`:

``` clojure
{:base-url "https://orgtech.se"
 :podcast { ... }
 :episodes [ ... ]}
```

OK, now we're ready to give it a spin in the REPL:

``` clojure
(comment

  (podcast-feed (assoc opts :out-dir "public"))
  ;; => "<?xml version='1.0' encoding='UTF-8'?>\n
  ;;     <rss version=\"2.0\"\n
  ;;          xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\n
  ;;          xmlns:atom=\"http://www.w3.org/2005/Atom\">\n
  ;;       <channel>\n
  ;;         <title>Organising Tech in Sweden</title>\n
  ;;         <description>Organising Tech in Sweden is a...</description>\n
  ;;         <itunes:image href=\"https://orgtech.se/img/orgtech-se-cover.jpg\"/>\n
  ;;         <language>en</language>\n
  ;;         <itunes:explicit>true</itunes:explicit>\n\n
  ;;         <itunes:category text=\"Technology\">\n\n
  ;;         </itunes:category>\n\n
  ;;         <itunes:category text=\"News\">\n\n
  ;;           <itunes:category text=\"Politics\" />\n\n
  ;;         </itunes:category>\n\n
  ;;         <itunes:author>Organising Tech in Sweden</itunes:author>\n
  ;;         <link>https://orgtech.se</link>\n
  ;;         <itunes:title>Organising Tech in Sweden</itunes:title>\n
  ;;         <itunes:type>Serial</itunes:type>\n
  ;;         <copyright>All rights reserved</copyright>\n\n
  ;;         <item>\n
  ;;           <title>Trailer</title>\n
  ;;           <enclosure\n
  ;;               url=\"https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.mp3\"\n
  ;;               length=\"1016937\"\n
  ;;               type=\"audio/mpeg\" />\n
  ;;           <guid>https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.mp3</guid>\n
  ;;           <pubDate>Thu, 5 Sep 2024 00:00:00 +0000</pubDate>\n
  ;;           <description><![CDATA[\n
  ;;             <p>\n  Union organising seems to be in the air these days...</p>\n
  ;;             <p class=\"soundcljoud-hidden\">\n
  ;;               To view full show notes, including transcripts, please visit the\n
  ;;               <a href=\"{{base-url}}{{episode.path}}/\">episode page</a>.\n
  ;;             </p>\n
  ;;             <p>\n
  ;;               Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>\n
  ;;             </p>\n
  ;;             <p>\n
  ;;               Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>\n</p>
  ;;           ]]></description>\n
  ;;           <itunes:duration></itunes:duration>\n
  ;;           <link>https://orgtech.se/episodes/ep00-trailer</link>\n
  ;;           <itunes:title>Trailer</itunes:title>\n
  ;;           <itunes:episode>0</itunes:episode>\n
  ;;           <itunes:episodeType>Trailer</itunes:episodeType>\n
  ;;           <transcriptUrl>
  ;;             https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.otr
  ;;           </transcriptUrl>\n
  ;;         </item>\n\n
  ;;         ...
  ;;       </channel>\n
  ;;     </rss>\n"

  )
```

This looks like a good start, but a few things jump out at us:
1. Our `<itunes:duration>` tag is empty
2. We still have a few Selmer template variables in our output, for example: `<a
   href=\"{{base-url}}{{episode.path}}/\">episode page</a>`

Let's tackle the duration issue first, because we already have the tools to fix
that in the soundcljoud.processor code that we wrote for Garth:

``` clojure
(ns soundcljoud.audio
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as str]))

(defn mp3-duration [filename]
  (-> (p/shell {:out :string}
               "ffprobe -v quiet -print_format json -show_format -show_streams"
               filename)
      :out
      (json/parse-string keyword)
      :streams
      first
      :duration
      (str/replace #"[.]\d+$" "")))

;; ...
```

Let's pull soundcljoud.audio into our namespace and then grab the duration in
`update-episode`:

``` clojure
(ns soundcljoud.rss
  (:require ; ...
            [soundcljoud.audio :as audio])
  (:import ...))

;; ...

(defn update-episode [{:keys [base-dir src-dir] :as opts}
                      {:keys [audio-file path] :as episode}]
  (let [filename (format "%s/%s%s/%s" base-dir src-dir path audio-file)]
    (assoc episode
           :audio-filesize (fs/size filename)
           :duration (audio/mp3-duration filename))))

;; ...

(comment

  (podcast-feed opts)
  ;; => "<?xml version='1.0' encoding='UTF-8'?>\n
  ;;     <rss version=\"2.0\" ...>\n
  ;;       <channel>\n
  ;;         <title>Organising Tech in Sweden</title>\n
  ;;         <description>Organising Tech in Sweden is a...</description>\n
  ;;         ...
  ;;         <item>\n
  ;;           <title>Trailer</title>\n
  ;;           ...
  ;;           <itunes:duration>42</itunes:duration>\n
  ;;           ...
  ;;         </item>\n\n
  ;;         ...
  ;;       </channel>\n
  ;;     </rss>\n"

)
```

This looks good, so let's turn our roving eye to the last remaining problem.

## It's templates all the way down, young man

After rendering our RSS feed template, we somehow still have unrendered Selmer
in our output:

``` xml
<description>
  <![CDATA[
  <p>
    Union organising seems to be in the air these days...
  </p>
  <p class="soundcljoud-hidden">
    To view full show notes, including transcripts, please visit the
    <a href="{{base-url}}{{episode.path}}/">episode page</a>.
  </p>
  <p>
    Cover art by <a href="https://anyakjordan.com/">Anya K. Jordan</a>
  </p>
  <p>
    Theme music by <a href="https://soundcloud.com/ptzery">Ptzery</a>
  </p>]]>
</description>
```

Let's see what's going on in our `podcast-feed.rss` template for episodes:

``` xml
{% for episode in episodes %}
    <item>
      <title>{{episode.title}}</title>
      ...
      <description><![CDATA[{{episode.description|safe}}]]></description>
      ...
    </item>
{% endfor %}
```

So we're plugging `episode.description` into the template. Let's see what that
looks like in our `podcast.edn`:

``` clojure
{ ; ...
 :episodes
 [{:number 0
   :title "Trailer"
   ;; ...
   :description "
<p>
  Union organising seems to be in the air these days, as tech workers wake up and
  realise that they are, in fact, workers. Here in Sweden, it's no exception.
  Join us as we sit down with some of the people involved in organising two of
  Sweden's foremost tech unicorns, Klarna and Spotify. This is Organising Tech in
  Sweden.
</p>
<p class=\"soundcljoud-hidden\">
  To view full show notes, including transcripts, please visit the
  <a href=\"{{base-url}}{{episode.path}}/\">episode page</a>.
</p>
<p>
  Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>
  <a href=\"https://bsky.app/profile/anyakjordan.bsky.social\">@anyakjordan.bsky.social</a>
</p>
<p>
  Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>
</p>"
   ;; ...
   }
  ;; ...
]}
```

Ah-ha! The value of `episode.description` itself contains some templating. So it
looks like we need to render that as well.

``` clojure
(defn update-episode [{:keys [base-dir src-dir] :as opts}
                      {:keys [audio-file path] :as episode}]
  (let [filename (format "%s/%s%s/%s" base-dir src-dir path audio-file)]
    (assoc episode
           :audio-filesize (fs/size filename)
           :duration (audio/mp3-duration filename)
           :description (selmer/render (:description episode)
                                       (assoc opts :episode episode)))))

;; ...

(comment

  (podcast-feed opts)
  ;; => "<?xml version='1.0' encoding='UTF-8'?>\n
  ;;     <rss version=\"2.0\" ...>\n
  ;;       <channel>\n
  ;;         <title>Organising Tech in Sweden</title>\n
  ;;         <description>Organising Tech in Sweden is a...</description>\n
  ;;         ...
  ;;         <item>\n
  ;;           <title>Trailer</title>\n
  ;;           <description><![CDATA[\n
  ;;             <p>\n  Union organising seems to be in the air these days...</p>\n
  ;;             <p class=\"soundcljoud-hidden\">\n
  ;;               To view full show notes, including transcripts, please visit the\n
  ;;               <a href=\"https://orgtech.se/episodes/ep00-trailer/\">episode page</a>.\n
  ;;             </p>\n
  ;;             <p>\n
  ;;               Cover art by <a href=\"https://anyakjordan.com/\">Anya K. Jordan</a>\n
  ;;             </p>\n
  ;;             <p>\n
  ;;               Theme music by <a href=\"https://soundcloud.com/ptzery\">Ptzery</a>\n</p>
  ;;           ]]></description>\n
  ;;           ...
  ;;         </item>\n\n
  ;;         ...
  ;;       </channel>\n
  ;;     </rss>\n"

  )
```

OK, this looks much better! And in fact, it looks so much better that we can
declare victory and move on to figuring out how to write this beautiful feed to
disk!

To do that, let's jump back to our `orgtech-se/bb.edn` and add a task for
rendering the feed. We'll need to add the Soundcljoud processor to our deps,
then we can pretend we have a `tasks/render` function and call it:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:git/sha "e203166a020509d126149ff8046489857ce5c89f"}
        ;; You can always depend on Soundcljoud!
        ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
        io.github.jmglov/soundcljoud
        {:local/root "/home/jmglov/code/soundcljoud/processor"}
        ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆
        io.github.jmglov/transcribble
        {:local/root "/home/jmglov/code/transcribble/cli"}}
 :paths ["."]
 :tasks
 {
  ;; ...

  ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
  render {:doc "Create webpages from templates"
          :task (tasks/render opts)}
  ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆
   
  ;; ...
 }}
```

And now we pop over to `tasks.clj` to implement the task. Sadly, we need to
restart our REPL since we added a new dependency and I'm too lazy to learn how
to use the new `clojure.repl.deps.add-lib` from Clojure 1.12 (added to Babashka
in version
[1.4.192](https://github.com/babashka/babashka/releases/tag/v1.4.192)). In
Emacs, we can do this with **C-c C-z** (`cider-switch-to-repl-buffer`) to jump
to the REPL buffer, then **C-c C-q** (`cider-quit`) to stop the REPL, then **C-c
M-j** (`cider-jack-in-clj`) to start a new REPL. Easy peasy!

Thus armed with a new REPL, let's pull in the namespaces required to load our
`podcast.edn` and then actually load our `podcast.edn`:

``` clojure
(ns tasks
  (:require [babashka.cli :as cli]
            [babashka.process :as p]
            ;; Pull in some new namespaces
            ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇 
            [babashka.fs :as fs]
            [clojure.edn :as edn]
            ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆
            ))

(comment

  (def default-opts {:website-bucket "orgtech.se"
                     :out-dir "public"
                     :distribution-id "FDCBA42RSTUV3"})  ; C-c C-v f c e
  ;; => #'tasks/default-opts

  (def opts
    (let [base-dir (str (fs/cwd))]
      (merge default-opts
             (-> (fs/file base-dir "podcast.edn")
                 slurp
                 edn/read-string
                 (assoc :base-dir base-dir)))))
  ;; => #'tasks/opts

  opts
  ;; => {:website-bucket "orgtech.se/blog",
  ;;     :out-dir "public",
  ;;     :distribution-id "EPTUS11MTYJF7",
  ;;     :base-url "https://orgtech.se",
  ;;     :src-dir "public",
  ;;     :podcast { ... },
  ;;     :episodes [ ... ],
  ;;     :base-dir "/home/jmglov/code/orgtech-se"}

)
```

To set ourselves up for success with `soundcljoud.rss/podcast-feed`, we know
that we need our MP3 files in the right place. And in fact, we cheated a bit in
our REPL to copy those files to the right place, which means we have some code
lying around that we can use! And whilst we're at it, we should also copy the
transcript files, since we're referring to them in the rendered feed.

``` clojure
(comment

  (doseq [episode (:episodes opts)
          file (map episode [:audio-file :transcript-file])
          :let [filename (format "%s/%s%s/%s"
                                 (:base-dir opts) (:src-dir opts)
                                 (:path episode) file)
                src-filename (fs/file (:base-dir opts)
                                      (fs/file-name (:path episode))
                                      file)]]
    (when-not (fs/exists? filename)
      (fs/create-dirs (fs/parent filename))
      (fs/copy src-filename filename)))
  ;; => nil

  (->> (fs/glob (fs/file (:base-dir opts) (:src-dir opts)) "episodes/**")
       (map #(-> (str %)
                 (str/replace (:base-dir opts) ""))))
  ;; => ("/public/episodes/ep01-klarna-part1"
  ;;     "/public/episodes/ep01-klarna-part1/otis-ep01-klarna-part1.otr"
  ;;     "/public/episodes/ep01-klarna-part1/otis-ep01-klarna-part1.mp3"
  ;;     "/public/episodes/ep02-klarna-part2"
  ;;     "/public/episodes/ep02-klarna-part2/otis-ep02-klarna-part2.otr"
  ;;     "/public/episodes/ep02-klarna-part2/otis-ep02-klarna-part2.mp3"
  ;;     "/public/episodes/ep00-trailer"
  ;;     "/public/episodes/ep00-trailer/otis-ep00-trailer.otr"
  ;;     "/public/episodes/ep00-trailer/otis-ep00-trailer.mp3")

  )
```

Now that the files are, well, filed, let's see about rendering the podcast feed.

``` clojure
(ns tasks
  (:require ; ...
            [soundcljoud.rss :as rss]))

(comment

  (let [feed-file (fs/file (:src-dir opts) "feed.rss")]
    (println (format "Writing RSS feed %s" feed-file))
    (->> (rss/podcast-feed opts)
         (spit feed-file)))
  ;; => nil

  (slurp "public/feed.rss")
  ;; => "<?xml version='1.0' encoding='UTF-8'?>\n
  ;;     <rss version=\"2.0\"\n
  ;;          xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\n
  ;;          xmlns:atom=\"http://www.w3.org/2005/Atom\">\n
  ;;       <channel>\n
  ;;         <title>Organising Tech in Sweden</title>\n
  ;;         <description>Organising Tech in Sweden is a...</description>\n
  ;;         <itunes:image href=\"https://orgtech.se/img/orgtech-se-cover.jpg\"/>\n
  ;;         <language>en</language>\n
  ;;         <itunes:explicit>true</itunes:explicit>\n
  ;;         <itunes:category text=\"Technology\">\n
  ;;         </itunes:category>\n
  ;;         <itunes:category text=\"News\">\n
  ;;           <itunes:category text=\"Politics\" />\n
  ;;         </itunes:category>\n
  ;;         <itunes:author>Organising Tech in Sweden</itunes:author>\n
  ;;         <link>https://orgtech.se</link>\n
  ;;         <itunes:title>Organising Tech in Sweden</itunes:title>\n
  ;;         <itunes:type>Serial</itunes:type>\n
  ;;         <copyright>All rights reserved, Organising Tech in Sweden</copyright>\n
  ;;         <item>\n
  ;;           <title>Trailer</title>\n
  ;;           <enclosure\n
  ;;               url=\"https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.mp3\"\n
  ;;               length=\"1016937\"\n
  ;;               type=\"audio/mpeg\" />\n
  ;;           <guid>https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.mp3</guid>\n
  ;;           <pubDate>Thu, 5 Sep 2024 00:00:00 +0000</pubDate>\n
  ;;           <description><![CDATA[\n
  ;;             <p>\n
  ;;               Union organising seems to be in the air these days...
  ;;             </p>]]>
  ;;           </description>\n
  ;;           <itunes:duration>42</itunes:duration>\n
  ;;           <link>https://orgtech.se/episodes/ep00-trailer</link>\n
  ;;           <itunes:title>Trailer</itunes:title>\n
  ;;           <itunes:episode>0</itunes:episode>\n
  ;;           <itunes:episodeType>Trailer</itunes:episodeType>\n
  ;;           <transcriptUrl>https://orgtech.se/episodes/ep00-trailer/otis-ep00-trailer.otr</transcriptUrl>\n
  ;;         </item>\n
  ;;         ...
  ;;       </channel>
  ;;     </rss>

  )
```

OK, now we have everything we need to write our `render` function, so let's get
to it:

``` clojure
(defn render [default-opts]
  (let [base-dir (str (fs/cwd))
        {:keys [episodes src-dir] :as opts}
        (merge default-opts
               (cli/parse-opts *command-line-args*)
               (-> (fs/file base-dir "podcast.edn")
                   slurp
                   edn/read-string
                   (assoc :base-dir base-dir)))
        feed-file (fs/file src-dir "feed.rss")]
    (doseq [{:keys [path] :as episode} (:episodes opts)
            file (map episode [:audio-file :transcript-file])
            :let [filename (format "%s/%s%s/%s" base-dir src-dir path file)
                  src-filename (fs/file base-dir (fs/file-name path) file)]]
      (when-not (fs/exists? filename)
        (fs/create-dirs (fs/parent filename))
        (fs/copy src-filename filename)))
    (println (format "Writing RSS feed %s" feed-file))
    (->> (rss/podcast-feed opts)
         (spit feed-file))))

(comment

  (render default-opts)
  ;; => nil

  )
```

We should now be able to aim our web browser at http://localhost:1341/feed.rss
and see a lovely podcast feed.

![RSS feed displayed in a web browser][feed]
[feed]: assets/2024-09-18-podcast-soundcljoud-feed.png "Feed me nothing but the good stuff!" width=800px border=1

As lovely as this loveliness is, our eye is inexorably and tragically drawn to
one thing which we do not love:

``` xml
    <item>
      ...
      <link>https://orgtech.se/episodes/ep00-trailer</link>
      ...
    </item>
```

This page, dear reader, does not exist!

## Selmer to the rescue

Where does this `<link>` thingy come from, and why do we need it anyway? Well,
if we refer back to [A Podcaster's Guide to
RSS](https://help.apple.com/itc/podcasts_connect/#/itcb54353390), we see:

> `<link>`
> 
> An episode link URL.
> This is used when an episode has a corresponding webpage.

Ah, so it's an episode page we need, eh? Well, we have a bunch of info about the
episode in our `podcast.edn` file, and some code that loops over episodes and
does stuff in `tasks/render`, and a deep and abiding love for Selmer, so let's
whip up an episode page template, then plug some stuff in whilst we're looping
over episodes. We'll start with the template, which we'll drop into a new
`templates/episode-page.html` file:

``` html
<!doctype html>
<html class="no-js" lang="">

<head>
  <title>
    {{podcast.title}} Episode {{episode.number}} - {{episode.title}}
  </title>

  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">

  <link rel="stylesheet" href="/css/main.css">

  <!-- Favicon from https://realfavicongenerator.net/ -->
  <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
  <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
  <link rel="manifest" href="/site.webmanifest">
  <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#5bbad5">
  <meta name="msapplication-TileColor" content="#da532c">
  <meta name="theme-color" content="#ffffff">

  <!-- Social sharing (Facebook, Twitter, LinkedIn, etc.) -->
  <meta name="title" content="{{podcast.title}} Episode {{episode.number}} - {{episode.title}}">
  <meta name="twitter:title" content="{{podcast.title}} Episode {{episode.number}} - {{episode.title}}">
  <meta property="og:title" content="{{podcast.title}} Episode {{episode.number}} - {{episode.title}}">
  <meta property="og:type" content="website">

  <meta name="description" content="{{episode.summary}}">
  <meta name="twitter:description" content="{{episode.summary}}">
  <meta property="og:description" content="{{episode.summary}}">

  <meta name="twitter:url" content="{{base-url}}{{episode.path}}/index.html">
  <meta property="og:url" content="{{base-url}}{{episode.path}}/index.html">

  <meta name="twitter:image" content="{{base-url}}{{preview-image}}">
  <meta name="twitter:card" content="summary_large_image">
  <meta property="og:image" content="{{base-url}}{{preview-image}}">
  <meta property="og:image:alt" content="{{podcast.image-alt}}">
</head>

<body>
  <div id="wrapper">
    <div id="left-side">
      <img id="cover-image" src="{{podcast.image}}" alt="{{podcast.image-alt}}" />
      <div id="aggregators-1">
        <div id="apple">
          <a class="apple-button"
            href="https://podcasts.apple.com/us/podcast/organising-tech-in-sweden/id1766442275?itsct=podcast_box_badge&amp;itscg=30200&amp;ls=1">
            <img src="https://tools.applemediaservices.com/api/badges/listen-on-apple-podcasts/badge/en-us?size=250x83&amp;releaseDate=1725494400"
              title="Listen on Apple Podcasts" alt="Listen on Apple Podcasts" class="apple-button">
          </a>
        </div>
        <div id="spotify">
          <a href="https://open.spotify.com/show/53psoLoX187axvmgb80l1x">
            <img src="/img/spotify-podcast-badge-blk-grn-330x80.svg" title="Listen on Spotify"
              alt="Listen on Spotify">
          </a>
        </div>
      </div>
      <div id="aggregators-2">
        <div id="podbean">
          <a href="https://www.podbean.com/podcast-detail/2r2tz-31b053/Organising-Tech-in-Sweden-Podcast"
            rel="noopener noreferrer" target="_blank">
            <img src="https://pbcdn1.podbean.com/fs1/site/images/badges/w600_1.png"
              title="Listen on Podbean" alt="Listen on Podbean">
          </a>
        </div>
      </div>
    </div>
    <div id="main">
      <nav id="header">
        <h1 id="title">{{episode.title}}</h1>
        <div id="socials">
          {% for social in socials %}
          <a href="{{social.url}}">
            <img src="{{social.image}}" alt="{{social.image-alt}}" />
          </a>
          {% endfor %}
        </div>
      </nav>
      <div id="description">{{episode.description|safe}}</div>
    </div>
  </div>
  <div id="transcript">
    <h1>Transcript</h1>
    <div id="transcript-body">{{episode.transcript-html|safe}}</div>
  </div>
</body>

</html>
```

We should also sprinkle a little extra CSS into our `public/css/main.css`:

``` css
/* ... */

#aggregators-1 {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

#aggregators-1 img {
  width: 175px;
}

#apple a {
  display: inline-block;
  overflow: hidden;
}

.apple-button {
  border-radius: 13px;
}

#aggregators-2 {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

#podbean img {
  height: 42px;
}

/* Some paragraphs in the description shouldn't be displayed on the episode page */
p.soundcljoud-hidden {
  display: none;
}

#transcript {
  background-color: #e4f1fe;
  border: solid 1px;
  padding-left: 1em;
  padding-right: 1em;
  margin-top: 1em;
}

#transcript-body > br {
  display: none;
}

span.timestamp {
  margin-right: 5px;
  color: blue;
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
}

@media screen and (min-width: 600px) {

  /* ... */

  #aggregators-1 img {
    width: 155px;
  }

  #podbean img {
    height: 37px;
  }

  #podbean img {
    margin-top: 0px;
  }

}
```

We'll need the following template vars:
- base-url
- episode.description
- episode.number
- episode.path
- episode.summary
- episode.title
- episode.transcript-html
- podcast.image
- podcast.image-alt
- podcast.title
- preview-image
- socials
  - social.url
  - social.image
  - social.image-alt

Most of this we already have, but there are a couple new things. Let's take the
easiest two first.
- podcast.image-alt
- preview-image

We'll add some alt text for our podcast cover image and a social preview image
to `podcast.edn`. The alt text is just a description of the cover image, and it
turns out that the preview image is one of the many things we hardcoded into our
`public/index.html` way back when.

``` clojure
{ ; ...
 :preview-image "/img/orgtech-se-preview.jpg"
 ;; ...
 :podcast { ; ...
           :image-alt "Organising Tech in Sweden superimposed on raised fists with a Swedish flag with a circuit board pattern in the background"
           ;; ...
           }
 ;; ...
 }
```

Let's turn next to `socials`. This is how we refer to it in the template:

``` html
        {% for social in socials %}
        <a href="{{social.url}}">
          <img src="{{social.image}}" alt="{{social.image-alt}}" />
        </a>
        {% endfor %}
```

This means that it needs to be a list, and each list item should be a map
containing three keys:
- social.url
- social.image
- social.image-alt

Let's add the following to our `podcast.edn`:

``` clojure
{ ; ...
 :preview-image "/img/orgtech-se-preview.jpg"
 ;; ...
 :socials [{:name "Twitter"
            :url "https://x.com/orgtech_se"
            :image "/img/twitter-color-svgrepo-com.svg"
            :image-alt "Twitter logo"}
           {:name "BlueSky"
            :url "https://bsky.app/profile/orgtech-se.bsky.social"
            :image "/img/bluesky-logo.svg"
            :image-alt "BlueSky logo"}]
 ;; ...
 }
```

Finally, we need to conjure up one last key for each episode:
- episode.transcript-html

Episodes already have a `:transcript-file` key, which refers to an OTR file.
Let's have a quick look at one of those and see what is contained therein:

``` json
{
  "text": "<p>[Theme music begins]</p><p><span class=\"timestamp\" data-timestamp=\"12.111684\">00:12</span><b>Josh</b>: ... </p>",
  "media": "otis-ep01-klarna-part1.mp3",
  "media-time": 1315.629803
}
```

What we have here is a JSON file with a thin veneer of metadata around a
looooong HTML string. Let's deal with this back in `podcast.rss/update-episode`:

``` clojure
(defn update-episode [{:keys [base-dir src-dir] :as opts}
                      ;;                 👇👇👇👇👇👇👇
                      {:keys [audio-file transcript-file path] :as episode}]
                      ;;                 👆👆👆👆👆👆👆
  (let [filename (format "%s/%s%s/%s" base-dir src-dir path audio-file)
        ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
        transcript (format "%s/%s%s/%s" base-dir src-dir path transcript-file)]
        ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆
    (assoc episode
           :audio-filesize (fs/size filename)
           :duration (audio/mp3-duration filename)
           :description (selmer/render (:description episode)
                                       (assoc opts :episode episode))
           ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
           :transcript-html (-> (slurp transcript)
                                (json/parse-string keyword)
                                :text))))
```

Having satisfied ourselves that we have all of the data we need for our
template, let's get to rendering. In our `tasks/render` function, we're looping
over episodes in order to copy the MP3 and OTR files into the right place.
Sadly, we can't just pop `selmer/render` into that `doseq` and be done with it,
because we need to ensure the audio file is in the right place before we can use
`update-episode`. No problem, we'll just add a new bit after spitting our
podcast feed:

``` clojure
(ns tasks
  (:require ; ...
            [selmer.parser :as selmer]))

(defn render [default-opts]
  (let [...]
    ;; ...
    (println (format "Writing RSS feed %s" feed-file))
    (->> (rss/podcast-feed opts)
         (spit feed-file))
    (let [template (slurp "templates/episode-page.html")
          opts (rss/update-episodes opts)]
      (doseq [{:keys [path] :as episode} (:episodes opts)
              :let [filename (format "%s/%s%s/%s"
                                     base-dir out-dir path "index.html")]]
        (println "Writing episode page" filename)
        (->> (selmer/render template (assoc opts :episode episode))
             (spit filename))))))
```

And now, the moment of truth!

``` text
: orgtech-se; bb render
Writing RSS feed public/feed.rss
Writing episode page ~/code/orgtech-se/public/episodes/ep00-trailer/index.html
Writing episode page ~/code/orgtech-se/public/episodes/ep01-klarna-part1/index.html
Writing episode page ~/code/orgtech-se/public/episodes/ep02-klarna-part2/index.html
```

And now if we visit (for example) http://localhost:1341/episodes/ep01-klarna-part1,
we should see an amazing webpage:

![Episode page displayed in a web browser][episode]
[episode]: assets/2024-09-18-podcast-soundcljoud-episode.png "I feel good, but also empty" width=800px border=1

## Podcast half empty, or podcast half full?

Astute observers may have noticed one issue with the episode page: there's no
way to play the episode. 🤦🏼

Fear not! In the next instalment, we'll look at playing a podcast with
ClojureScript, perhaps even using our own friend
[Soundcljoud](tags/soundcljoud.html)!
