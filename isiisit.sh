#!/bin/bash
#
# Options set to improve ImageJ performance. See
# http://imagejdocu.tudor.lu/doku.php?id=howto:java:imagej_performance_tuning
#
#

#export DISPLAY=:1
#Xvfb $DISPLAY -auth /dev/null & (

time java -Xms32m -Xmx2000m -Xincgc -XX:+DisableExplicitGC -cp libs/ij.jar:dist/ISIISit.jar ISIISpreProcess.ISIISpreProcess $1 $2
# time java -Xms32m -Xmx2000m -Xincgc -XX:+DisableExplicitGC -cp dist/lib/ij.jar:dist/ISIISit.jar ISIISgetParticles.ISIISgetParticles $1 $2 $3

#wait
#)

#exit 0
