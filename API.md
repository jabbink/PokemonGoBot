# REST API Documentation

Control your bot externally via the REST API.

## Bot configuration

Secure your API by putting a secure (randomized) password in the `rest_api_password` setting in your config.properties file or the `restApiPassword` setting in your JSON file(s).

Each bot has its own password, so use the correct one in the next step (or if you are lazy, use the same value everywhere).

If the configuration key is not set, it will be generated and saved in the config file.

## Authentication

Request an access token with a `POST` to `http://localhost:8080/api/bot/{name}/auth` where `{name}` is the name of your bot (= `default` by default) and the raw body must be the value from the `rest_api_password` setting from the first step.

Mind the default server port, which is 8080 (to change this, run the bot as `java -jar PokemonGoBot.jar --server-port=XXXX`) and do NOT use the socket port defined in your bot configuration (which is by default 8001).

You will get a response from the server with a random session token which must be placed in the `X-PGB-ACCESS-TOKEN` header for further use of the API.

If the authentication fails, you will get a HTTP status code 401 (Unauthorized). Make sure the configured password is correct, and your client sends a correct raw message (ex `curl -d` appends the raw body with `=` which fails the auth).

## REST API Endpoints

* General end-point:
  - GET `/api/bots` => List all bots (this does not need the `X-PGB-ACCESS-TOKEN` header)

* Bot end-point:
  - POST `/api/bot/{name}/load` => Load bot
  - POST `/api/bot/{name}/unload` => Unload bot
  - POST `/api/bot/{name}/reload` => Reload bot
  - POST `/api/bot/{name}/stop` => Stop bot
  - POST `/api/bot/{name}/start` => Start bot

* Pokemon end-point:
  - GET `/api/bot/{name}/pokemons` => List all pokemons
  - POST `/api/bot/{name}/pokemon/{id}/transfer` => Transfer pokemon
  - POST `/api/bot/{name}/pokemon/{id}/evolve` => Evolve pokemon (HTTP status code 400 (Bad Request) when not enough candy)
  - POST `/api/bot/{name}/pokemon/{id}/powerup` => Power-up pokemon (HTTP status code 400 (Bad Request) when not enough candy or stardust)
  - POST `/api/bot/{name}/pokemon/{id}/favorite` => Toggle favorite for this pokemon
  - POST `/api/bot/{name}/pokemon/{id}/rename` => Rename pokemon, request body MUST be the new name of the pokemon

* Item end-point:
  - GET `/api/bot/{name}/items` => List all items
  - DELETE `/api/bot/{name}/item/{id}/drop/{quantity}` => Drop `quantity` of this item (HTTP status code 400 (Bad Request) when invalid quantity)
  - POST `/api/bot/{name}/useIncense` => Use an incense (HTTP status code 400 (Bad Request) when not enough incense)
  - POST `/api/bot/{name}/useLuckyEgg` => Use a lucky egg (HTTP status code 400 (Bad Request) when not enough lucky eggs)

* Misc end-points:
  - GET `/api/bot/{name}/location` => Get bot current location
  - POST `/api/bot/{name}/location/{latitude}/{longitude}` => Change bot location (HTTP status code 400 (Bad Request) on invalid `latitude` or `longitude`)
  - GET `/api/bot/{name}/profile` => Get account profile
  - GET `/api/bot/{name}/pokedex` => Get account pokedex
  - GET `/api/bot/{name}/eggs` => Get eggs

## Examples

* A very simple [proof of concept with javascript](https://gist.github.com/Sieberkev/0f96f190615cebf15a07ca2a8a2a61ca) can be found here.
