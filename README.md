# Pokemon Go Bot

[![Build Status](https://travis-ci.org/jabbink/PokemonGoBot.svg?branch=develop)](https://travis-ci.org/jabbink/PokemonGoBot)

# Usage

## From Source

1. Clone this repo: `git clone https://github.com/jabbink/PokemonGoBot.git && cd PokemonGoBot` or download the zip
2. Run `git submodule update --init --recursive`
3. Run from terminal/cmd::`gradlew build -x check`
4. Follow the instructions below for adding a bot configuration
5. To run the bot directly from console run `gradlew run`

## Building a Jar

1. Make sure you have Java 1.8 or higher installed (`java -version` in a command line)
2. Download the latest release from https://github.com/jabbink/PokemonGoBot/releases
3. Follow the instructions below for adding a bot configuration
4. Open a terminal (or `cmd.exe` on Windows)
5. Use `cd` to go into the directory with your config and the downloaded `.jar`
6. `java -jar pogo.scraper-all-VERSION.jar` (replace version with the downloaded one, or type `pogo.scraper-all` and press `TAB`)
# Adding a Bot Configuration

Bot configuration files are JSON files stored in `./bot-settings`, and each bot instance has its own file.  These files contain many configuration values, but most have defaults and the app populate them for you.

To get started, make the `bot-settings` directory and copy the following JSON templates (filling in the appropriate values) into `bot-settings/my-bot-name.json`.
Once you've authenticated, your settings file will be updated with a token that is used to re-authenticate for a period of time.

### Initial settings with Google login
```json
{
  "name" : "my-bot-name",
  "startingLatitude" : xx.xxxxxxxxxxxxxxx,
  "startingLongitude" : xx.xxxxxxxxxxxxxx,
  "credentials" : {
    "type" : "google"
  }
}
```

### Initial settings with Pokemon Trainer's Club login
```json
{
  "name" : "my-bot-name",
  "startingLatitude" : xx.xxxxxxxxxxxxxxx,
  "startingLongitude" : xx.xxxxxxxxxxxxxx,
  "credentials" : {
    "type" : "PTC",
    "username": "my-username",
    "password": "my-password"
  }
}
```

# Contributing
If you want to help and add a new feature, you can create a pull request to merge in the `develop` branch and not in the `master`.  
As the name says, the `develop` branch is for developing where we'll add new features, with your help; instead we'll update the `master` every now and then, and from that we'll release a new jar.

# Known issues

## I got `BUILD FAILED`
Most of the time it's because the api the bot is built upon, [PokeGOAPI-Java](https://github.com/Grover-c13/PokeGOAPI-Java), updated and changed something.  
Run `gradlew --refresh-dependencies clean build`.  
If there's still a problem, open an issue.

## The bot doesn't catch Pokemon

Currently not reproducible so can't be reliably fixed. Look here for some people that managed to get it fixed: https://github.com/jabbink/PokemonGoBot/issues/21

Some possible issues:

 * Make sure your system time is (semi-)correct (let it autosync with an online server)
 * Make sure the mobile app is not on (kill the process if need be)
 * Make sure the account you're botting on did do the initial tutorial (mainly catching a starter Pokemon)

## Immediately after starting I get a LoginFailedException

Make sure the provided credentials are correct.

If you're using PTC, your credentials are correct and your password is longer than 15 characters, only enter the first 15 characters of your account and the login should work.

## After 20-30 minutes I get a LoginFailedException

Known issue in the used Java API; fix is being worked on.

## I get a RemoteServerException or something about "502"

The Pokemon Go servers are offline/too busy. Check IsPokemonGoDownOrNot.com
