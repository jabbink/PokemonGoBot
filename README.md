# Pokemon Go Bot

[![Build Status](https://travis-ci.org/jabbink/PokemonGoBot.svg?branch=develop)](https://travis-ci.org/jabbink/PokemonGoBot)

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
 

## Usage (from source)

1. Clone this repo: `git clone https://github.com/jabbink/PokemonGoBot.git && cd PokemonGoBot` or download the zip
2. Run `git submodule update --init --recursive`
3. Run from terminal/cmd: `gradlew build`
4. Rename `./config.properties.template` to `./config.properties`
5. Modify `config.properties` as you please
6. To run the bot directly from console run `gradlew run`
7. :exclamation: If you use JetBrains IntelliJ, install the Lombok plugin :exclamation:

## Usage (prebuilt)

1. Make sure you have Java 1.8 or higher installed (`java -version` in a command line)
    - If not, go [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
2. Download the latest release from [here](https://github.com/jabbink/PokemonGoBot/releases).
3. Download [config.properties.template](https://raw.githubusercontent.com/jabbink/PokemonGoBot/master/config.properties.template) and save it in the same directory
4. Rename `config.properties.template` to `config.properties` (make sure your operating system doesn't rename it to `config.properties.txt`)
5. Fill in the blanks
6. Open a terminal (or `cmd.exe` on Windows)
7. Use `cd` to go into the directory with your config and the downloaded `.jar`
8. `java -jar pogo.scraper-all-VERSION.jar` (replace version with the downloaded one, or type `pogo.scraper-all` and press `TAB`)

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

## After 20-30 minutes I get a LoginFailedException

Update to the latest version; it *should* be fixed.

## I get a RemoteServerException or something about "502"

The Pokemon Go servers are offline/too busy. Check IsPokemonGoDownOrNot.com

## The GUI only loads the command line.

Separate the windows as they're conjoined together.

