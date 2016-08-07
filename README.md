# Pokemon Go Bot

[![Build Status](https://travis-ci.org/jabbink/PokemonGoBot.svg?branch=develop)](https://travis-ci.org/jabbink/PokemonGoBot)

## Welcome to _the_ Pokemon Go Bot

[Join our Discord](https://discord.gg/7Dr84MT)

FAQ for 0.5.0 (up here because people won't scroll)

[*Do you need multiple bots?* Define them in multiple JSON files in the `bot-settings` directory: https://github.com/jabbink/PokemonGoBot/issues/777](https://github.com/jabbink/PokemonGoBot/issues/777#issuecomment-237219032)

[java.lang.RuntimeException: java.lang.reflect.InvocationTargetException ... Address already in use?](https://github.com/jabbink/PokemonGoBot/issues/806#issuecomment-238089960)

### Screenshot

![GUI Screenshot](http://pogo.abb.ink/img/gui-screenshot-01.png)

## Features

* Login with Google and Pokemon Trainer Club
* Walk (while following roads (optional)) from Pokestop to Pokestop (highly configurable)
* Collect all Pokemon that are within reach
* Automatically drop useless items and bad Pokemon (100% configurable)
* Hatch eggs
* Export your Pokemon to many different export formats (to keep track in Excel/other external applications)

# Usage

## ***Please Read***:

- We're very happy to have you partake in this experience with us and even possibly contribute.
- However, due to an overwhelming amount of attention from the public this repository has seen an onslaught of attention.
- With that said, please do your due diligence and research your problem without opening unnecessary issue tickets.
- *Searching* here, reddit or Google will more than likely provide you with an answer.
    - Common issues may be found at the bottom of this page.
- Those that are actively contributing to this project utilize the ticket system for tracking technical issues and
having to answer the same question can really clog up the pipes for people who are presenting an original problem.
- For legitimate technical issues **PLEASE** abide by the given template and provide as much information as possible.
    - For extensive logs, please use PasteBin.

## Prebuilt

1. Make sure you have Oracle Java 1.8 or higher installed (`java -version` in a command line)
    - If not, go [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
2. Download the latest release from [here](https://github.com/jabbink/PokemonGoBot/releases).
3. Download [config.properties.template](https://raw.githubusercontent.com/jabbink/PokemonGoBot/master/config.properties.template) and save it in the same directory
4. Rename `config.properties.template` to `config.properties` (make sure your operating system doesn't rename it to `config.properties.txt`)
5. Fill in the blanks
6. Open a terminal (or `cmd.exe` on Windows)
7. Use `cd` to go into the directory with your config and the downloaded `.jar`
8. `java -jar PokemonGoBot-VERSION.jar` (replace version with the downloaded one, or type `PokemonGoBot-` and press `TAB`)

## From source

1. Clone this repo: `git clone https://github.com/jabbink/PokemonGoBot.git && cd PokemonGoBot` or download the zip
2. Run from terminal/cmd: `gradlew build`
3. Rename `./config.properties.template` to `./config.properties`
4. Modify `config.properties` as you please
5. To run the bot directly from console run `gradlew run`
6. :exclamation: If you use JetBrains IntelliJ, install the Lombok plugin and enable Settings -> Compiler -> Annotation Processors -> Enable annotation processing :exclamation:

# Contributing
If you want to help and add a new feature, you can create a pull request to merge in the `develop` branch and not in the `master`.
As the name says, the `develop` branch is for developing where we'll add new features, with your help; instead we'll update the `master` every now and then, and from that we'll release a new jar.

# Known issues

## I got `BUILD FAILED`
Most of the time it's because the you didn't execute step 2 of the README.

After that, just try `gradlew clean build`.

If there's still a problem, open an issue.

## The bot doesn't catch Pokemon

Currently not reproducible so can't be reliably fixed. Look here for some people that managed to get it fixed: https://github.com/jabbink/PokemonGoBot/issues/21

Some possible issues:

 * Make sure your system time is (semi-)correct (let it autosync with an online server)
 * Make sure the mobile app is not on (kill the process if need be)
 * Make sure the account you're botting on did do the initial tutorial (mainly catching a starter Pokemon)

## Immediately after starting I get a LoginFailedException

- Make sure the provided credentials in the `config.properties` file are correct.
- If you're using PTC, your credentials are correct and your password is longer than 15 characters, only enter the first 15 characters of your account and the login should work.
- If there is a token present, remove it and retry.

## After 1.5 hours I get a errors with Google login

Known issue with the Java API handling login for Google.

## I get a RemoteServerException or something about "502"

The Pokemon Go servers are offline/too busy. Check IsPokemonGoDownOrNot.com

## The GUI only loads the command line.

Separate the windows as they're conjoined together.

## Where did the GUI go?

The GUI is now hosted on http://pogo.abb.ink/RocketTheme/

This URL is also shown in the console when you launch the bot.

## The bot refuses to login to my Google account

Make sure you enter the full emailaddress (including `@`) in the `username=` property.

## Error: unable to access jarfile xxxxx

Make sure you are working in the directory where the JAR file is located

## CreateProcess error=206, The filename or extension is too long

Known Windows issue, build the bot from a directory close to rootdir eg. `C:\pogobot\`

## FileNotFoundException: config.properties (No such file or directory)

Make sure windows didn't add `.txt` to the filename.
