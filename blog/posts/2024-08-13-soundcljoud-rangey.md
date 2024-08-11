Title: Soundcljoud gets more rangey
Date: 2024-08-13
Tags: clojure,babashka,scittle,clonejure,clojurescript,soundcljoud
Description: In which I teach a webserver about range requests
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1723562900567459
Image: assets/2024-08-13-soundcljoud-rangey-preview.png
Image-Alt: A golfer with the Soundcljoud logo as their head about to hit a shot; Photo by Andrew Rice on Unsplash

![A golfer with the Soundcljoud logo as their head about to hit a shot; Photo by Andrew Rice on Unsplash][preview]
[preview]: assets/2024-08-13-soundcljoud-rangey-preview.png "FORE!" width=800px

[Last time](2024-07-20-soundcljoud-cloudy.html) on "Soundcljoud gets more
cloudy", I found myself deeply saddened that the eternal truths I was seeking in
the music of Garth Brooks remained elusive due to my attempts to seek forward in
a track were rebuffed by my browser, instead abruptly returning me to the
beginning of the track. 😳

Appropriately chastened, I popped the bonnet and had a look at what my user
agent was doing on my behalf. When I loaded a track, I saw a request like this:

``` text
GET /Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3 HTTP/1.1
Range: bytes=0-
```

and a response like this:

``` text
HTTP/1.1 200 OK
Content-length: 5943424
Content-Type: audio/mpeg
Server: http-kit
```

with a bunch of bytes in the body. In fact, a bountiful buffet of beautiful
bytes, five whole million of them! And another 943,424 thrown in for dessert.

Herein lies the rub. What the browser wants back is some indication that the
server knows how to return a range of bytes, because the browser doesn't want to
fetch the entire damned file every time the user starts playing a track. After all,
the user might be trying to remember if the track entitled "The Old Stuff"
contains the amazing homage to a "worn out tape of Chris LeDoux" (spoiler: it
does not), and just listening to the first few seconds to determine this, then,
disappointed, moving on to another track to sample the first few seconds of that
one.

And how, you might ask, does the server indicate its range savviness? Well,
according to our good friends over at the [Mozilla Developer
Network](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests), by
returning a response such as this:

``` text
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-length: 1048576
Content-Range: bytes 0-1048575/5943424
Content-Type: audio/mpeg
```

## Whence ranges?

Let's refresh our memory a bit by firing up
[Soundcljoud](https://github.com/jmglov/soundcljoud):

``` text
cd ~/code/soundcljoud/player
bb dev
```

Now we can pop over to [http://localhost:1341/](http://localhost:1341/), open up
the `soundcljoud.cljs` in Emacs (or whatever inferior text editor you choose to
inflict upon yourself), hit **C-c l C** (`cider-connect-cljs`) to start a REPL
connected to localhost port 1339 (REPL type `nbb`), and finally evaluate
`load-ui!` to get things going:

``` clojure
(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  )
```

![The Soundcljoud UI, playing album Garth Brooks - Fresh Horses][eye]
[eye]: assets/2024-08-13-soundcljoud-rangey-eye.png "The all-seeing eye of Garth is upon us" width=800px border=1

Opening the network tab, we see exactly what the browser asked for and exactly
what the server responded:

![Browser developer tools, showing the network request and response for the MP3 file][range]
[range]: assets/2024-08-13-soundcljoud-rangey-range.png "Yeah, I didn't want *all* of the bytes..." width=800px border=1

First, the browser asks for some bytes, starting at the beginning of the file:

``` text
Range: bytes=0-
```

Since the end of the byte range isn't specified, the server is free to decide
how many bytes to send back. Let's say we'll send back 1 MB (1048576 bytes).
Our response should start by indicating that we're not returning the entire
file, but rather just a part of it:

``` text
HTTP/1.1 206 Partial Content
```

Now we need to say which bytes we're returning, out of the total number of bytes
in the file, as well as the length of the response, in bytes:

``` text
Content-Range: bytes 0-1048575/6062208
Content-length: 1048576
```

Note that the byte range is zero-indexed and **inclusive** on the end, meaning
that the last byte we return is at index 1048575, whilst the content length is
the **number of bytes** in the response body.

Finally, we need to let the client know what kind of range requests we support.
We'll limit this to bytes:

``` text
Accept-Ranges: bytes
```

We must now flip Hegel on his head, as the saying goes, and move from lofty
ideas to dirty, inconvenient material reality. In other words, we gotta
implement range requests in our actual webserver.

## Getting materialistic

Let's cast our minds back to what happens when we type

``` text
bb dev
```

in our terminal. According to our `bb.edn`:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:git/sha "b38c1f16ad2c618adae2c3b102a5520c261a7dd3"}}
 :tasks
 {http-server
  {:doc "Starts http server for serving static files"
   :requires ([babashka.http-server :as http])
   :task (do (http/serve {:port 1341 :dir "public"})
             (println "Serving static assets at http://localhost:1341"))}

  browser-nrepl
  {:doc "Start browser nREPL"
   :requires ([sci.nrepl.browser-server :as bp])
   :task (bp/start! {})}

  -dev
  {:depends [http-server browser-nrepl]}

  dev
  {:task (do (run '-dev {:parallel true})
           (deref (promise)))}}}
```

OK, so it looks like
[io.github.babashka/http-server](https://github.com/babashka/http-server) is the
thing serving up our content. Let's go ahead and clone that so we can start
digging through the code:

``` text
cd ~/code
git clone git@github.com:babashka/http-server.git
```

Tracing through `bb.edn`, we see that the webserver is started by calling
`babashka.http-server/serve` with a config map containing the port and
directory:

``` clojure
{ ;; ...
 :tasks
 {http-server
  {:requires ([babashka.http-server :as http])
   :task (do (http/serve {:port 1341 :dir "public"})
             (println "Serving static assets at http://localhost:1341"))}
 ;; ...
 }}
