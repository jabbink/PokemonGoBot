package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.pokegoapi.api.inventory.Pokeball

/**
 * @author Andrew Potter (ddcapotter)
 */
data class Settings(
    val name: String,
    val startingLatitude: Double,
    val startingLongitude: Double,

    val credentials: Credentials,

    val speed: Double,
    val shouldDropItems: Boolean,
    val preferredBall: ItemId,
    val shouldAutoTransfer: Boolean,
    val shouldDisplayKeepalive: Boolean,
    val transferCPThreshold: Int,
    val ignoredPokemon: List<String>,
    val obligatoryTransfer: List<String>,

    val pokeballItems: Map<ItemId, Pokeball> = mapOf(Pair(ItemIdOuterClass.ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
        Pair(ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
        Pair(ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
        Pair(ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL)),

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
    interface Credentials

    data class GoogleCredentials(var token: String) : Credentials
    data class PokemonTrainersClubCredentials(val username: String, val password: String, var token: String) : Credentials
}
