package ink.abb.pogo.scraper

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId
import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Andrew Potter (ddcapotter)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Settings(
    val name: String,
    val startingLatitude: Double,
    val startingLongitude: Double,

    val credentials: Credentials,

    val speed: Double = 2.778,
    val shouldDropItems: Boolean = false,
    val preferredBall: ItemId = ItemId.ITEM_POKE_BALL,
    val shouldAutoTransfer: Boolean = false,
    val shouldDisplayKeepalive: Boolean = true,
    val transferCPThreshold: Int = 400,
    val ignoredPokemon: List<PokemonId> = listOf(PokemonId.EEVEE, PokemonId.MEWTWO, PokemonId.CHARMENDER),
    val obligatoryTransfer: List<PokemonId> = listOf(PokemonId.DODUO, PokemonId.RATTATA, PokemonId.CATERPIE, PokemonId.PIDGEY),

    val uselessItems: Map<ItemId, Int> = mapOf(
        Pair(ItemId.ITEM_REVIVE, 20),
        Pair(ItemId.ITEM_MAX_REVIVE, 10),
        Pair(ItemId.ITEM_POTION, 0),
        Pair(ItemId.ITEM_SUPER_POTION, 30),
        Pair(ItemId.ITEM_HYPER_POTION, 50),
        Pair(ItemId.ITEM_MAX_POTION, 50),
        Pair(ItemId.ITEM_POKE_BALL, 50),
        Pair(ItemId.ITEM_GREAT_BALL, 50),
        Pair(ItemId.ITEM_ULTRA_BALL, 50),
        Pair(ItemId.ITEM_MASTER_BALL, 10),
        Pair(ItemId.ITEM_RAZZ_BERRY, 50)
    )
) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = GoogleCredentials::class, name = "google"),
        JsonSubTypes.Type(value = PokemonTrainersClubCredentials::class, name = "PTC")
    )
    @JsonIgnoreProperties(ignoreUnknown = true)
    interface Credentials

    data class GoogleCredentials(var token: String) : Credentials
    data class PokemonTrainersClubCredentials(val username: String, val password: String, var token: String) : Credentials
}
