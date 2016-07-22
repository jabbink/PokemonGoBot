class Log {

    companion object {

        private val green_ = "\u001B[32m"
        private val black_ = "\u001B[30m"
        private val yellow_ = "\u001B[33m"
        private val red_ = "\u001B[31m"
        private val white_ = "\u001B[37m"
        private val reset = "\u001B[0m"

        fun green(text: String) {
            println(green_ + text + reset)
        }

        fun normal(text: String) {
            println(text)
        }

        fun red(text: String) {
            println(red_ + text + reset)
        }

        fun yellow(text: String) {
            println(yellow_ + text + reset)
        }

        fun white(text: String) {
            println(white_ + text + reset)
        }

        fun black(text: String) {
            println(black_ + text + reset)
        }
    }
}