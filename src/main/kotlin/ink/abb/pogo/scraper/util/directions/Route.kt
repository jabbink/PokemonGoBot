package ink.abb.pogo.scraper.util.directions


import com.pokegoapi.google.common.geometry.S1Angle
import com.pokegoapi.google.common.geometry.S2LatLng
import java.util.*

class Route(var coordinateList: ArrayList<S2LatLng>) {

    var acceptableDistance = 30
    /**
     * Returns list of coordinates between the i-th and i-th+1 coordinates,
     * if their distance is too long.
     * @param index: index of the first coordinate
     */
    fun additionalSteps(index: Int, speed: Double, sec: Int): ArrayList<S2LatLng> {
        if (index < (coordinateList.size - 1)) {
            val distance = coordinateList[index].getEarthDistance(coordinateList[index + 1])
            if (distance > acceptableDistance) {
                val dx = coordinateList[index + 1].latRadians() - coordinateList[index].latRadians()
                val dy = coordinateList[index + 1].lngRadians() - coordinateList[index].lngRadians()
                val steps = Math.floor(distance / (speed * sec)).toInt()
                val stepX = dx / steps
                val stepY = dy / steps
                val addsteps = ArrayList<S2LatLng>()
                for (i in 1..steps - 2) {
                    addsteps.add(S2LatLng(S1Angle.radians(coordinateList[index].latRadians() + stepX * i), S1Angle.radians(coordinateList[index].lngRadians() + stepY * i)))
                }
                return addsteps
            } else {
                return ArrayList<S2LatLng>()
            }
        } else {
            return ArrayList<S2LatLng>()
        }
    }
}