```

Let's see what's going on thereabouts in the http-server source code. Opening [src/babashka/http_server.clj](https://github.com/babashka/http-server/blob/e625b1a367023bc400d38474677d071abd8c02fb/src/babashka/http_server.clj):

``` clojure
(defn serve
  "Serves static assets using web server.
Options:
  * `:dir` - directory from which to serve assets
  * `:port` - port
  * `:headers` - map of headers {key value}"
  [{:keys [port]
    :or {port 8090}
    :as opts}]
  (let [dir (or (:dir opts) ".")
        opts (assoc opts :dir dir :port port)
        dir (fs/path dir)]
    (assert (fs/directory? dir) (str "The given dir `" dir "` is not a directory."))
    (binding [*out* *err*]
      (println (str "Serving assets at http://localhost:" (:port opts))))
    (server/run-server (file-router dir (opts :headers)) opts)))
```

we see a bunch of ceremony before `server/run-server` is called with a
`file-router` (whatever that is) and some opts; basically the port and directory
we passed in from `bb.edn`. But what, pray tell, is this mystical `server`
namespace?

``` clojure
(ns babashka.http-server
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            #_[clojure.tools.cli :refer [parse-opts]]
            [hiccup2.core :as html]
            [babashka.cli :as cli]
            [org.httpkit.server :as server])
  (:import [java.net URLDecoder URLEncoder]))
```

Aha! 'Tis none other than [http-kit](https://github.com/http-kit/http-kit), a
"minimalist and efficient Ring-compatible HTTP client+server for Clojure".
Looking at the [documentation for
`run-server`](https://github.com/http-kit/http-kit/wiki/3-Server#start-server),
we see that the `file-router` thingy must return a [Ring
handler](https://github.com/ring-clojure/ring/wiki/Concepts#handlers), which is
nothing more than a function that takes a request map as its argument and
returns a response map. This function will be called by http-kit upon every
request.

`start-server` returns a function that we can call to [stop the
server](https://github.com/http-kit/http-kit/wiki/3-Server#stop-server).

Using this knowledge, let's dig into the `file-router` handler function:

``` clojure
(defn file-router [dir headers]
  (fn [{:keys [uri]}]
    (let [f (fs/path dir (str/replace-first (URLDecoder/decode uri) #"^/" ""))
          index-file (fs/path f "index.html")]
      (update (cond
                (and (fs/directory? f) (fs/readable? index-file))
                (body index-file)

                (fs/directory? f)
                (index dir f)

                (fs/readable? f)
                (body f)

                (and (nil? (fs/extension f)) (fs/readable? (with-ext f ".html")))
                (body (with-ext f ".html") headers)

                :else
                {:status 404 :body (str "Not found `" f "` in " dir)})
              :headers (fn [response-headers]
                         (merge headers response-headers))))))
```

OK, what's going on here? Well, we're returning a function (i.e. the Ring
handler) that basically grabs the path part of the URI (which will be relative
to the directory named by our `:dir` option; in other words,
`soundcljoud/player/public`) and asks a series of questions in a
[cond](https://clojuredocs.org/clojure.core/cond) form:

1. Does the path refer to a directory? If so, does there exist an `index.html`
   that is readable by the webserver?
2. Otherwise, does the path refer to a directory (without an `index.html`)?
3. Otherwise, does the path refer to a file that is readable by the webserver?
4. Otherwise, does the path refer to a thing which, if we slap a `.html`
   extension on the end, is a file that is readable by the webserver?
5. Why is this user wasting our time requesting stuff that we don't have?

Let's think for a second about which case we're interested in. Our browser is
requesting `/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.mp3`, which
is going to hit condition #3 in the list:

``` clojure
                (fs/readable? f)
                (body f)
```

Let's see what's going on with this body. And yes, I am aware that sounds like
the title of a [Pitbull](https://en.wikipedia.org/wiki/Pitbull_(rapper)) collabo
with [Nicki Minaj](https://en.wikipedia.org/wiki/Nicki_Minaj).

![Fake cover art for a What's Going on with that Body? single][body]
[body]: assets/2024-08-13-soundcljoud-rangey-body.png "¿Qué pasa con ese cuerpo?"

``` clojure
(defn- body
  ([path]
   (body path {}))
  ([path headers]
   {:headers (merge {"Content-Type" (ext-mime-type (fs/file-name path))} headers)
    :body (fs/file path)}))
```

The only thing happening here is that the [MIME
type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
of the file is being looked up using its extension and added as the
`Content-Type` header, then the path itself is turned into a `java.io.File` with
the
[babashka.fs/file](https://github.com/babashka/fs/blob/master/API.md#babashka.fs/file)
function and added to the response map under the `:body` key. Presumably,
http-kit will then take that `java.io.File` object and send the bytes back as
the response body.

This looks very similar to what we will need to do, with the exception that
instead of sending back all of the bytes in the file, we'll just want to send
back those that were asked for.

Now that we know more or less where to start, let's fire up a REPL and start
playing!

## We aim to serve

The first thing we need to do is Ctrl-c our `bb dev` process, since we won't be
able to start a webserver on port 1341 with that one in the way.

Next, let's open up `http-server/src/babashka/http_server.clj` in Emacs and
start a REPL with **C-c M-j** (`cider-jack-in-clj`), choosing `babashka` as the
command to start the REPL. Now, we load the buffer with **C-c C-k**
(`cider-load-buffer`), and sign in relief as we're back in the REPL again.

For our first order of business, let's try starting a server from the REPL to
serve up the files in the `soundcljoud/player/public` directory on port 1341,
just like we had before:

``` clojure
(comment

  (def dir "../soundcljoud/player/public")  ; C-c C-v f c e
  ;; => #'babashka.http-server/dir
  
  (def server
    (server/run-server (file-router dir {})
                       {:dir dir, :port 1341}))
  ;; => #'babashka.http-server/server

  )
```

OK, so we maybe have a webserver running. Let's try fetching a file to be sure:

``` text
: jmglov@alhana; curl http://localhost:1341/site.webmanifest
{
    "name": "Soundcljoud",
    "short_name": "Soundcljoud",
    "icons": [
        {
            "src": "icons/android-chrome-192x192.png",
            "sizes": "192x192",
            "type": "image/png"
        },
        {
            "src": "icons/android-chrome-512x512.png",
            "sizes": "512x512",
            "type": "image/png"
        }
    ],
    "theme_color": "#ffffff",
    "background_color": "#ffffff",
    "display": "standalone"
}
```

Looks good!

## Hacking the cloud

The next step is making Soundcljoud use our local HTTP server instead of
starting a new one. Back in `soundcljoud/player`, we open up `bb.edn`. Let's go
ahead and change the deps first:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:git/sha "b38c1f16ad2c618adae2c3b102a5520c261a7dd3"}}
 ;; ...
 }
```

For the `io.github.babashka/http-server` dep, we can change the value from a Git
reference to a local directory like this:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:local/root "../../http-server"}}
 ;; ...
 }
