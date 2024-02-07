Title: Counting blog posts in 50 simple steps
Date: 2024-02-09
Tags: 50-simple-steps,clojure,nix
Description: In which one does not simply count the posts in one's blog
Image: assets/2024-02-09-preview.png
Image-Alt: An abacus
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1707474642683389

![An abacus][preview]
[preview]: assets/2024-02-09-preview.png "Photo by Crissy Jarvis on Unsplash" width=800px

As I was writing [yesterday's post](2024-02-08-back-in-the-hammock-again.html), I
wanted to count the number of posts I had written during my last involuntary
vacation in the summer of 2022. There is of course a very simple way to do this:

1. Navigate to the [archive page](archive.html) of my blog
2. Realise that whilst there are only (as of the writing of yesterday's post)
   two posts in 2024 (three as of the writing of this post, since yesterday's is
   now there, and in fact four as of the reading of this post, because now this
   post is also there) and three in 2023, there were enough in 2022 that
   counting them by hand smacks of effort
3. Decide to use [Babashka](https://github.com/babashka/babashka) to count the
   posts, since this blog is powered by
   [quickblog](https://github.com/borkdude/quickblog) and thus the posts are
   right at hand
4. Assert that it should be as easy as dropping a `user.clj` on the blog's
   classpath
5. Open up the blog's
   [bb.edn](https://github.com/jmglov/jmglov.net/blob/main/bb.edn) to figure out
   how to do this:
   ``` clojure
   {:deps {jmglov/jmglov {:local/root "."}
           io.github.borkdude/quickblog {:local/root "../clojure/quickblog"}
           #_"You use the newest SHA here:"
           #_{:git/sha "b69c11f4292702f78a8ac0a9f32379603bebf2af"}
          }
    ;; ...
   }
   ```
6. Realise that the classpath is set up in `deps.edn`, because of this
   incantation:
   ``` clojure
   {jmglov/jmglov {:local/root "."}
   ```
7. Open up `deps.edn` instead:
   ``` clojure
   {:paths ["." "classes"]
    :deps {markdown-clj/markdown-clj {:mvn/version "1.10.7"}
           org.babashka/cli {:mvn/version "0.8.55"}
           babashka/fs {:mvn/version "0.1.6"}
           org.clojure/data.xml {:mvn/version "0.2.0-alpha6"}
           hiccup/hiccup {:mvn/version "2.0.0-alpha2"}
           babashka/pods {:git/url "https://github.com/babashka/pods"
                          :git/sha "93081b75e66fb4c4d161f89e714c6b9e8d55c8d5"}
           rewrite-clj/rewrite-clj {:mvn/version "1.1.45"}
           selmer/selmer {:mvn/version "1.12.53"}}}
   ```
8. Armed with the knowledge that base directory is on the path, drop a
   `user.clj` there:
   ``` clojure
   (ns user)
   ```
9. Poise your hands elegantly above your keyboard like a concert pianist, then,
   imagining the swells of the string section as they invite you in and the
   upraised faces of the expecting crowd, attack the keyboard with a **C-c M-j**
   (`cider-jack-in-clj`) and select babashka from the REPL options! 🎉
10. Refer back to `bb.edn` to see how the quickblog opts are defined:
    ``` clojure
    {:deps { ; ...
            }
     :tasks
     {:init (def opts {:blog-title "jmglov's blog"
                       :blog-author "Josh Glover"
                       :blog-description "A blog about stuff but also things."
                       :blog-root "https://jmglov.net/blog/"
                       :about-link "https://jmglov.net/"
                       :twitter-handle "jmglov"
                       :assets-dir "blog/assets"
                       :num-index-posts 3
                       :cache-dir ".cache"
                       :favicon true
                       :favicon-dir "favicon"
                       :out-dir "public/blog"
                       :posts-dir "blog/posts"
                       :templates-dir "blog/templates"})
   
      :requires ([babashka.cli]
                 [babashka.fs :as fs]
                 [clojure.string :as str]
                 [quickblog.api :as qb]
                 [quickblog.cli :as cli])
      ;; ...
     }}
    ```
11. Start to copy the `(def opts {...})` bit into `user.clj`, but then, struck
    by a blinding flash of insight that `bb.edn` is just EDN, decide to Not
    Repeat Yourself ([NRY](https://en.wikipedia.org/wiki/Don't_repeat_yourself),
    obv) and just read in `bb.edn` and set opts from what's defined there:
    ``` clojure
    (ns user
      (:require [clojure.edn :as edn]))

    (comment
   
      (-> (slurp "bb.edn")
          edn/read-string
          :tasks
          :init)
      ;; (def opts {:blog-title "jmglov's blog"
                    :blog-author "Josh Glover"
                    :blog-description "A blog about stuff but also things."
                    :blog-root "https://jmglov.net/blog/"
                    :about-link "https://jmglov.net/"
                    :twitter-handle "jmglov"
                    :assets-dir "blog/assets"
                    :num-index-posts 3
                    :cache-dir ".cache"
                    :favicon true
                    :favicon-dir "favicon"
                    :out-dir "public/blog"
                    :posts-dir "blog/posts"
                    :templates-dir "blog/templates"})
    )
   ```
12. Celebrate your genius and [eval](https://clojuredocs.org/clojure.core/eval)
    that string (making sure of course that you're only eval'ing it if it's
    `(def opts ...)` and nothing untoward and/or sinister)!
    ``` clojure
    (comment
   
      (let [[form & params :as expr] (-> (slurp "bb.edn")
                                         edn/read-string
                                         :tasks
                                         :init)]
        (when (and (= 'def form) (= 'opts (first params)))
          (eval expr)))
      ;; => #'user/opts
    
      opts
      ;; => {:blog-description "A blog about stuff but also things.",
      ;;     :blog-author "Josh Glover",
      ;;     :num-index-posts 3,
      ;;     :favicon-dir "favicon",
      ;;     :posts-dir "blog/posts",
      ;;     :assets-dir "blog/assets",
      ;;     :templates-dir "blog/templates",
      ;;     :favicon true,
      ;;     :out-dir "public/blog",
      ;;     :blog-root "https://jmglov.net/blog/",
      ;;     :link-posts true,
      ;;     :cache-dir ".cache",
      ;;     :about-link "https://jmglov.net/",
      ;;     :blog-title "jmglov's blog"}
    
    )
    ```
12. Revel in the power of Lisp: verily code is data and data is code!
13. Realise that you are a silly silly person and that there's a much less
    ridiculous way to do this: move the opts to a file named `opts.edn`...
    ``` clojure
        {:blog-title "jmglov's blog"
     :blog-author "Josh Glover"
     :blog-description "A blog about stuff but also things."
     :blog-root "https://jmglov.net/blog/"
     :about-link "https://jmglov.net/"
     :assets-dir "blog/assets"
     :num-index-posts 3
     :cache-dir ".cache"
     :favicon true
     :favicon-dir "favicon"
     :out-dir "public/blog"
     :posts-dir "blog/posts"
     :templates-dir "blog/templates"
     :link-posts true}
    ```
14. ...and read them in `bb.edn`:
    ``` clojure
    { ; ...
     :tasks
     {:requires ([babashka.cli]
                 [babashka.fs :as fs]
                 [clojure.edn :as edn]
                 [clojure.string :as str]
                 [quickblog.api :as qb]
                 [quickblog.cli :as cli])
      :init (def opts (slurp "opts.edn"))
      ;; ...
     }}
    ```
15. ...and also in `user.clj`:
    ``` clojure
    (ns user
      (:require [clojure.edn :as edn]))
      
    (comment
    
      (def opts (-> (slurp "opts.edn") edn/read-string))
      ;; => #'user/opts
    
      opts
      ;; => {:blog-description "A blog about stuff but also things.",
      ;;     :blog-author "Josh Glover",
      ;;     :num-index-posts 3,
      ;;     :favicon-dir "favicon",
      ;;     :posts-dir "blog/posts",
      ;;     :assets-dir "blog/assets",
      ;;     :templates-dir "blog/templates",
      ;;     :favicon true,
      ;;     :out-dir "public/blog",
      ;;     :blog-root "https://jmglov.net/blog/",
      ;;     :link-posts true,
      ;;     :cache-dir ".cache",
      ;;     :about-link "https://jmglov.net/",
      ;;     :blog-title "jmglov's blog"}
    
     )
    ```
16. Now try and remember how to load posts in quickblog. Maybe [the API
    documentation](https://github.com/borkdude/quickblog/blob/ebf91f5859d36aeee1a52af14538f379eb76c64a/API.md#render)
    has a clue?
    ``` text
    (render opts)
    
    Renders posts declared in posts.edn to out-dir.
    ```
17. Conclude that whilst rendering the blog shouldn't be necessary to count the
    posts, the [source code for
    `render`](https://github.com/borkdude/quickblog/blob/ebf91f5859d36aeee1a52af14538f379eb76c64a/src/quickblog/api.clj#L467)
    must contain incantations of great power that load posts before rendering
    them:
    ``` clojure
    (defn render
      "Renders posts declared in `posts.edn` to `out-dir`."
      [opts]
      (let [{:keys [assets-dir
                    assets-out-dir
                    cache-dir
                    favicon-dir
                    favicon-out-dir
                    out-dir
                    posts-file
                    templates-dir]
             :as opts}
            (-> opts apply-default-opts lib/refresh-cache)]
      ;; ...
      ))
    ```
18. Armed with this arcane knowledge, return to `user.clj`:
    ``` clojure
    (ns user
      (:require ; ...
                [quickblog.api :as qb]
                [quickblog.internal :as lib]))
    
    (defn load-opts [base-opts]
      (-> base-opts
          #'qb/apply-default-opts
          lib/refresh-cache))
    
    (comment
    
      (def base-opts (-> (slurp "opts.edn") edn/read-string))
      ;; => #'user/base-opts
    
      (def opts (load-opts base-opts))
      ;; => #'user/opts
    
      (keys opts)
      ;; => (:blog-description
      ;;     :blog-author
      ;;     :num-index-posts
      ;;     :favicon-dir
      ;;     :posts-dir
      ;;     :assets-dir
      ;;     :modified-posts
      ;;     :cached-posts
      ;;     :templates-dir
      ;;     :deleted-posts
      ;;     :favicon
      ;;     :modified-metadata
      ;;     :out-dir
      ;;     :blog-root
      ;;     :modified-tags
      ;;     :link-posts
      ;;     :cache-dir
      ;;     :about-link
      ;;     :blog-title
      ;;     :posts)
    
     )
    ```
19. Count all the posts!
    ``` clojure
    (comment
    
      (->> opts
           :posts
           count)
      ;; => 71
    
     )
    ```
20. Break out the champagne! 🍾
21. Remember that the goal wasn't to count all of the posts, but rather the ones
    from the summer of 2022 before I started my old new job 😢
22. See what a post looks like so you can figure out this whole date thing:
    ``` clojure
    (comment
    
      ;; => (["2022-08-26-doing-software-wrong.md"
      ;;      {:description
      ;;       "In which I make a bold statement, but then rather than explaining it or providing any evidence whatsoever, go on to talk about something completely different.",
      ;;       :tags #{"waffle"},
      ;;       :date "2022-08-26",
      ;;       :file "2022-08-26-doing-software-wrong.md",
      ;;       :title "We're doing software wrong",
      ;;       :image-alt
      ;;       "A man on a mobile phone stands in front of a wall with the word \"productivity\" written on it - Photo by Andreas Klassen on Unsplash",
      ;;       :image "assets/2022-08-26-preview.jpg",
      ;;       :html #<Delay@6b4fc6d6: :not-delivered>}])
    
     )
    ```
23. Realise that `:posts` is a map of filename to post, but no matter!
``` clojure
(comment

  (->> opts
       :posts
       vals
       (map :date))
  ;; => ("2022-08-26"
  ;;     "2022-06-22"
  ;;     "2023-11-12"
  ;;     "2022-07-01"
  ;;     "2022-07-31"
  ;;     "2022-07-09"
  ;;     "2022-06-21"
  ;;     ...
  ;;     "2022-07-02")

 )
```
24. Sigh as you come to terms with the fact that you're going to need to do some
    date parsing and you never remember how to use
    [java.time](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
    and argh!
25. Smile as you remember about [tick](https://github.com/juxt/tick), which is a
    library from our good friends at [JUXT](https://www.juxt.pro/) that provides
    a nicer API for this sort of stuff
26. Since you hate restarting your REPL, use the power of Babashka to hotload
    the dependency right in `user.clj`:
```clojure
(ns user
  (:require [babashka.deps :as deps]
            ;; ...
            ))

(comment

  (deps/add-deps '{:deps {tick/tick {:mvn/version "0.7.5"}}})
  ;; => nil

  (require '[tick.core :as t])
  ;; => clojure.lang.ExceptionInfo: Could not resolve symbol: java.time.temporal.TemporalQuery user cljc/java_time/format/date_time_formatter.clj:33:444

 )
```
27. Swear as you realise that tick must not work with Babashka 🤬
28. Scratch your head as you read that tick is "a Clojure(Script) & babashka
    library for dealing with time" and explicitly mentions Babashka so 😕
29. Search #babashka on Clojurians Slack for tick and find [borkdude
    mentioning](https://clojurians.slack.com/archives/CLX41ASCS/p1677842827651279)
    [a message in
    #announcements](https://clojurians.slack.com/archives/C06MAR553/p1677842754963519)
    that claims that "the new version works with babashka". Note the date of
    this message as 2023-04-03.
30. Suspect that you may be running an older version of Babashka that doesn't
    ship with `java.time.temporal.TemporalQuery` and have a look at
    [src/babashka/impl/classes.clj](https://github.com/babashka/babashka/blob/771f69d922b9c674bdfa1ae02f53691c54e28170/src/babashka/impl/classes.clj#L453)
    in the Babashka codebase to see when it was added
31. Track down [commit
    ead237e](https://github.com/babashka/babashka/commit/ead237eee335fc3bc4cdfc88e3fb4af821878b3c)
    and note that it made it into Babashka
    [v1.2.174](https://github.com/babashka/babashka/releases/tag/v1.2.174)
32. Figure out what version of Babashka you're running:
    ``` text
    $ bb --version
    babashka v1.1.173
    ```
33. Realise that you've found the issue and need to upgrade Babashka
34. Since you're using [Home
    Manager](https://github.com/nix-community/home-manager) on
    [NixOS](https://nixos.org/) like a normal person who is totally normal, open
    up your
    [home.nix](https://github.com/jmglov/nixos-config/blob/79752ae530ab0036b569cc0bc848cdda29a43af8/jmglov/home.nix)
35. Note that apparently you're installing the binary version of Babashka in
    your own package that you wrote like a normal person who is totally normal
    and open up
    [pkgs/babashka-bin/default.nix](https://github.com/jmglov/nixos-config/blob/79752ae530ab0036b569cc0bc848cdda29a43af8/jmglov/pkgs/babashka-bin/default.nix):
    ``` nix
    { stdenv, ... }:
    
    let
      arch = if stdenv.isAarch64 then "aarch64" else "amd64";
      osName = if stdenv.isDarwin then
        "macos"
      else if stdenv.isLinux then
        "linux"
      else
        null;
      sha256 = assert !isNull osName;
        {
          linux = {
            aarch64 =
              "bc7e733863486b334b8bff83ba13b416800e0ce45050153cb413906b46090d68";
            amd64 =
              "25975d5424e7dea9fbaef5a6551ce7d3834631b5e28bdc4caf037bf45af57dfd";
          };
          macos = {
            # No MacOS builds for ARM at the moment
            # aarch64 =
            #   "11c4b4bd0b534db1ecd732b03bc376f8b21bbda0d88cacb4bbe15b8469029123";
            amd64 =
              "792ade86e61703170f3de3082183173db66a9a98b11d01c95ace0235f0a5e345";
          };
        }.${osName}.${arch};
    in stdenv.mkDerivation rec {
      pname = "babashka";
      version = "1.1.173";
      filename = if osName == "macos" then
      # No static builds for MacOS
        "babashka-${version}-${osName}-${arch}.tar.gz"
      else
        "babashka-${version}-${osName}-${arch}-static.tar.gz";
    
      src = builtins.fetchurl {
        inherit sha256;
        url =
          "https://github.com/babashka/babashka/releases/download/v${version}/${filename}";
      };
    
      dontFixup = true;
      dontUnpack = true;
    
      installPhase = ''
        mkdir -p $out/bin
        cd $out/bin && tar xvzf $src
      '';
    }
    ```
36. Avoid the urge to celebrate your own genius and instead pop over to the
    [Babashka releases page](https://github.com/babashka/babashka/releases) on
    Github and find that the latest release is
    [v1.3.188](https://github.com/babashka/babashka/releases/tag/v1.3.188)
37. Grab the SHA256 hashes you're going to need to plug into your Nix package:
    ``` text
    $ curl -L https://github.com/babashka/babashka/releases/download/v1.3.188/babashka-1.3.188-linux-aarch64-static.tar.gz.sha256
417280537b20754b675b7552d560c4c2817a93fbcaa0d51e426a1bff385e3e47
    $ curl -L https://github.com/babashka/babashka/releases/download/v1.3.188/babashka-1.3.188-linux-amd64-static.tar.gz.sha256
    89431b0659e84a468da05ad78daf2982cbc8ea9e17f315fa2e51fecc78af7cc0
    $ curl -L https://github.com/babashka/babashka/releases/download/v1.3.188/babashka-1.3.188-macos-aarch64.tar.gz.sha256
    77eb9ec502260fa94008e1e43edc5678fab8dc1a5082b7eb3d28ae594ea54e09
    $ curl -L https://github.com/babashka/babashka/releases/download/v1.3.188/babashka-1.3.188-macos-amd64.tar.gz.sha256
    d8854833a052bb578360294d6975b85ed917b9f86da0068fb3c263f8cbcc9e15
    ```
38. Update the SHAs and Babashka version in your Nix package:
    ``` nix
    let
      # ...
      sha256 = {
        linux = {
          aarch64 =
            "417280537b20754b675b7552d560c4c2817a93fbcaa0d51e426a1bff385e3e47";
          amd64 =
            "89431b0659e84a468da05ad78daf2982cbc8ea9e17f315fa2e51fecc78af7cc0";
        };
        macos = {
          aarch64 =
            "77eb9ec502260fa94008e1e43edc5678fab8dc1a5082b7eb3d28ae594ea54e09";
          amd64 =
            "d8854833a052bb578360294d6975b85ed917b9f86da0068fb3c263f8cbcc9e15";
        };
      }.${osName}.${arch};
    in stdenv.mkDerivation rec {
      pname = "babashka";
      version = "1.3.188";
      # ...
    }
    ```
39. Update Babashka:
    ``` text
    $ sudo nixos-rebuild switch
    building Nix...
    building the system configuration...
    these 8 derivations will be built:
      /nix/store/x9c0ip7xchwzhkhznvjz5r57krcqjm3r-babashka-1.3.188.drv
      /nix/store/lsq07jvqmk5kywbdrj55vh3ndjrw2vwm-home-manager-path.drv
    [...]
    building '/nix/store/x9c0ip7xchwzhkhznvjz5r57krcqjm3r-babashka-1.3.188.drv'...
    patching sources
    updateAutotoolsGnuConfigScriptsPhase
    configuring
    no configure script, doing nothing
    building
    no Makefile or custom buildPhase, doing nothing
    installing
    bb
    [...]
    activating the configuration...
    setting up /etc...
    reloading user units for jmglov...
    setting up tmpfiles
    restarting the following units: home-manager-jmglov.service
    ```
40. Trust but verify:
    ``` text
    $ bb --version
    babashka v1.3.188
    ```
41. Hang your head in shame as you prepare to restart your REPL, but take the
    opportunity to add tick to your `deps.edn` so you won't have to hotload it
    in `user.clj`:
    ``` clojure
    {:paths ["." "classes"]
     :deps { ; ...
            tick/tick {:mvn/version "0.7.5"}}}
    ```
42. Drop back into `user.clj`, then **C-c C-z** to hop to your REPL buffer,
    **C-c C-q** to quit it, then **C-c M-j** to start a new REPL, then require
    tick in the ns form:
    ``` clojure
    (ns user
      (:require ; ...
                [tick.core :as t]))
    ```
43. Evaluate the buffer with **C-c C-k** and get to ticking!
44. Figure out how to parse a date string like "2022-07-02" by looking at the
    [tick
    cheatsheet](https://github.com/juxt/tick/blob/master/docs/cheatsheet.md#fromto-strings),
    a sheet that lets you cheat, apparently:
    ``` clojure
    (comment
    
      (t/date "2022-07-02")
      ;; => #time/date "2022-07-02"
    
     )
    ```
46. Turn the date strings into date dates:
    ``` clojure
    (comment
    
      (def opts (-> (slurp "opts.edn") edn/read-string load-opts))
      ;; => #'user/opts
    
      (->> opts
           :posts
           vals
           (map (comp t/date :date)))
      ;; => (#time/date "2022-08-26"
      ;;     #time/date "2022-06-22"
      ;;     #time/date "2023-11-12"
      ;;     #time/date "2022-07-01"
      ;;     ...
      ;;     #time/date "2022-07-02")
    
     )
    ```
47. Ask yourself what you were doing again?
48. Oh yeah, counting posts before September 1, 2022, which was when I started
    my old new job
49. Say this in Clojure, not English!
    ``` clojure
    (comment
    
      (->> opts
           :posts
           vals
           (remove #(= "FIXME" (:date %)))
           (map (comp t/date :date))
           (filter #(t/< % (t/date "2022-09-01")))
           count)
      ;; => 55
    
     )
    ```
50. Sit back and reflect on just how easy that was and how it took less time
    than just counting those 55 things with your finger
