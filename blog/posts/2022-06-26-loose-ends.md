Title: Tying off loose ends
Tags: clojure,aws,s3,babashka,lambda
Date: 2022-06-26

I have been working my way through the [old todo
list](2022-06-21-todo-list.html) in the five days since I wrote it, and I wanted
to give you an update.

As I predicted, all of the things that should have definitely happened did in
fact definitely happen:
- Fika with Esther. Beers were drunk and shade was sat it. Conversation was had,
  and verily it was good.
- Stop by the store on the way home from said fika. I got ingredients for
  harissa and guacamole, though my plans for making and consuming these
  delicacies did encounter a slight setback, as you will see.
- Walk my dog. Regular as clockwork, that dude gets his walks, though it's been
  bloody hot in Stockholm recently (today is 30 degrees!) and thus the poor
  fella doesn't look like he's enjoying them all that much.
- Play Guitar Hero with Simon. This happened, and it happened twice. On the day
  that I wrote the todo list, the original idea was to drink beer and eat
  burritos and guacamole and such prior to fucking out, but through an accident
  of fate, Simon needed to return home for dinner after the shredfest, and thus
  drove his car over instead of biking. This made beers an impossibility, and I
  decided that I didn't want to go to the trouble of making food that I wouldn't
  eat until much later, so only Guitar Hero was played. However, I'm happy to
  admit that my plans for Wednesday fell through because my friend Johan very
  rudely got the sniffles or something and thus couldn't make his leaving
  drinks, so Simon and I reprised our roles as the greatest middle aged rockers
  to have ever rocked clicky plastic guitars to an adoring crowd of my dog, and
  this time we did eat the black bean and rice burritos and loads of guacamole
  and drink some margaritas that I accidentally made double strong.

Some of the things that were likely to have happened have already happened, and
they shall be detailed below!

The thing that was unlikely to have happened did not in fact happen, but I don't
feel bad about that because I told you it was unlikely to happen anyway so in a
way, it's awesome that it didn't happen because otherwise I would have been
wrong in my prediction that it was unlikely to happen. Of course, the summer
isn't over yet, so it could still be unlikely to happen yet, Mr. Frodo.

## HTTPS for the blog

I did finally get HTTPS working for my blog, as you can see from the following
screenshot that looks something like what you would see in the address bar of
your very own browser should you choose to look up there at it:

![Browser address bar showing a shield and a
lock](assets/2022-06-26-address-bar.png)

Of course, the URL itself will be a bit different, but you know what I mean.

So here's what I needed to do (after following my [50 simple
steps](2022-06-24-s3-https.html), of course):
1. Use the [AWS Certificate Manager
   console](https://us-east-1.console.aws.amazon.com/acm/home?region=us-east-1#/welcome)
   (in the us-east-1 region; this is extremely important!) to create a
   certificate containing domain names `jmglov.net` and `www.jmglov.net`, using
   DNS validation since I host my own domain using Route 53.
2. Open the hosted zone for `jmglov.net` in the [Route 53
   console](https://us-east-1.console.aws.amazon.com/route53/v2/hostedzones#)
   and manually add the CNAME records displayed on the ACM certificate (for some
   reason, the **Create records in Route 53** button in ACM didn't work for me,
   but whatevs).
3. Use the [CloudFront
   console](https://us-east-1.console.aws.amazon.com/cloudfront/v3/home?region=eu-west-1#/)
   to create a distribuion, setting the origin to
   `jmglov.net.s3-website-eu-west-1.amazonaws.com`, adding `jmglov.net` and
   `www.jmglov.net` as alternate domain names, choosing the ACM certificate
   that I just created as the custom SSL certificate, and setting HTTP to
   redirect to HTTPS.
4. Wait for the CloudFront distribution to deploy. You can test this by making
   sure the S3 hosted site loads when you browse to the distribution domain name
   that CloudFront creates for you (in my case,
   https://d3bulohh9org4y.cloudfront.net).
5. Once the website loads using the distribution, use the Route 53 console to
   update the A records for `jmglov.net` and `www.jmglov.net` to alias my
   CloudFront distribution instead of my S3 bucket.

Et voilà!

Oh, and my website logs did of course start showing up in the S3 logs bucket I
created, after some delay probably caused by buffering. I also configured my
CloudFront distribution to log to the same place, and those logs are also
showing up there.

## Blambda!

I also created an AWS Lambda custom runtime for Babashka, which I have called
[Blambda!](https://github.com/jmglov/blambda), because I couldn't think of a
good pun involving BBs and lambdas or something like that.

This was also quite easy to do, thanks to a
[bb-lambda](https://github.com/tatut/bb-lambda) project that I found on Github
that already took care of the heavy lifting of interacting with the Lambda
runtime API to process function invocations.

I decided to create my own custom runtime instead of just straight up using
bb-lambda because it uses [Docker](https://www.docker.com/), which I hate and
fear (I don't even know what a container is, much less why you should use it to
implement a serverless function). Thanks to the borktacular
[borkdude](https://github.com/borkdude) ([toss him a few euros on
Ko-fi](https://ko-fi.com/borkdude) if you can) and the fact that he builds a
static executable for [each borkin' release of
Babashka](https://github.com/babashka/babashka/releases/tag/v0.8.156), creating
a lambda layer for a custom runtime is really easy!

## More stuff to do

Things that are likely to happen:
- Open up my Guitar Hero controller, attempt to fix it, don't succeed, but also
  don't make things any worse.
- Play some Civ V. I had [planned to do this
  yesterday](2022-06-25-midsommar.html), but ran out of time because I ended up
  going over to Simon and Pippa's for pizza (red peppers and aubergine /
  eggplant, thanks for asking!). It is imperative that I do this today, because
  those Koreans and Ottomans aren't going to defeat themselves. (Unless of
  course they turn on each other, which would be totally awesome!)
- Add some cool stuff to Blambda! to make it easier to deploy functions and the
  like.
- Dig into
  [REPL-acement](https://open.spotify.com/episode/4TPwgRZTOsHPGXuVwJQyHd) with
  Ray.
- Learn what's so awesome about [Nix flakes](https://nixos.wiki/wiki/Flakes).

Things that may happen:
- Open up my Guitar Hero controller, attempt to fix it, and succeed!
- Open up my Guitar Hero controller, attempt to fix it, and bollocks it all up
  and then cry tears of great sadness and hit [Blocket](https://www.blocket.se/)
  (kinda like a Swedish version of eBay or online garage sale thingy) to try to
  find a new to me used one.

Things that are unlikely to happen but really should:
- Learn Swedish.
