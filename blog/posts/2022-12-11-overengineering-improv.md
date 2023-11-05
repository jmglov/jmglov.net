Title: Over-Engineering Improv
Date: 2022-12-11
Tags: engineering
Description: In which I invent (I think) an icebreaker for engineers
Image: assets/2022-12-11-preview.png
Image-Alt: Photo by NOAA on Unsplash

I was walking Rover this morning through the snow, listening to a podcast on the
dangers of economism, when I realised that my mind had drifted away from
political theory and was stuck on a work-related problem. I've learned over the
years that it's completely futile to fight that and try to turn my attention
back to my podcast or book or whatever I was trying to do, so I leaned into it
and tried to see if I could just solve the problem so it would leave me in peace
and I could go back to enjoying my walk.

I've been a professional software engineer for about 20 years now, and these
intrusions have historically been oriented around why my multi-threaded C++ IVR
system is deadlocking or how to design DynamoDB tables for my serverless API or
whatever, but in the past couple of years, these problems have been more likely
oriented around organisational psychology, single-piece flow, or how to build a
good promotion case for that great engineer on my team. Yes, it's true: I'm an
engineering manager. And yes, I know, I swore that I would never be interested
in becoming a manager, but I also swore that I would never do a lot of things
that I have subsequently done and enjoyed, so it's no surprise that I was wrong
about this as well.

But anyway, back to the problem at hand. I'm managing a newly formed team, and
we're having a team kickoff session first thing in January. This is three days
of doing things like building a social contract, defining our team purpose,
meeting stakeholders, and deciding on ways of working, and I needed to find a
good way to start the session. One of the classic openers is breakfast and an
icebreaker, and I decided that if it ain't broke, don't fix it, so I should go
ahead and do that. So far, no problem.

The challenge then became what kind of icebreaker to use. I needed something
that would fit the following criteria:
1. Works remotely. A lot of icebreakers have a physical component to them that
   make them less or no fun over a video call.
2. Doesn't force people to disclose personal information that would make them
   uncomfortable. Seemingly innocuous games like everyone bringing a childhood
   photo and then the group trying to guess who's who can be traumatic for
   people with gender dysphoria, and games featuring questions like "what's the
   thing you most appreciate about your parents?" can be traumatic for survivors
   of child abuse. On the less extreme end of the spectrum, some people just
   don't feel comfortable sharing details about their personal life.
3. Fits in about half an hour.
4. Isn't one we've done recently.