```

Next, we'll need to figure out how to start just the browser REPL. Let's take a
look at the existing `dev` task that we've been using:

``` clojure
{ ;; ...
 :tasks {http-server
         {:doc "Starts http server for serving static files"
          :requires ([babashka.http-server :as http])
          :task (do
                  (http/serve {:port 1341 :dir "public"})
                  (println "Serving static assets at http://localhost:1341"))}

         browser-nrepl
         {:doc "Start browser nREPL"
          :requires ([sci.nrepl.browser-server :as bp])
          :task (bp/start! {})}

         -dev
         {:depends [http-server browser-nrepl]}

         dev
         {:task (do (run '-dev {:parallel true})
                  (deref (promise)))}}}
```

So `dev` just runs the `-dev` task in parallel, then derefs an empty promise to
avoid exiting (calling `deref` on a promise will block the calling thread until
the promise delivers, which an empty promise never will). The `-dev` task itself
depends on `http-server` and `browser-nrepl`, but does nothing on its own.

Let's create a new task that follows this pattern but only starts the browser
NREPL:

``` clojure
{ ;; ...
 :tasks { ;; ...

         dev
         {:task (do (run '-dev {:parallel true})
                  (deref (promise)))}

         browser
         {:task (do (run 'browser-nrepl {:parallel true})
                    (deref (promise)))}}}
```

Now let's fire it up and see what happens:

``` text
: jmglov@alhana; bb browser
nREPL server started on port 1339...
Websocket server started on 1340...
```

Cool! If we now open http://localhost:1341/ in the browser, switch back to
our open `soundcljoud.cljs` buffer and hit **C-c l C**, we see some very welcome
log messages in our terminal:

``` text
nREPL server started on port 1339...
:msg "{:versions {\"scittle-nrepl\" {\"major\" \"0\", ..."
```

With baited breath, we evaluate the `load-ui!` form and... see the good ol' Eye
of Garth! 🎉

This means Soundcljoud is using the http-server we're running from our REPL.

## Homing in on the range

Switching back to the `http-server/src/babashka/http_server.clj` buffer, let's
figure out how to do some REPL-driven development to implement handling range
requests.

The first order of business might be giving ourselves a way to log the requests
we're getting from the client. Let's create an atom at the top of the file for
this very purpose:

``` clojure
(defonce state (atom {:requests [], :log []}))
```

I'm using [defonce](https://clojuredocs.org/clojure.core/defonce) instead of
plain 'ol `def` here because I tend to hit **C-c C-k** quite often whilst
editing code, which not only causes the buffer to be re-evaluated, but also
causes Emacs to ask me if I want to save my changes to the file, which is useful
to keep code that's running in the system from drifting away from the code
that's written in the source file. If I used `def` instead of `defonce`, my
state atom would be reset every time I re-evaluate the buffer.

Now, we know that the function returned by `file-router` is a Ring handler, so
let's jump there and see about how we can shove each request into our `state`
atom:

``` clojure
(defn file-router [dir headers]
  (fn [{:keys [uri]}]
    ;; ...
    ))
```

OK, at the moment, the handler function only cares about the `:uri` key in the
request. Let's bind the entire request and then add it to the atom:

``` clojure
(defn file-router [dir headers]
  (fn [{:keys [uri] :as req}]
    (swap! state update :requests conj req)
    ;; ...
    ))
```

In order to test this, we need to restart the server since we made a change to
the anonymous function returned by `file-router`. To do this, we stop the server
by calling the function that `server/run-server` returned when we evaluated it,
then evaluate the `server/run-server` expression again:

``` clojure
(comment

  (server)
  ;; => nil

  (def server
    (server/run-server (file-router dir {})
                       {:dir dir, :port 1341}))
  ;; => #'babashka.http-server/server

  )
```

Now, let's curl the manifest file again:

``` text
: jmglov@alhana; curl http://localhost:1341/site.webmanifest
{
    "name": "Soundcljoud",
    ...
}
```

If we look at our state atom now, we can see that the request was successfully
logged:

``` clojure
(comment

  (:requests @state)
  ;; => [{:remote-addr "0:0:0:0:0:0:0:1",
  ;;      :start-time 1004192760289113,
  ;;      :headers
  ;;      {"accept" "*/*", "host" "localhost:1341", "user-agent" "curl/8.4.0"},
  ;;      :async-channel
  ;;      #object[org.httpkit.server.AsyncChannel 0x44d028e7 "/[0:0:0:0:0:0:0:1]:1341<->/[0:0:0:0:0:0:0:1]:45890"],
  ;;      :server-port 1341,
  ;;      :content-length 0,
  ;;      :websocket? false,
  ;;      :content-type nil,
  ;;      :character-encoding "utf8",
  ;;      :uri "/site.webmanifest",
  ;;      :server-name "localhost",
  ;;      :query-string nil,
  ;;      :body nil,
  ;;      :scheme :http,
  ;;      :request-method :get}]

  )
```

OK, now that we've got some basic logging in place, let's get back to thinking
about range requests. A good place to start is by looking at the requests we get
from Soundcljoud when it loads a file, so let's pop back over to that browser
window and click on a track.

Once we've done that, we can look at the request in our http-server REPL:

``` clojure
(comment

  (->> @state
       :requests
       (map #(select-keys % [:start-time :headers :uri]))
       last)
  ;; => {:start-time 1006716878994472,
  ;;     :headers
  ;;     {"range" "bytes=0-",
  ;;      "sec-fetch-site" "same-origin",
  ;;      "sec-ch-ua-mobile" "?0",
  ;;      "host" "localhost:1341",
  ;;      "user-agent"
  ;;      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
  ;;      "sec-ch-ua"
  ;;      "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Brave\";v=\"120\"",
  ;;      "sec-ch-ua-platform" "\"Linux\"",
  ;;      "referer" "http://localhost:1341/",
  ;;      "connection" "keep-alive",
  ;;      "accept" "*/*",
  ;;      "accept-language" "en-GB,en",
  ;;      "sec-fetch-dest" "audio",
  ;;      "accept-encoding" "identity;q=1, *;q=0",
  ;;      "sec-fetch-mode" "no-cors",
  ;;      "sec-gpc" "1"},
  ;;     :uri
  ;;     "/Garth%20Brooks/Fresh%20Horses/01%20-%20Garth%20Brooks%20-%20The%20Old%20Stuff.mp3"}

  )
```

The interesting bit is this header right here, which is the thing that tells us
that what we're dealing with here is a range request:

``` text
  ;;     :headers
  ;;     {"range" "bytes=0-",
```

Remember those 5 questions we asked back in `file-router`?

1. Does the path refer to a directory? If so, does there exist an `index.html`
   that is readable by the webserver?
2. Otherwise, does the path refer to a directory (without an `index.html`)?
3. Otherwise, does the path refer to a file that is readable by the webserver?
4. Otherwise, does the path refer to a thing which, if we slap a `.html`
   extension on the end, is a file that is readable by the webserver?
5. Why is this user wasting our time requesting stuff that we don't have?

Well, let's insert a new question in there as #3, and bump the rest down:

1. Does the path refer to a directory? If so, does there exist an `index.html`
   that is readable by the webserver?
2. Otherwise, does the path refer to a directory (without an `index.html`)?
3. **Otherwise, does the path refer to a file that is readable by the webserver and we have a `range` header in our request?**
4. Otherwise, does the path refer to a file that is readable by the webserver?
5. Otherwise, does the path refer to a thing which, if we slap a `.html`
   extension on the end, is a file that is readable by the webserver?
6. Why is this user wasting our time requesting stuff that we don't have?

Let's write that in Clojure instead of English:

``` clojure
(defn file-router [dir headers]
  (fn [{:keys [uri] :as req}]
    (swap! state update :requests conj req)
    (let [f (fs/path dir (str/replace-first (URLDecoder/decode uri) #"^/" ""))
          index-file (fs/path f "index.html")]
      (update (cond
                (and (fs/directory? f) (fs/readable? index-file))
                (body index-file)

                (fs/directory? f)
                (index dir f)

                ;; 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇
                (and (fs/readable? f) (contains? (:headers req) "range"))
                (do
                  (swap! state update :log conj "Handling range request")
                  (body f))
                ;; 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆

                (fs/readable? f)
                (body f)

                (and (nil? (fs/extension f)) (fs/readable? (with-ext f ".html")))
                (body (with-ext f ".html") headers)

                :else
                {:status 404 :body (str "Not found `" f "` in " dir)})
              :headers (fn [response-headers]
                         (merge headers response-headers))))))
```

Now we can try this out. Unfortunately, we need to restart the server again to
have it pick up the new code. The issue is that `file-handler` is returning an
anonymous function, so when we edit the code and re-evaluate the buffer, we're
not updating the copy of the function that http-kit is using as the request
handler, we're updating `file-handler` itself, so the next time it's called, it
will return a new handler function. In the writing of this blog, I did try
pulling the anonymous function out and giving it a name, which I expected to fix
this issue, but that didn't work, for reasons that aren't clear to me (maybe
because http-kit is running the server on a different thread?). Yell at me in
the Clojurians Slack thread if you know how to do this. 😅

Anyway, let's stop the server as usual:

``` clojure
(comment

  (server)
  ;; => nil

  )
```

And now, since we know we're going to need to do this dance every time we make
changes to the code, let's write a little convenience function:

``` clojure
(comment

  (defn restart-server []
    (when (:server @state)
      ((:server @state)))
    (reset! state
            {:requests []
             :log []
             :server
             (server/run-server (file-router dir {})
                                {:dir dir, :port 1341})}))
  ;; => #'babashka.http-server/restart-server

  )
```

Now we can just call `restart-server` whenever we need to, well, restart the
server. Let's do so now:

``` clojure
(comment

  (restart-server)
  ;; => {:requests [],
  ;;     :server
  ;;     #object[clojure.lang.AFunction$1 0x3ece031a "clojure.lang.AFunction$1@3ece031a"]}

  )
```

Having done this, let's pop back over to Soundcljoud and click on another track,
then inspect the log to make sure we see the message we expect:

``` clojure
(comment

  (->> @state
       :log
       last)
  ;; => "Handling range request"

  )
```

Looks good! Except for the fact that we're still returning the entire file in
the request body, of course. Still, the key to REPL-driven development is
rapidly iterating, so let's take that next iteration now!

## What's in a range?

Since we've captured the request, let's go ahead and pull the range header out
so we can play with it:

``` clojure
(comment

  (-> (:requests @state) last (get-in [:headers "range"]))
  ;; => "bytes=0-"

  (def range-header *1)
  ;; => #'babashka.http-server/range-header

  (let [[start end] (-> range-header
                        (str/replace #"^bytes=" "")
                        (str/split #"-"))]
    [start end])
  ;; => ["0" nil]

  )
```

The header parsing thing looks like a good thing to make into a function:

``` clojure
(defn- parse-range-header [range-header]
  (map #(when % (Long/parseLong %))
       (-> range-header
           (str/replace #"^bytes=" "")
           (str/split #"-"))))

(comment

  (parse-range-header range-header)
  ;; => (0)

)
```

OK, now let's shift gears and figure out how to return a specific byte range
from a file. After much searching, I found a magical way to seek to an arbitrary
location in a file in Java (and hence Clojure, through the magic of interop).
Every
[FileInputStream](https://docs.oracle.com/javase/8/docs/api/java/io/FileInputStream.html)
has an associated
[FileChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/FileChannel.html),
and this FileChannel has a helpful
[position()](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/FileChannel.html#position-long-)
instance method, which sets the position in the FileChannel for subsequent read
operations on the channel.

Now, how to perform a read operation on a FileInputStream? Looking at the
documentation, this method looks quite useful:

[**read**](https://docs.oracle.com/javase/8/docs/api/java/io/FileInputStream.html#read-byte:A-)

``` java
public int read(byte[] b)
         throws IOException
```

> Reads up to b.length bytes of data from this input stream into an array of
> bytes. This method blocks until some input is available.

And how do we create a `byte[]` array of an arbitrary size in Clojure? Why, by
using the aptly-named
[byte-array](https://clojuredocs.org/clojure.core/byte-array) function,
naturally! 😀

Let's try this out, using our helpful `site.webmanifest` file:

``` clojure
(comment

  (let [arr (byte-array 32)]
    (with-open [is (java.io.FileInputStream. manifest-file)]
      (-> is .getChannel (.position 0))
      (.read is arr))
    (String. arr))
  ;; => "{\n    \"name\": \"Soundcljoud\",\n   "

  (let [arr (byte-array 16)]
    (with-open [is (java.io.FileInputStream. manifest-file)]
      (-> is .getChannel (.position 14))
      (.read is arr))
    (String. arr))
  ;; => "\"Soundcljoud\",\n "

  )
```

Now we're cooking with gas! 💥

Let's see if we can make a nice function out of this:

``` clojure
(defn- read-bytes [f [start end]]
  (let [arr (byte-array (- end start))]
    (with-open [is (java.io.FileInputStream. f)]
      (-> is .getChannel (.position start))
      (.read is arr))
    arr))

(comment

  (-> (read-bytes manifest-file [0 31])
      (String.))
  ;; => "{\n    \"name\": \"Soundcljoud\",\n   "

  (-> (read-bytes manifest-file [14 29])
      (String.))
  ;; => "\"Soundcljoud\",\n "

  )
```

There's one issue remaining, though. Remember the range header we got from
Soundcljoud?

``` clojure
(comment

  range-header
  ;; => "bytes=0-"

  (parse-range-header range-header)
  ;; => (0)

)
```

We have a `start`, but not an `end`. 😱

Let's think about what we want to do in this case. The client is effectively
saying, "give me as many bytes as you feel inclined to do, starting at this
offset in the file". So how many bytes are we inclined to hand out willy-nilly?
I dunno, how about 1 mega of them bytes?

``` clojure
(defn- read-bytes [f [start end]]
  (let [end (or end (dec (+ start (* 1024 1024)))
        arr (byte-array (- end start))]
    (with-open [is (java.io.FileInputStream. f)]
      (-> is .getChannel (.position start))
      (.read is arr))
    arr))

(comment

  (-> (read-bytes manifest-file [0 31])
      (String.))  ; ⚠ OMG wait don't evaluate this for the love of Pete!

)
```

Yeah, so you really don't want to evaluate that last `read-bytes` expression.
"And why's that," you might ask? "Well," I might answer, "cast your mind back to
the Java documentation":

[**read**](https://docs.oracle.com/javase/8/docs/api/java/io/FileInputStream.html#read-byte:A-)

``` java
public int read(byte[] b)
         throws IOException
```

> Reads up to b.length bytes of data from this input stream into an array of
> bytes. 👉 **This method blocks until some input is available.** 👈

"And how do you know this is a problem?" you might query. "Well," I might
respond, "um, just 'cuz? I mean... I certainly didn't evaluate this and hang my
REPL process and then have to forcibly kill Emacs or anything, because that
would be a rookie mistake. Haha." And then I might laugh nervously and quickly
change the subject. "So, how 'bout them Yankees?" I might mutter, maybe even
looking at my shoes.

So blerg, what to do, what to do?

Well, we do know (or at least **can** know) how many bytes are in the file, so
maybe we don't read past the end of the file? Amazing insights you get in this
here blog, innit?

``` clojure
(defn- read-bytes [f [start end]]
  (let [end (or end (dec (min (fs/size f)
                              (+ start (* 1024 1024)))))
        arr (byte-array (- end start))]
    (with-open [is (java.io.FileInputStream. f)]
      (-> is .getChannel (.position start))
      (.read is arr))
    arr))

(comment

  (let [f manifest-file
        end nil
        end (or end (dec (min (fs/size f) (* 1024 1024))))]
    end)
  ;; => 457

  ;; Should be safe to do this... 🙈

  (-> (read-bytes manifest-file [0 31])
      (String.))
  ;; => "{\n    \"name\": \"Soundcljoud\",\n   "

  (-> (read-bytes manifest-file [14 29])
      (String.))
  ;; => "\"Soundcljoud\",\n "

  ;; Never in doubt... 😌

  )
```

OK, we're making some progress here. In fact, it seems that we have most of the
pieces we'll need to actually fulfil a range request, so let's see about
sticking them together in a reasonable way.

## How do you respond?

Let's review what the response to a range request is supposed to look like:

``` text
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Length: 1048576
Content-Range: bytes 0-1048575/5943424
Content-Type: audio/mpeg
```

At the moment, we're just using the `body` function to respond to range
requests:

``` clojure
(defn file-router [dir headers]
  ;; ...
              (cond
                ;; ...
                (and (fs/readable? f) (contains? (:headers req) "range"))
                (do
                  (swap! state update :log conj "Handling range request")
                  (body f))
                ;; ...
              )
 ;; ...
 )
```

And `body` just chucks the file into a map with some headers:

``` clojure
(defn- body
  ([path]
   (body path {}))
  ([path headers]
   {:headers (merge {"Content-Type" (ext-mime-type (fs/file-name path))} headers)
    :body (fs/file path)}))
```

Let's follow suit. Since http-kit is so magical and wonderful, we'll go out on a
limb and make the assumption that if we just stuff our byte array into the
response body, http-kit will do The Right Thing™.

``` clojure
(defn- byte-range
  ([path request-headers]
   (byte-range path request-headers {}))
  ([path request-headers response-headers]
   (let [f (fs/file path)
         [start end
          :as requested-range] (parse-range-header (request-headers "range"))
         arr (read-bytes f requested-range)
         num-bytes-read (count arr)]
     {:status 206
      :headers (merge {"Content-Type" (ext-mime-type (fs/file-name path))
                       "Accept-Ranges" "bytes"
                       "Content-Length" num-bytes-read
                       "Content-Range" (format "bytes %d-%d/%d"
                                               start
                                               (+ start num-bytes-read)
                                               (fs/size f))}
                      response-headers)
      :body arr})))

(comment

  (byte-range manifest-file {"range" "bytes=0-"})
  ;; => {:status 206,
  ;;     :headers
  ;;     {"Content-Type" nil,
  ;;      "Accept-Ranges" "bytes",
  ;;      "Content-Length" 458,
  ;;      "Content-Range" "bytes 0-457/458"},
  ;;     :body
  ;;     [123, 10, 32, 32, 32, 32, 34, 110, 97, 109, 101, 34, 58, 32, 34, 83, 111, 117,
  ;;      110, 100, 99, 108, 106, 111, 117, 100, 34, 44, 10, 32, 32, 32, 32, 34, 115,
  ;;      104, 111, 114, 116, 95, 110, 97, 109, 101, 34, 58, 32, 34, 83, 111, 117, 110,
  ;;      100, 99, 108, 106, 111, 117, 100, 34, 44, 10, 32, 32, 32, 32, 34, 105, 99,
  ;;      111, 110, 115, 34, 58, 32, 91, 10, 32, 32, 32, 32, 32, 32, 32, 32, 123, 10,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 114, 99, 34, 58, 32,
  ;;      34, 105, 99, 111, 110, 115, 47, 97, 110, 100, 114, 111, 105, 100, 45, 99,
  ;;      104, 114, 111, 109, 101, 45, 49, 57, 50, 120, 49, 57, 50, 46, 112, 110, 103,
  ;;      34, 44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 105,
  ;;      122, 101, 115, 34, 58, 32, 34, 49, 57, 50, 120, 49, 57, 50, 34, 44, 10, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 116, 121, 112, 101, 34, 58,
  ;;      32, 34, 105, 109, 97, 103, 101, 47, 112, 110, 103, 34, 10, 32, 32, 32, 32,
  ;;      32, 32, 32, 32, 125, 44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 123, 10, 32, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 114, 99, 34, 58, 32, 34,
  ;;      105, 99, 111, 110, 115, 47, 97, 110, 100, 114, 111, 105, 100, 45, 99, 104,
  ;;      114, 111, 109, 101, 45, 53, 49, 50, 120, 53, 49, 50, 46, 112, 110, 103, 34,
  ;;      44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 105, 122,
  ;;      101, 115, 34, 58, 32, 34, 53, 49, 50, 120, 53, 49, 50, 34, 44, 10, 32, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 116, 121, 112, 101, 34, 58, 32,
  ;;      34, 105, 109, 97, 103, 101, 47, 112, 110, 103, 34, 10, 32, 32, 32, 32, 32,
  ;;      32, 32, 32, 125, 10, 32, 32, 32, 32, 93, 44, 10, 32, 32, 32, 32, 34, 116,
  ;;      104, 101, 109, 101, 95, 99, 111, 108, 111, 114, 34, 58, 32, 34, 35, 102, 102,
  ;;      102, 102, 102, 102, 34, 44, 10, 32, 32, 32, 32, 34, 98, 97, 99, 107, 103,
  ;;      114, 111, 117, 110, 100, 95, 99, 111, 108, 111, 114, 34, 58, 32, 34, 35, 102,
  ;;      102, 102, 102, 102, 102, 34, 44, 10, 32, 32, 32, 32, 34, 100, 105, 115, 112,
  ;;      108, 97, 121, 34, 58, 32, 34, 115, 116, 97, 110, 100, 97, 108, 111, 110, 101,
  ;;      34, 10, 125, 10]}

  )
```

That looks fairly reasonable. Let's now complete the plumbing so when we turn on
the tap of range requests, we get a delicious stream of ice cold, alpine spring
fed responses flowing back:

``` clojure
(defn file-router [dir headers]
  ;; ...
              (cond
                ;; ...
                (and (fs/readable? f) (contains? (:headers req) "range"))
                (do
                  (swap! state update :log conj "Handling range request")
                  (byte-range f (:headers req)))
                ;; ...
              )
 ;; ...
 )

(comment

  ((file-router dir {}) {:headers {"range" "bytes=0-"}
                         :uri "/site.webmanifest"})
  ;; => {:status 206,
  ;;     :headers
  ;;     {"Content-Type" nil,
  ;;      "Accept-Ranges" "bytes",
  ;;      "Content-Length" 458,
  ;;      "Content-Range" "bytes 0-457/458"},
  ;;     :body
  ;;     [123, 10, 32, 32, 32, 32, 34, 110, 97, 109, 101, 34, 58, 32, 34, 83, 111, 117,
  ;;      110, 100, 99, 108, 106, 111, 117, 100, 34, 44, 10, 32, 32, 32, 32, 34, 115,
  ;;      104, 111, 114, 116, 95, 110, 97, 109, 101, 34, 58, 32, 34, 83, 111, 117, 110,
  ;;      100, 99, 108, 106, 111, 117, 100, 34, 44, 10, 32, 32, 32, 32, 34, 105, 99,
  ;;      111, 110, 115, 34, 58, 32, 91, 10, 32, 32, 32, 32, 32, 32, 32, 32, 123, 10,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 114, 99, 34, 58, 32,
  ;;      34, 105, 99, 111, 110, 115, 47, 97, 110, 100, 114, 111, 105, 100, 45, 99,
  ;;      104, 114, 111, 109, 101, 45, 49, 57, 50, 120, 49, 57, 50, 46, 112, 110, 103,
  ;;      34, 44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 105,
  ;;      122, 101, 115, 34, 58, 32, 34, 49, 57, 50, 120, 49, 57, 50, 34, 44, 10, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 116, 121, 112, 101, 34, 58,
  ;;      32, 34, 105, 109, 97, 103, 101, 47, 112, 110, 103, 34, 10, 32, 32, 32, 32,
  ;;      32, 32, 32, 32, 125, 44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 123, 10, 32, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 114, 99, 34, 58, 32, 34,
  ;;      105, 99, 111, 110, 115, 47, 97, 110, 100, 114, 111, 105, 100, 45, 99, 104,
  ;;      114, 111, 109, 101, 45, 53, 49, 50, 120, 53, 49, 50, 46, 112, 110, 103, 34,
  ;;      44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 115, 105, 122,
  ;;      101, 115, 34, 58, 32, 34, 53, 49, 50, 120, 53, 49, 50, 34, 44, 10, 32, 32,
  ;;      32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 116, 121, 112, 101, 34, 58, 32,
  ;;      34, 105, 109, 97, 103, 101, 47, 112, 110, 103, 34, 10, 32, 32, 32, 32, 32,
  ;;      32, 32, 32, 125, 10, 32, 32, 32, 32, 93, 44, 10, 32, 32, 32, 32, 34, 116,
  ;;      104, 101, 109, 101, 95, 99, 111, 108, 111, 114, 34, 58, 32, 34, 35, 102, 102,
  ;;      102, 102, 102, 102, 34, 44, 10, 32, 32, 32, 32, 34, 98, 97, 99, 107, 103,
  ;;      114, 111, 117, 110, 100, 95, 99, 111, 108, 111, 114, 34, 58, 32, 34, 35, 102,
  ;;      102, 102, 102, 102, 102, 34, 44, 10, 32, 32, 32, 32, 34, 100, 105, 115, 112,
  ;;      108, 97, 121, 34, 58, 32, 34, 115, 116, 97, 110, 100, 97, 108, 111, 110, 101,
  ;;      34, 10, 125, 10]}

  )
```

Looks great... except for the `Content-Type: nil` bit, since our server has no
clue what a `.webmanifest` extension portends, but who cares about such trivial
details, since we're not gonna be getting range requests for non-media files
anyway. Plus, a standard request for that file does the same thing:

``` clojure
(comment

  ((file-router dir {}) {:headers {}
                         :uri "/site.webmanifest"})
  ;; => {:headers {"Content-Type" nil},
  ;;     :body
  ;;     #object[java.io.File 0x659969c9 "../soundcljoud/player/public/site.webmanifest"]}

  )
```

🤷

Before we break out the 🍾 though, let's try this in the wild. And before we try
this in the wild, it probably behoves us—at least, I feel rather behoved, and
it's my blog, so I'm going to follow this deep sense of behoval where it
leads—to log responses as well as requests, so let's make one last minor change
to good 'ol `file-router`:

``` clojure
(defn file-router [dir headers]
  (fn [{:keys [uri] :as req}]
    ;; 👉 Move the state swappage from here...
    (let [f (fs/path dir (str/replace-first (URLDecoder/decode uri) #"^/" ""))
          index-file (fs/path f "index.html")
          res
          (update (cond
                    (and (fs/directory? f) (fs/readable? index-file))
                    (body index-file)

                    (fs/directory? f)
                    (index dir f)

                    (and (fs/readable? f) (contains? (:headers req) "range"))
                    (do
                      (swap! state update :log conj "Handling range request")
                      (byte-range f (:headers req)))

                    (fs/readable? f)
                    (body f)

                    (and (nil? (fs/extension f)) (fs/readable? (with-ext f ".html")))
                    (body (with-ext f ".html") headers)

                    :else
                    {:status 404 :body (str "Not found `" f "` in " dir)})
                  :headers (fn [response-headers]
                             (merge headers response-headers)))]
      ;; ...to here 👇
      (swap! state
             update :requests
             conj {:request req, :response (dissoc res :body)})
      res)))
```

## I'm the one that put the Range in the Rover

Casting our minds back to [the last post in this potentially infinite sequence
of posts](2024-07-20-soundcljoud-cloudy.html#is_this_the_end%3F), we recall that
Soundcljoud was unable to seek in the audio file. Let's repeat this experience
by jumping over to `soundcljoud/player/public/soundcljoud.cljs`:

``` clojure
(comment

  (-> (get-el "audio")
      (.-seekable)
      (.-length))
  ;; => 1

  (let [s (-> (get-el "audio")
              (.-seekable))]
    [(.start s 0) (.end s 0)])
  ;; => [0 0]
  
  )
```

This is what we expected, since we haven't restarted the server to apply our
changes. Let's do that now (back in our http-server REPL):

``` clojure
(comment

  (restart-server)
  ;; => {:requests [],
  ;;     :log [],
  ;;     :server
  ;;     #object[clojure.lang.AFunction$1 0x2d75d828 "clojure.lang.AFunction$1@2d75d828"]}

  )
```

Now we can click on another track in Soundcljoud and see what happens. 😬

![Clicking on a track in the Soundcljoud UI][hope]
[hope]: assets/2024-08-13-soundcljoud-rangey-hope.png "It's the hope that kills" width=800 border=1

OK, nothing blew up. Let's look at the request in the http-server logs:

``` clojure
(comment

  (->> (:requests @state)
       (filter #(str/ends-with? (get-in % [:request :uri]) ".mp3"))
       (map (fn [{:keys [request response]}]
              {:request {:uri (:uri request)
                         :headers (select-keys (:headers request)
                                               ["range"])}
               :response response})))
  ;; => ({:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=0-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 1048576,
  ;;        "Content-Range" "bytes 0-1048575/3426432"}}})

  )
```

So far, so good. But if we now seek, can we find? Let's ask in our Soundcljoud
REPL:

``` clojure
(comment

  (let [seekable (-> (get-el "audio") (.-seekable))]
    (->> (.-length seekable)
         range
         (map (fn [i]
                [(.start seekable i) (.end seekable i)]))))
  ;; => ([0 142.654694])
  
  )
```

And if we actually click play? OMG we hear the sweet sweet sounds of a steel
guitar! And if we seek forward in the track? Garth sings! Let's just check in
with http-server one last time to see what it thinks:

``` clojure
(comment

  (->> (:requests @state)
       (filter #(str/ends-with? (get-in % [:request :uri]) ".mp3"))
       (map (fn [{:keys [request response]}]
              {:request {:uri (:uri request)
                         :headers (select-keys (:headers request)
                                               ["range"])}
               :response response})))
  ;; => ({:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=0-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 1048575,
  ;;        "Content-Range" "bytes 0-1048575/3426432"}}}
  ;;     {:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=0-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 1048575,
  ;;        "Content-Range" "bytes 0-1048575/3426432"}}}
  ;;     {:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=1048575-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 1048575,
  ;;        "Content-Range" "bytes 1048575-2097150/3426432"}}}
  ;;     {:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=2097150-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 1048575,
  ;;        "Content-Range" "bytes 2097150-3145725/3426432"}}}
  ;;     {:request
  ;;      {:uri
  ;;       "/Garth%20Brooks/Fresh%20Horses/Garth%20Brooks%20-%20It%27s%20Midnight%20Cinderella.mp3",
  ;;       :headers {"range" "bytes=3145725-"}},
  ;;      :response
  ;;      {:status 206,
  ;;       :headers
  ;;       {"Content-Type" "audio/mpeg",
  ;;        "Accept-Ranges" "bytes",
  ;;        "Content-Length" 280706,
  ;;        "Content-Range" "bytes 3145725-3426431/3426432"}}})

  )
```

Now that, my friends, smells like the sweet sweet smell of...

![A woman on a beach at sunrise with her head thrown back, saying "Victory"][victory]
[victory]: assets/2024-08-13-victory.jpg "Never in doubt" width=800px

Ah... it's been a while since I've been able to use that lovely image. 🌅

## Is this the end?

Well... the fact that I'm asking this rhetorical question points to the answer
likely being "no". 😅

And in fact it isn't the end, because I feel (perhaps arrogantly so) that this
range support could be useful to others using babashka.http-server, so I should
probably open up a pull request for the [borkiest of
dudes](https://github.com/borkdude) to review. I'll quickly [fork http-server](https://github.com/jmglov/http-server) on
Github, then update my remotes in [magit](https://magit.vc/) to make `origin`
point to `git@github.com:jmglov/http-server.git` and `upstream` point to
`git@github.com:babashka/http-server.git`, stash my changes, create a
`range-requests` branch, then pop the stash.

I doubt Señor Borkdude will be terribly impressed by my [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment)
and state atom, so I'd better go ahead and remove that nonsense before
committing. I'll open a [feature
request](https://github.com/babashka/http-server/issues/16) on the Github
project as well, since I know this is how Borkdude prefers to work.

With this, I have a fairly minimal commit that I'm ready to subject to the
slings and arrows of outrageous fortune that are part of any Borkdude code
review:

``` diff
range-requests a87a841e02d362ae8dc346153b166d28882c3c6e
Author:     Josh Glover <jmglov@jmglov.net>
AuthorDate: Tue Aug 13 14:18:47 2024 +0200
Commit:     Josh Glover <jmglov@jmglov.net>
CommitDate: Tue Aug 13 17:08:31 2024 +0200

Support range requests

2 files changed, 42 insertions(+)
CHANGELOG.md                 |  4 ++++
src/babashka/http_server.clj | 38 ++++++++++++++++++++++++++++++++++++++

modified   CHANGELOG.md
@@ -2,6 +2,10 @@
 
 [Http-server](https://github.com/babashka/http-server): Serve static assets with [babashka](https://babashka.org/)
 
+## Unreleased
+
+- [#16](https://github.com/babashka/http-server/issues/16): support range requests
+
 ## 0.1.13
 
 - [#13](https://github.com/babashka/http-server/issues/13): add an ending slash to the dir link, and don't encode the slashes ([@KDr2](https://github.com/KDr2))
modified   src/babashka/http_server.clj
@@ -165,6 +165,41 @@
    {:headers (merge {"Content-Type" (ext-mime-type (fs/file-name path))} headers)
     :body (fs/file path)}))
 
+(defn- parse-range-header [range-header]
+  (map #(when % (Long/parseLong %))
+       (-> range-header
+           (str/replace #"^bytes=" "")
+           (str/split #"-"))))
+
+(defn- read-bytes [f [start end]]
+  (let [end (or end (dec (min (fs/size f)
+                              (+ start (* 1024 1024)))))
+        arr (byte-array (- end start))]
+    (with-open [is (java.io.FileInputStream. f)]
+      (-> is .getChannel (.position start))
+      (.read is arr))
+    arr))
+
+(defn- byte-range
+  ([path request-headers]
+   (byte-range path request-headers {}))
+  ([path request-headers response-headers]
+   (let [f (fs/file path)
+         [start end
+          :as requested-range] (parse-range-header (request-headers "range"))
+         arr (read-bytes f requested-range)
+         num-bytes-read (count arr)]
+     {:status 206
+      :headers (merge {"Content-Type" (ext-mime-type (fs/file-name path))
+                       "Accept-Ranges" "bytes"
+                       "Content-Length" num-bytes-read
+                       "Content-Range" (format "bytes %d-%d/%d"
+                                               start
+                                               (+ start num-bytes-read)
+                                               (fs/size f))}
+                      response-headers)
+      :body arr})))
+
 (defn- with-ext [path ext]
   (fs/path (fs/parent path) (str (fs/file-name path) ext)))
 
@@ -179,6 +214,9 @@
                 (fs/directory? f)
                 (index dir f)
 
+                (and (fs/readable? f) (contains? (:headers req) "range"))
+                (byte-range f (:headers req))
+
                 (fs/readable? f)
                 (body f)
```

Wish me well, folks! If I'm not heard from again, you'll know that my [pull
request](https://github.com/babashka/http-server/pull/17) was found to be
sub-par and I was sent to Java Jail to work on an enterprise workflow management
system. 😭

# OK but now are we done?

Soundcljoud has clearly now implemented the critical functionality of
Soundcloud, so I could call it a day, but I'm loathe to do that when I could
instead extend it to be the best podcast player that ever was! Maybe I'll
rebrand it OverClj... or better yet, CljerCast! VCs, get your wallets ready and
stay posted for the next instalment of the exciting Soundcljoud series, right
here on jmglov.net!

Previously on Soundcljoud:
- Part 1: [Soundcljoud, or a young man's Soundcloud
clonejure](2024-07-09-soundcljoud.html)
- Part 2: [Soundcljoud gets more
cloudy](2024-07-20-soundcljoud-cloudy.html)

## Photo credits

What's Going on with that Body cover art:
- Mashup by Josh Glover
- Concert photo by [Andre Benz on
Unsplash](https://unsplash.com/photos/people-watching-concert-Jb7TLs6fW_I)
- Photo of Pitbull by Photobra Adam Bielawski - Own work, CC BY-SA 3.0
  [https://commons.wikimedia.org/w/index.php?curid=14736058](https://commons.wikimedia.org/w/index.php?curid=14736058)
- Photo of Nicki Minaj by Rory from Glasgow, United Kingdom - IMG_4388, CC BY 2.0
 [https://commons.wikimedia.org/w/index.php?curid=115814812](https://commons.wikimedia.org/w/index.php?curid=115814812)
