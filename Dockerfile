FROM java
RUN apt-get update \
 && apt-get install -y git wget \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
ENV ROOT /PokemonGoBot
WORKDIR ${ROOT}
COPY . ${ROOT}
RUN git submodule update --init --recursive \
 && ./gradlew build \
 && ./gradlew fatJar \
 && mv config.properties.template config.properties
CMD ["./docker-entrypoint.sh"]