One of my favourites is [Two Truths and a
Lie](https://icebreakerideas.com/two-truths-and-a-lie/), in which you come up
with three short statements about yourself, two of which are true and one of
which is a lie, and the group tries to figure out which one is the lie. This
works well remotely; doesn't force people to get more personal than they want
to, since the statements can be about anything (for example: I've brought down a
global e-commerce website and cost my employer about $10 million in lost sales);
and fits comfortably in half an hour for groups up to 7-8 people.

However, we used this icebreaker last time we had a team offsite three months
ago, and some of the people from my previous team will also be on the new team.
Worse yet, my so-called friend D.S. (you know who you are!) used this one in an
org-wide offsite just last week. The nerve!

So TTaaL is right out. On Friday, I was browsing through some icebreaker ideas,
and all of them failed to satisfy one or more criteria. This apparently had been
eating at my subconscious, and decided to surface right when I was trying to
concentrate on something else.

I started thinking about fun work-related things that I've done over the years,
and one particular exercise popped into my mind: [Architecture
Golf](https://engineering.klarna.com/architecture-golf-60fb51a6e787).
Architecture Golf is a group learning exercise created by my former colleagues
Túlio Ornelas and Kenneth Gibson, and works like this:
1. The team gathers around a whiteboard (physical or digital).
2. Someone states a process, practice, or system you want to learn about as a
   team.
3. One team member is randomly selected to go first.
4. That person draws the first stage of the process on the whiteboard and then
   passes the pen to the next person, who draws the next stage. If the person
   with the pen doesn't know what the next stage is, they simply take a guess.
   Not only is this OK, it's expected! Whenever something is drawn that doesn't
   match how the thing actually works, a discussion happens around why the thing
   works like it does, and should it in fact work differently?
5. Rinse and repeat until the diagram is complete.

According to Túlio:

> We call this exercise Architecture golf because it helped us explain the crazy
> architecture that we sometimes have. Each team member takes one swing at a
> time which pushes them closer to the solution, just like a team version of
> golf.

I encourage you to read the [full
article](https://engineering.klarna.com/architecture-golf-60fb51a6e787) and try
this game out with your team sometime!

As cool as this is, it didn't quite fit my needs because 30 minutes is a bit too
short for completing a session, and my team will own two systems, so I didn't
want to single one and potentially send the message that it's somehow more
important than the other system. However, the kernel of the idea is exactly what
I was looking for: collaborative drawing, one person at a time.

So if we can't draw a real system, how about a fake one? My brain suddenly did
that cool thing where it made a connection to some other thing, and I had the
answer! I remembered being very amused by
[FizzBuzzEnterpriseEdition](https://github.com/EnterpriseQualityCoding/FizzBuzzEnterpriseEdition)
a few years back, and thought it would be fun to collaboratively design an
incredibly complicated distributed system to solve an incredibly trivial
problem, such as FizzBuzz itself, reversing a string, sorting a list, etc.

My first thought was to call the game "Enterprise Improv", but one thing
bothered me a little bit about it. A few years back, I read a really great blog
post by Aurynn Shaw title [Contempt
Culture](https://blog.aurynn.com/2015/12/16-contempt-culture). It starts like
this:

> So when I started programming in 2001, it was du jour in the communities I
> participated in to be highly critical of other languages. Other languages
> sucked, the people using them were losers or stupid, if they would just use a
> real language, such as the one we used, everything would just be better.
>
> Right?

The point Aurynn makes in the post (which you really should read; it's
fantastic!) is that making fun of other languages or tools can result in people
who use those languages or tools feeling ridiculed or less than.

Even though FizzBuzzEnterpriseEdition is clearly tongue in cheek and intended to
be all in good fun, if you're a Java developer working at a large enterprise, I
could certainly understand if you feel like you're being made fun of.

So I tried to think of something fairly universal to software development,
something that all of us do from time to time. And lo! it came to me in a flash.

Over-engineering.

Which one of us has never added a layer or two of indirection because "we might
need to switch to a different database" or implemented a plugin system because
"then our users can write their own modules" or tried to use everything from the
[Design Patterns](https://www.goodreads.com/book/show/85009.Design_Patterns)
book in the same class because "those patterns are important for writing quality
software"?

So I decided the name of the game would be "Over-Engineering Improv". And here's
how you play:

## The rules

1. Gather around a whiteboard (physical or digital).
2. Choose a trivial problem
   ([FizzBuzz](https://leetcode.com/problems/fizz-buzz/), [reverse
   string](https://leetcode.com/problems/reverse-string/description/), [sort
   numbers](https://leetcode.com/problems/sort-list/), etc) with a known
   solution.
3. Write the solution to the problem down and post it next to the board (or
   anywhere it can easily be referred to).
4. Randomly choose a first player.
5. The first player puts the first part of the complicated system that will
   solve the problem on the board, explains what the part does (the first part
   will almost certainly produce the data that will be operated on), then passes
   the marker to the next player.
6. The next player says "[yes
   and...](https://en.wikipedia.org/wiki/Yes,_and...)" to the diagram by adding
   the next part of the system, connecting it to what's already on the board,
   and explaining what is happening. The object here is to make the system more
   complicated than what's currently on the board. Having done this, they pass
   the marker to the next player.
7. Repeat step 6 until there is about 5 minutes left in the game. Now the object
   becomes completing the solution within the remaining time. Continuing drawing
   and passing the marker.
8. When about a minute is left, declare the final turn. The player taking the
   final turn should complete the system to solve the problem.

If the problem is solved in the final turn, it's a win. If the problem isn't
solved, it's still a win because hopefully everyone had loads of fun, and will
be primed to beat the game next time!
