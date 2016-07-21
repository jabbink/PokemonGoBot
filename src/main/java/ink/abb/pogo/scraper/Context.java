package ink.abb.pogo.scraper;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import com.google.common.util.concurrent.AtomicDouble;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.player.PlayerProfile;
import okhttp3.OkHttpClient;

/**
 * Created by TimD on 7/21/2016.
 */
public class Context {
    private OkHttpClient http;
    private PokemonGo go;
    private AtomicDouble lat = new AtomicDouble();
    private AtomicDouble lng = new AtomicDouble();
    private PlayerProfile profile;
    private double speed;
    private boolean walking;
    private RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo;
    private MapObjects mapObjects;

    public Context(PokemonGo go, AtomicDouble lat, AtomicDouble lng, PlayerProfile profile, double speed, boolean walking, RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo, OkHttpClient http) {
        this.go = go;
        this.lat = lat;
        this.lng = lng;
        this.profile = profile;
        this.speed = speed;
        this.walking = walking;
        this.authInfo = authInfo;
        this.http = http;
    }

    public MapObjects getMapObjects() {
        return mapObjects;
    }

    public void setMapObjects(MapObjects mapObjects) {
        this.mapObjects = mapObjects;
    }

    public OkHttpClient getHttp() {
        return http;
    }

    public void setHttp(OkHttpClient http) {
        this.http = http;
    }

    public PokemonGo getApi() {
        return go;
    }

    public void setGo(PokemonGo go) {
        this.go = go;
    }

    public AtomicDouble getLat() {
        return lat;
    }

    public void setLat(AtomicDouble lat) {
        this.lat = lat;
    }

    public AtomicDouble getLng() {
        return lng;
    }

    public void setLng(AtomicDouble lng) {
        this.lng = lng;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean isWalking() {
        return walking;
    }

    public void setWalking(boolean walking) {
        this.walking = walking;
    }

    public RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo getAuthInfo() {
        return authInfo;
    }

    public void setAuthInfo(RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo) {
        this.authInfo = authInfo;
    }
}
