# Usage

## ***Please Read***:

- We're very happy to have you partake in this experience with us and even possibly contribute.
- However, due to an overwhelming amount of attention from the public this repository has seen an onslaught of attention.
- With that said, please do your due diligence and research your problem without opening unnecessary issue tickets.
- *Searching* here, reddit or Google will more than likely provide you with an answer.
    - Common issues may be found at the bottom of this page.
- Those that are actively contributing to this project utilize the ticket system for tracking technical issues and
having to answer the same question can really clog up the pipes for people who are presenting an original problem.
- For legitimate technical issues **PLEASE** abide by the given template and provide as much information as possible.
    - For extensive logs, please use PasteBin.

## Prebuilt

1. Make sure you have Oracle Java 1.8 JRE (JDK works too) installed (`java -version` in a command line)
    - If not, go [here](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
2. Download the latest release from [here](https://github.com/jabbink/PokemonGoBot/releases).
3. Download [config.properties.template](https://raw.githubusercontent.com/jabbink/PokemonGoBot/master/config.properties.template) and save it in the same directory
4. Rename `config.properties.template` to `config.properties` (make sure your operating system doesn't rename it to `config.properties.txt`)
5. Fill in the blanks
6. Open a terminal (or `cmd.exe` on Windows)
7. Use `cd` to go into the directory with your config and the downloaded `.jar`
8. `java -jar PokemonGoBot-VERSION.jar` (replace version with the downloaded one, or type `PokemonGoBot-` and press `TAB`)

## From source

1. Make sure you have Oracle Java JDK 1.8 installed (`java -version` in a command line)
    - If not, go [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
2. Clone this repo: `git clone https://github.com/jabbink/PokemonGoBot.git && cd PokemonGoBot` or download the zip
3. Run from terminal/cmd: `gradlew build`
4. Copy `./config.properties.template` to `./config.properties`
5. Modify `config.properties` as you please
6. To run the bot directly from console run `gradlew run`
7. :exclamation: If you use JetBrains IntelliJ, install the Lombok plugin and enable Settings -> Compiler -> Annotation Processors -> Enable annotation processing :exclamation:

## Stopping the bot

Do not simply close the console window, but press `CTRL+C` first to terminate the bot gracefully. Otherwise there's a chance the process will still run in the background and give an `address in use` error when you want to restart it.