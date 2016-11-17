# Frequently Asked Questions

## I got `BUILD FAILED`

There is a lot of output with the reason WHY the build fails, please read it through carefully and fix any errors mentionned in the output.

## I get an `AsyncPokemonGoException: Unknown exception occurred` / `Error running loop ProfileLoop!` / `Error running loop BotLoop!`

Only that is not enough, check the whole stacktrace for the correct error.

## I get an `InvalidProtocolBufferException: Contents of buffer are null`

This is a known issue with the currently used Pokemon GO API. The bot sends too many requests in a too short time to the servers, which return with a null value. We hope to get that fixed shortly, but is not something you can simply fix with some configuration settings or by putting extra Thread.sleep() in the code.

## After login I see `Accepting ToS` and get a `RemoteServerException: Your account may be banned! please try from the official client.`

If you CAN login to the official client, complete the tutorial (catch your first pokemon, touch your first pokestop) and try again.

If you CAN'T login in the official client but see "Error fetching game data": RIP items/eggs/pokemons, your account got permabanned. You knew this was possible since you did read and violate the ToS of the official game.

## The bot doesn't find/catch Pokemon

Currently not reproducible so can't be reliably fixed. Look here for some people that managed to get it fixed: https://github.com/jabbink/PokemonGoBot/issues/21

Some possible issues:

 * Make sure your system time is (semi-)correct (let it autosync with an online server)
 * Make sure the mobile app is not on (kill the process if need be)
 * Make sure the account you're botting on did do the initial tutorial (mainly catching a starter Pokemon)
 * Make sure your item bank is empty
 * Make sure you have enough pokeballs and your pokebank is not full
 * You caught too many pokemons in a short time and are softbanned (don't catch more than 1000 Pokemon in 23 hours, [source](https://www.reddit.com/r/pokemongodev/comments/4xkqmq/new_ban_types_and_their_causes/)).

## The bot doesn't find/loot Pokestops

Some possible issues:

 * Your startlocation is wrong and/or there are no pokestops nearby.
 * You looted too many pokestops in a short time and are softbanned (don't loot more than 2000 Pokestops in 23 hours, [source](https://www.reddit.com/r/pokemongodev/comments/4xkqmq/new_ban_types_and_their_causes/))

## The bot doesn't walk

Some possible issues:

 * You're camping a Pokestop with a lure (should be visible in log)
 * You're resting (should be visible in log)

## Immediately after starting I get a LoginFailedException

- Make sure the provided credentials in the `config.properties` file are correct.
- If you're using PTC, your credentials are correct and your password is longer than 15 characters, only enter the first 15 characters of your account and the login should work.
- If there is a token present, remove it and retry.

## After 1.5 hours I get a errors with PTC/Google login `LoginFailedException: Invalid Auth status code recieved, token not refreshed?`

Known issue with the Java API handling login for PTC/Google.

## I get a RemoteServerException or something about "502"

The Pokemon Go servers are offline/too busy. Check IsPokemonGoDownOrNot.com

## The GUI only loads the command line.

Separate the windows as they're conjoined together.

## Where did the GUI go?

The GUI is now hosted on http://ui.pogobot.club/

This URL is also shown in the console when you launch the bot.

## Where did the GPX support go?

If you like the old UI with GPX support, go to http://ui.pogobot.club/0.5.0-alpha4/map.html

If your bot runs the GUI socket on port `8001`, everything should work automatically, if not, append your IP address and port to the URL like this: http://ui.pogobot.club/0.5.0-alpha4/map.html#127.0.0.1:8001

## The bot refuses to login to my Google account

Make sure you enter the full email address (including `@`) in the `username=` property. If you use a JSON configuration, make sure the credential type is set as `google-auto`.

## Error: unable to access jarfile xxxxx

Make sure you are working in the directory where the JAR file is located. If you JAR file is located in `C:\Users\userprofilename\Desktop\pogobot\` you can change your directory in the console with the command `cd C:\Users\userprofilename\Desktop\pogobot\` (do not simply copy/paste this command but alter it to your location first).

## I want to use multiple bots

Define the bot configurations in multiple JSON files in the `bot-settings` directory.

Refer to the file named [`json-template.json`](./json-template.json) for an example.

If you use a PTC account, set credential type to "ptc", if you use a Google account, set credential type to `google-auto`!

Make sure all JSON files have a different `guiPortSocket` or set that port to `0`!

## I get "Address already in use"

It is possible you did not close the bot correctly (do not close the console window, but do `CTRL+C` first to close the process) and it is still running in the background. Kill all running JAVA processes and restart the bot.

If that is not the case, you are either trying to run multiple bots by running Java multiple times, multiple bot configurations use the same `guiPortSocket` or the port is simply in use by another application.

To change the socket ports, change `guiPortSocket` (`gui_port_socket` in `config.properties`) to another port, or disable the GUI by setting this to `0`.

Also the management API for multiple bots requires a port. By default `8080` is used. To change this, run the bot as `java -jar PokemonGoBot.jar --server.port=XXXX`. If you don't need that interface, run the bot as `java -jar PokemonGoBot.jar --spring.main.web-environment=false`!

## I get JsonMappingException "Can not deserialize instance of java.util.ArrayList out of VALUE_FALSE token"

The followStreets settings changed from boolean to array.
You have to remove the "followStreets" line on your "default.json" file.
