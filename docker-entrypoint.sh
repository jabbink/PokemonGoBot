#!/bin/bash

root=/PokemonGoBot
config=$root/config.properties

for e in $(env); do
    case $e in
        POGO_*)
            k=$(echo ${e,,} | cut -d= -f1 | cut -c 6-)
            v=$(echo $e | cut -d= -f2-)
            p=""
            if [ $v == "#" ]; then
                v=""
                p="#"
            fi
            if grep -Fq "$k" $config; then
                sed -i "s/^$k.*/$p$k=$v/g" $config
            else
                echo  "$p$k=$v">>$config
            fi
    esac
done

version=$(cat build.gradle | grep -Po '[0-9]\.[0-9]\.[0-9](-alpha[0-9]*)?' | head -n 1)
java -jar $root/build/libs/pogo.scraper-all-${version}.jar
