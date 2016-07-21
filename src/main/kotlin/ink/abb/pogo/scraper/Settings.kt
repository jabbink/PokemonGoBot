package ink.abb.pogo.scraper

/**
 * @author Andrew Potter (ddcapotter)
 */
import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import com.pokegoapi.api.inventory.Pokeball
import java.util.*

class Settings(val properties: Properties) {

    val pokeballItems = mapOf(Pair(ItemIdOuterClass.ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL))

    val uselessItems = mapOf(
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

    fun getLatitude(): Double {
        return properties.getProperty("latitude").toDouble()
    }

    fun getLongitude(): Double {
        return properties.getProperty("longitude").toDouble()
    }

    fun getUsername(): String {
        return properties.getProperty("username")
    }

    fun getPassword(): String {
        return String(Base64.getDecoder().decode(properties.getProperty("password")))
    }

    fun getSpeed(): Double {
        return getPropertyIfSet("Speed", "speed", 2.778, String::toDouble)
    }

    fun shouldDropItems(): Boolean {
        return getPropertyIfSet("Item Drop", "drop_items", false, String::toBoolean)
    }

    fun getPreferredBall(): ItemId {
        return getPropertyIfSet("Preferred Ball", "preferred_ball", ItemId.ITEM_POKE_BALL, ItemId::valueOf)
    }

    fun shouldAutoTransfer(): Boolean {
        return getPropertyIfSet("Autotransfer", "autotransfer", false, String::toBoolean)
    }

    private fun <T> getPropertyIfSet(description: String, property: String, default: T, conversion: (String) -> T): T {

        val settingString = "$description setting (\"$property\")"
        val defaulting = "defaulting to \"$default\""

        if(!properties.containsKey(property)) {
            println("$settingString not specified, $defaulting.")
            return default
        }

        try {
            return conversion(properties.getProperty(property))
        } catch (e: Exception) {
            println("$settingString is invalid, defaulting to $default: ${e.message}")
            return default
        }
    }
}
