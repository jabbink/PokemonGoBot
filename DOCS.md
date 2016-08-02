# GUI Creation

Please be encouraged to create your own GUI and share it with others!


## Creation


### Initialization

You must add this script to your <head> element in order to communicate with the server
    <script src="https://cdn.socket.io/socket.io-1.4.5.js"></script>

Upon page ready, create the socket connection using the following:
    var socket = io.connect('ws://HOSTNAME:PORT');
HOSTNAME is typically localhost, should be user-configurable
PORT is usually 8001


### Callbacks

The following callbacks are avaliable:

#### Profile data

    socket.on('profile', function(data){})
Returns 10 items, as follows:
* items - number of items in inventory sack
* itemsMax - Maximum items inventory sack can hold
* level - current level
* levelRatio - percentage to next level
* levelXp - XP at current time
* pokebank - number of Pokémon in the Pokébank
* pokebankMax - maximum Pokémon the Pokébank can hold
* stardust - amount of stardust collected
* team - NEUTRAL, BLUE, YELLOW, and RED currently **subject to change**
* username - username signed in as

#### Pokébank

    socket.on('pokebank', function(data){})
Contains a pokemon array, holding all of your pokemon in array format.
Each child of the pokemon array represents 1 Pokémon in your Pokébank, and contains:
* cp - Combat Power of the Pokémon
* id - unique identifier of Pokémon
* iv - IV percentage from 0-100
* name - name of Pokémon **not nickname**
* pokemonId - Pokédex number of Pokémon
* stats - String formatted to show specific IVs in the following format: "Stamina: 7 | Attack: 1 | Defense: 15 | IV: 23 (51%)"

#### Eggs

**UNDER DEVELOPMENT**

    socket.on('eggs', function(data){})
Returns the eggs in your inventory and currently only tells distanceTarget.
Returns as a nested array

#### Movement

    socket.on('newLocation', function(data){})
Sends a message each time the player is moved, contains a lat and a lng float for the latitude and longitude of the player

#### Pokéstop in range

    socket.on('pokestop', function(data){})
Contains an unique pokestop *id*, *name* of the Pokéstop, and a *lat* and *lng* for the pokéstop

#### newPokemon and releasePokemon

don't appear to be used.  \*shrugs\*

#### Raw log

    socket.on('log', function(data){})
There are 2 objects returned, text and type.  Text is the raw message displayed in the log, and type is the color of the message
