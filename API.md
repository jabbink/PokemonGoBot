REST API Documentation :

* Secure your API with the `rest_api_password` setting. This password is shared across all bots. If the setting isn't set, it will be generated and saved in the config file.
* Request an access token with a `POST` to `/api/bot/{name}/auth`, the request body must be `rest_api_password`. This token must be placed in the `X-PGB-ACCESS-TOKEN` header for further use of the API.
* Bot end-point : 
  - POST `/api/bot{name}/load` => Load bot
  - POST `/api/bot{name}/unload` => Unload bot
  - POST `/api/bot{name}/reload` => Reload bot
  - POST `/api/bot{name}/stop` => Stop bot
  - POST `/api/bot{name}/start` => Start bot
* Pokemon end-point :
  - GET `/api/bot{name}/pokemons` => List all pokemons
  - POST `/api/bot{name}/pokemon/{id}/transfer` => Transfer pokemon
  - POST `/api/bot{name}/pokemon/{id}/evolve` => Evolve pokemon
  - POST `/api/bot{name}/pokemon/{id}/powerup` => Power-up pokemon
  - POST `/api/bot{name}/pokemon/{id}/favorite` => Toggle favorite for this pokemon
  - POST `/api/bot{name}/pokemon/{id}/rename` => Rename pokemon, request body MUST be the new name of the pokemon

* Item end-point :
  - GET `/api/bot{name}/items` => List all items
  - DELETE `/api/bot{name}/item/{id}/drop/{quantity}` => Drop `quantity` of this item
  - POST `/api/bot{name}/useIncense` => Use an incense
  - POST `/api/bot{name}/useLuckyEgg` => Use a lucky egg

* Misc end-points :
  - GET `/api/bot{name}/location` => Get bot current location
  - POST `/api/bot{name}/location/{latitude}/{longitude}` => Change bot location
  - GET `/api/bot{name}/profile` => Get account profile
  - GET `/api/bot{name}/pokedex` => Get account pokedex
  - GET `/api/bot{name}/eggs` => Get eggs