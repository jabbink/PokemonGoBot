# Frequently Asked Questions

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

## Where did the GPX support go?

If you like the old UI with GPX support, go to http://pogo.abb.ink/0.5.0-alpha4/map.html

If your bot runs the GUI socket on port `8001`, everything should work automatically, if not, append your IP address and port to the URL like this: http://pogo.abb.ink/0.5.0-alpha4/map.html#127.0.0.1:8001

## The bot refuses to login to my Google account

Make sure you enter the full email address (including `@`) in the `username=` property.

## Error: unable to access jarfile xxxxx

Make sure you are working in the directory where the JAR file is located

## I want to use multiple bots

Define the bot configurations in multiple JSON files in the `bot-settings` directory.

Refer to the file named [`json-template.json`](./json-template.json) for an example.

Make sure all JSON files have a different `guiPortSocket` or set that port to `0`!

## I get "Address already in use"

You are either trying to run multiple bots by running Java multiple times, multiple bot configurations use the same `guiPortSocket` or the port is simply in use by another application.

To change the socket ports, change `guiPortSocket` (`gui_port_socket` in `config.properties`) to another port, or disable the GUI by setting this to `0`.

Also the management API for multiple bots requires a port. By default `8080` is used. To change this, run the bot as `java -jar PokemonGoBot.jar --server-port=XXXX`. If you don't need that interface, run the bot as `java -jar PokemonGoBot.jar --spring.main.web-environment=false`!
