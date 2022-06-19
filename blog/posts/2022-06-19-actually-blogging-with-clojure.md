I admit that my previous post on [how to create a blog with
Clojure](2022-06-17-creating-a-blog-with-clojure.html) may have been slightly
tongue in cheek. This one will be less so, since I've actually gotten stuff
working. ;)

Assuming that you have [Babashka](https://github.com/babashka/babashka)
installed (which was really the only difficult part of my odyssey), cloning
[borkdude’s blog repo](https://github.com/borkdude/blog) from Github should give
you quite an easy start.

Here's all I needed to do after getting my Babashka issues sorted to make it my own:

1. Clone the repo: `git clone git@github.com:borkdude/blog.git`
2. Update `bb.edn` to change the `publish` task to do an `aws s3 sync` to the S3
   bucket my website is hosted from instead of rsync-ing to borkdude's webserver.
3. Run `rm posts/*.md` to get rid of all of borkdude's content.
4. Add a new `posts/2022-06-17-creating-a-blog-with-clojure.md` file and copy and
   paste the content from my Medium blog post into it.
5. Update `posts.edn` to remove the metadata for borkdude's content and replace
   it with my single post:
   ``` clojure
   {:title "Creating a blog with Clojure in 50 simple steps"
    :file "2022-06-17-creating-a-blog-with-clojure.md"
    :categories #{"clojure"}
    :date "2022-06-17"}
   ```
6. Update `render.clj` and change `blog-root` and `atom-feed` to reflect that
   this is my blog and not borkdude's.
7. Update `templates/base.html` to comment out the Github discussions link to
   borkdude's blog (I should put that back in once I make my website repo public)
   and replace his Twitter username with mine.
8. Open a pull request to [Planet
   Clojure](https://github.com/ghoseb/planet.clojure) to pick up
   http://jmglov.net/blog/atom.xml instead of my Medium feed.
9. Run `bb publish`.
10. Check out http://jmglov.net/blog/ and rejoice!

So it seems like having your kickass Clojure blog and eating it too only takes
10 easy steps, not 50.
