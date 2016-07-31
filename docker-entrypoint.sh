#!/bin/bash

root=/PokemonGoBot
version=$(cat build.gradle | grep -Po '[0-9]\.[0-9]\.[0-9](-alpha[0-9]*)?' | head -n 1)

for e in $(env); do
    case $e in
        POGO_*)
            k=$(echo ${e,,} | cut -d= -f1 | cut -c 6-)
            v=$(echo $e | cut -d= -f2)
            sed -i "s/$k.*/$k=$v/g" $root/config.properties
    esac
done

java -jar $root/build/libs/pogo.scraper-all-${version}.jar
