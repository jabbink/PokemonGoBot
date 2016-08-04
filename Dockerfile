FROM anapsix/alpine-java:8_jdk
RUN apk add --no-cache git bash wget grep sed
ENV ROOT /PokemonGoBot
WORKDIR ${ROOT}
COPY . ${ROOT}
RUN git submodule update --init --recursive \
 && ./gradlew build \
 && ./gradlew fatJar \
 && mv config.properties.template config.properties
CMD ["./docker-entrypoint.sh"]
