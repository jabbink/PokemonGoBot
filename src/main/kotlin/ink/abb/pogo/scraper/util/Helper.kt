
package ink.abb.pogo.scraper.util

import ink.abb.pogo.scraper.util.Log

class Helper {

	companion object {

		fun getRandomNumber(minnum: Int, maxnum: Int): Int {
			return (minnum + (Math.random() * ((maxnum - minnum) + 1))).toInt()
		}

		fun waitRandomSeconds(minnum: Int, maxnum: Int) {
			val seconds = getRandomNumber(minnum, maxnum)
			
			Log.green("Waiting for " + seconds + " seconds... ")
			Thread.sleep((seconds * 1000).toLong())
		}

	}
}