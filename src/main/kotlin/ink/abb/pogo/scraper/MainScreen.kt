package ink.abb.pogo.scraper

import Log
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import com.lynden.gmapsfx.GoogleMapView
import com.lynden.gmapsfx.MapComponentInitializedListener
import com.lynden.gmapsfx.javascript.`object`.*
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PtcLogin
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import okhttp3.OkHttpClient
import tornadofx.View
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.security.cert.CertificateException

class MainScreen : View(), MapComponentInitializedListener, Initializable {

    val mapView: GoogleMapView by fxid()
    val tfUsername: TextField by fxid()
    val tfXP: TextField by fxid()
    val tfLevel: TextField by fxid()
    val tfTeam: TextField by fxid()
    val tfPokecoin: TextField by fxid()
    val tfStardust: TextField by fxid()
    val taConsole: TextArea by fxid()
    val btnStart: Button by fxid()

    val tfSettingsUsername: TextField by fxid()
    val tfSettingsPassword: TextField by fxid()
    val tfSettingsToken: TextField by fxid()
    val tfSettingsLatitude: TextField by fxid()
    val tfSettingsLongitude: TextField by fxid()
    val tfSettingsSpeed: TextField by fxid()
    val tfSettingsTransferIVthreshold: TextField by fxid()
    val tfSettingsIgnoredPokemon: TextField by fxid()
    val tfSettingsObligatoryTransfer: TextField by fxid()
    //val choiceboxSettingsPreferedBall: ChoiceBox by fxid()
    val checkboxSettingsDropItems: CheckBox by fxid()
    val checkboxSettingsAutotransfer: CheckBox by fxid()


    var map: GoogleMap? = null

    var settings: Settings? = null

    fun allowProxy(builder: OkHttpClient.Builder) {
        builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8888)))
        val trustAllCerts = arrayOf<TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun getAcceptedIssuers(): Array<out X509Certificate> {
                return emptyArray()
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        builder.sslSocketFactory(sslSocketFactory)
        builder.hostnameVerifier { hostname, session -> true }
    }

    override fun mapInitialized() {

        //Set the initial properties of the map.
        val mapOptions = MapOptions()
        mapOptions.center(LatLong(settings!!.startingLatitude, settings!!.startingLongitude)).overviewMapControl(false).panControl(false).rotateControl(false).scaleControl(false).streetViewControl(false).zoomControl(false).zoom(12)
        map = mapView.createMap(mapOptions)

        //Add markers to the map
        val currentLocation = LatLong(settings!!.startingLatitude, settings!!.startingLongitude)
        val markerOptions1 = MarkerOptions()
        markerOptions1.position(currentLocation)
        val currentMarker = Marker(markerOptions1)
        map!!.addMarker(currentMarker)
//        val infoWindowOptions = InfoWindowOptions()
//        infoWindowOptions.content("<h2>Fred Wilkie</h2>"
//                + "Current Location: Safeway<br>"
//                + "ETA: 45 minutes")
//
//        val fredWilkeInfoWindow = InfoWindow(infoWindowOptions)
//        fredWilkeInfoWindow.open(map, fredWilkieMarker)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        mapView.addMapInializedListener(this)

        val properties = Properties()
        try {
            FileInputStream("config.properties").use {
                properties.load(it)
            }

            settings = Settings(properties)

            tfSettingsUsername.text = settings!!.username
            tfSettingsPassword.text = settings!!.password
            tfSettingsToken.text = settings!!.token
            tfSettingsLatitude.text = settings!!.startingLatitude.toString()
            tfSettingsLongitude.text = settings!!.startingLongitude.toString()
            tfSettingsSpeed.text = settings!!.speed.toString()
            tfSettingsTransferIVthreshold.text = settings!!.transferIVthreshold.toString()
            tfSettingsIgnoredPokemon.text = settings!!.ignoredPokemon.toString()
            tfSettingsObligatoryTransfer.text = settings!!.obligatoryTransfer.toString()
            checkboxSettingsDropItems.isSelected = settings!!.shouldDropItems
            checkboxSettingsAutotransfer.isSelected = settings!!.shouldAutoTransfer

        } catch (e: FileNotFoundException) {
            btnStart.isDisable = true;
        }

        // Close application properly
        apRoot.sceneProperty().addListener({ obs, oldScene, newScene ->
            Platform.runLater {
                val stage = newScene.window as Stage
                stage.setOnCloseRequest({ e ->
                    exit()
                })
            }
        })
    }

    override val root : AnchorPane by fxml("MainScreen.fxml");

    fun startBot() {
        val builder = OkHttpClient.Builder()
        // allowProxy(builder)
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        val http = builder.build()

        val username = settings!!.username
        val password = settings!!.password
        val token = settings!!.token

        //Log.normal(ctx, "Logging in to game server...")
        val auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo

        auth = if (token.isBlank()) {
            if (username.contains('@')) {
                GoogleLogin(http).login(username, password)
            } else {
                PtcLogin(http).login(username, password)
            }
        } else {
            if (token.contains("pokemon.com")) {
                PtcLogin(http).login(token)
            } else {
                GoogleLogin(http).login(token)
            }
        }

        tfUsername.text = username
        taConsole.appendText("Logged in with token ${auth.token.contents}\n")

        if (token.isBlank()) {
            taConsole.appendText("Set this token in your config to log in directly\n")
        }
        val api = PokemonGo(auth, http)

        taConsole.appendText("Getting profile data from pogo server\n")
        while (api.playerProfile == null) {
            print(".")
            Thread.sleep(1000)
        }
        println(".")

        Bot(api, settings!!, this).run()
    }

    fun saveSettings() {
        val properties = Properties()
        properties.setProperty("username", tfSettingsUsername.text)
        properties.setProperty("password", tfSettingsPassword.text)
        properties.setProperty("token", tfSettingsToken.text)
        properties.setProperty("latitude", tfSettingsLatitude.text)
        properties.setProperty("longitude", tfSettingsLongitude.text)
        properties.setProperty("speed", tfSettingsSpeed.text)
        properties.setProperty("transfer_iv_threshold", tfSettingsTransferIVthreshold.text)
        properties.setProperty("drop_items", checkboxSettingsDropItems.isSelected.toString())
        properties.setProperty("autotransfer", checkboxSettingsAutotransfer.isSelected.toString())
        properties.setProperty("ignored_pokemon", tfSettingsIgnoredPokemon.text)
        properties.setProperty("obligatory_transfer", tfSettingsObligatoryTransfer.text)
        FileOutputStream("config.properties").use {
            properties.store(it, null)
        }

        if (tfSettingsLatitude.text != "" && tfSettingsLongitude.text != "") {
            btnStart.isDisable = false
        }
    }

    fun exit() {
        System.exit(0)
    }

}