REST API Documentation :

* Secure your API with the `rest_api_password` setting. This password is shared across all bots. If the setting isn't set, it will be generated and saved in the config file.
* Request an access token with a `POST` to `/api/bot/{name}/auth`, the request body must be `rest_api_password`. This token must be placed in the `X-PGB-ACCESS-TOKEN` header or in a `token` parameter in each request URL for further use of the API.
* Pokemon end-point :
  - GET `/bot/{name}/pokemons` => List all pokemons
  - POST `/bot/{name}/pokemon/{id}/transfer` => Transfer pokemon
  - POST `/bot/{name}/pokemon/{id}/evolve` => Evolve pokemon
  - POST `/bot/{name}/pokemon/{id}/powerup` => Power-up pokemon
  - POST `/bot/{name}/pokemon/{id}/favorite` => Toggle favorite for this pokemon
  - POST `/bot/{name}/pokemon/{id}/rename` => Rename pokemon, request body MUST be the new name of the pokemon

* Item end-point :
  - GET `/bot/{name}/items` => List all items
  - DELETE `/bot/{name}/item/{id}/drop/{quantity}` => Drop `quantity` of this item
  - POST `/bot/{name}/useIncense` => Use an incense
  - POST `/bot/{name}/useLuckyEgg` => Use a lucky egg

* Misc end-points :
  - GET `/bot/{name}/location` => Get bot current location
  - POST `/bot/{name}/location/{latitude}/{longitude}` => Change bot location
  - GET `/bot/{name}/profile` => Get account profile
  - GET `/bot/{name}/pokedex` => Get account pokedex
  - GET `/bot/{name}/eggs` => Get eggs