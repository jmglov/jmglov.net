1. Install [NixOS](https://nixos.org/) on your laptop because it’s super cool
   but also because you hate yourself just a little.
2. Come up with the idea of writing something every day this summer and
   publishing it on “your blog”.
3. Try and remember if you have a blog somewhere.
4. Try and remember what your password to that blog might have been. Or did you
   use your Google account to sign up?
5. Get lucky when you sign in with Google and you’re dropped into your profile
   where you have a single blog entry from several years ago and of course a few
   drafts because why not?
6. Start a new blog entry.
7. Try to figure out what in the world you’re going to write about.
8. Go on a walk with your dog and talk to your friend Ray on the phone (well,
   Signal audio call, because who uses their phone for actual phoning?) about
   your nascent blogging career.
9. Mention that you’re planning to do your blogging on your own site, using some
   static site generator or other. Wisely realise that this will be way more
   work that you think. Predict serious rabbit-holing to your friend.
10. Remember that the inimitable [borkdude](https://www.michielborkent.nl/)
    wrote a [blog
    entry](https://blog.michielborkent.nl/migrating-octopress-to-babashka.html)
    about how he’s generating his blog as a static site with
    [Babashka](https://github.com/babashka/babashka).
11. Clone [borkdude’s blog](https://github.com/borkdude/blog) from Github
    because of course the blog itself is open source because borkdude is
    awesome!
12. Add `pkgs.babashka` to your
    [`home.nix`](https://github.com/jmglov/nixos-config/blob/main/home.nix) and
    run `sudo nixos-rebuild switch` to install it.
13. Run `bb render` and get excited as Babashka downloads the
    [clj-kondo](https://github.com/clj-kondo/clj-kondo) pod.
14. Cry tears of sadness as Babashka errors out with:
    ```
    java.util.zip.ZipException: invalid entry CRC (expected 0x7463ff01 but got
    0x6bbe279e)
    ```
15. Realise that you’re about a million versions behind the most current
    Babashka.
16. Check
    [`babashka.nix`](https://github.com/NixOS/nixpkgs/blob/a153c90eec38d1a5694c3a8793f5732608b94e8f/pkgs/development/interpreters/clojure/babashka.nix)
    on the main branch of [nixpkgs](https://github.com/NixOS/nixpkgs) to see
    what the latest available version is.
17. Get excited when you see that someone has just upgraded to the latest
    version of Babashka last week.
18. Update your home.nix to pull Babashka (and just Babashka) from nixpkgs at
the commit where it was updated because you can do that in Nix and you knew that
Nix rules previously so that’s why you’re running NixOS like a boss! (Also,
because you hate yourself just a little.)
19. Run `bb render` again, triumphant in the knowledge that it’s going to work
    now that it’s on the latest version, because the latest version of any given
    piece of software has fixed all of the bugs.
20. Get a permission denied error when Babashka tries to execute clj-kondo.
21. `ls -l ~/.babashka/pods/repository/borkdude/clj-kondo/2021.10.19/clj-kondo`
22. See that in fact the execute bit is not set.
23. Surmise that this is because the clj-kondo pod failed to install due to the
    `ZipException`.
24. `rm -rf ~/.babashka/pods/repository/ && bb render`
25. Get the same bloody ZipException.
26. Try to remember your password to [Clojurians
    Slack](https://clojurians.slack.com/).
27. Install the desktop Slack client by adding pkgs.slack to your home.nix.
28. Revel in the glory of Nix.
29. Get a magic signin link from the Slack client to your email.
30. See that your friend Ray DM’d you back in 2019 to ask if you were going to
    [ClojuTre](https://clojutre.org/) and you totally ghosted him because you
    apparently hadn’t logged into Clojurians Slack since the spring of 2018.
31. See if there’s a #babashka channel on Clojurians Slack and breathe a sigh of
    relief when of course there is!
32. Post about your problem in #babashka.
33. Wait 42 seconds for borkdude himself (creator of Babashka and about a
zillion other awesome open source thingies, mostly Clojure-related) to answer
your question.
34. Provide borkdude with the version of zlib you’re using to validate his
    theory.
35. Watch as borkdude pings in some badass Nix expert to help and wait 42 more
    seconds before said badass arrives.
36. Provide the badass with a 14 line `bb.nix` that reproduces the problem on
    any Nix installation because that’s how Nix works.
37. Whilst the badass works on a proper fix (applying the Arch Linux patch where
    they fixed the zlib issue to the zlib derivation that the graalvm derivation
    depends on, obviously), attempt to create a Nix overlay that pins zlib to
    1.2.11, the last version that works with GraalVM.
38. Ask yourself WTF is happening when you keep getting infinite recursion when
    evaluating your new Babashka derivation that uses the overlay that pins
    zlib.
39. Find out that apparently you [can’t override zlib in an
    overlay](https://github.com/NixOS/nixpkgs/issues/61682) because
    `pkgs.stdlib` for Linux includes zlib.
40. Despair when borkdude points you to a Nix flake that you could probably hack
    up to work around the problem because you haven’t had the time to wrap your
    head around [Nix flakes](https://nixos.wiki/wiki/Flakes).
41. Walk your dog and talk to Ray on Signal and describe the rabbit hole and
    laugh when he says that your words from the previous chat were prophetic
    when you said that building your blog in Clojure would result in a rabbit
    hole.
42. Be proud of yourself for writing your daily blog entry **before** messing
    around with this stuff.
43. Make some oatmeal for yourself and your son.
44. Start to head back to your desk to keep hacking but then realise that the
    sun is shining so go outside and play football with your son instead.
45. [Optional step] Go out to supper with some good friends from the union and
    drink your troubles away.
46. Watch some Star Trek: The Next Generation even though your friend Sen hates
    on it all the time because she thinks TOS is better even though she’s wrong
    and is super mean about it when you try to convince her she’s wrong but
    that’s OK because she just got a dope NCC-1701 tattoo so all is forgiven and
    she’s actually the best anyway because she’s chair of the local union and a
    total badass.
47. Fall asleep on the couch for half an hour but then do the responsible thing
    and brush your teeth and take the dog out for a wee before getting in bed.
48. Sleep pretty damned well even though your rib is aching like wild because
    you broke it 7 years ago and then broke it again 6 years ago (and when you
    say you broke it, what you actually mean is that some wanker broke it for
    you during football training, and then a really nice guy from work who was
    going in fairly for a 50–50 challenge caught you in the same rib with his
    shoulder a year later and broke it for you again) and then some 20-something
    wanker from Brommapojkarna (they really need to change their name, the
    sexist bastards) performed a full-on hockey-style body check on you during a
    7-a-side football match last week and hit the damned rib but luckily it
    didn’t break this time but just bruised pretty badly.
49. Wake up and see that the Nix badass has opened a PR to nixpkgs that fixes
    your issue.
50. Responsibly write a blog entry on how to write a blog in Clojure in 50 easy
    steps before you try out the fix from the badass’s branch.

See, toldya it was easy!

![The Clojure logo](img/clojure-logo.png)

PS: I did write a blog entry and publish it yesterday, but it was a guest piece
for my friend Tim’s blog: [“Story of a mediocre
fan”](https://7amkickoff.com/index.php/2022/06/16/story-of-a-mediocre-fan/). I
guess next time I do that, I should at least link it here so I have a record of
having written something for posterity.
