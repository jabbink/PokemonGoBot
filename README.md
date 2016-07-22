# Pokemon Go Bot

## Usage (from source)

1. Clone this repo
2. Copy `src/main/resources/config.properties.template` to `src/main/resources/config.properties`
3. Add your PTC username, password and starting location in `src/main/resources/config.properties`
4. Run `Main.kt`

## Usage (prebuilt)

1. Make sure you have Java 1.7 or higher installed (`java -version` in a command line)
1. Download the latest release from https://github.com/jabbink/PokemonGoBot/releases
2. Download https://raw.githubusercontent.com/jabbink/PokemonGoBot/master/config.properties.template and save it in the same directory
3. Rename `config.properties.template` to `config.properties` (make sure your operating system doesn't rename it to `config.properties.txt`)
4. Fill in the blanks
5. Open a terminal (or `cmd.exe` on Windows)
6. Use `cd` to go into the directory with your config and the downloaded `.jar`
7. `java -jar pogo.scraper-all-VERSION.jar` (replace version with the downloaded one, or type `pogo.scraper-all` and press `TAB`)

# Known issues

## The bot doesn't catch Pokemon

Currently not reproducible so can't be reliably fixed. Look here for some people that managed to get it fixed: https://github.com/jabbink/PokemonGoBot/issues/21

Some possible issues:

 * Make sure your system time is (semi-)correct (let it autosync with an online server)
 * Make sure the mobile app is not on (kill the process if need be)
 * Make sure the account you're botting on did do the initial tutorial (mainly catching a starter Pokemon)

## Immediately after starting I get a LoginFailedException

Make sure the provided credentials in the `config.properties` file are correct.

If you're using PTC, your credentials are correct and your password is longer than 15 characters, only enter the first 15 characters of your account and the login should work.

## After 20-30 minutes I get a LoginFailedException

Known issue in the used Java API; fix is being worked on.

## I get a RemoteServerException or something about "502"

The Pokemon Go servers are offline/too busy. Check IsPokemonGoDownOrNot.com

