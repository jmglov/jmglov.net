# Blog

My [blog](https://blog.michielborkent.nl/).

See [this](https://blog.michielborkent.nl/migrating-octopress-to-babashka.html)
blog post about rewriting this blog from Octopress to babashka.

## Usage

To see all the stuff you can do, run:

```
bb tasks
```

### Publishing

To publish the entire site:

```
bb publish
```

To publish just the blog:

```
bb publish-blog
```

### Blogging

To create a new draft:

``` text
bb new-draft --file POST-NAME
```

To see all options for creating drafts:

``` text
bb new-draft --help
```
