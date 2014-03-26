Geoserver for TJS
=====================
Geoserver TJS repository.

The community plugin TJS will add support for Table Joining Services to Geoserver. Code is based on code of GeoCuba/GeoMix, imported by Thijs Brentjens on request of GeoCuba/GeoMix. Minor changes on the Geoserver core code for the WMS and Catalog are required.

This extension is under development. Please read this carefully. Any feedback is welcome.

Features
========
The TJS extension adds TJS support to Geoserver (2.2.4). It adds TJS operations to join data and adds web administration options. The web admin allows for management / configuration of:
- spatial frameworks (spatial datasets to join data on) and non-spatial tabular data to be published in GDAS encoding
- pre-configured joins.

The TJS OGC webservice interface supports almost all TJS operations. It allows for example for:
- GetCapabilities
- getting data in GDAS encoding
- creating joins using JoinData
- output of joins as WMS layers, with GetfeatureInfo is supported. The TJS extension has (some) support for SLD.

All the above features hev been developed by GeoCuba/GeoMix.

In another project, Geonovum has extended the above functionality with extra output mechanism to download a geoJSON file of the join. This is still work in progress.

Work in Progress
==========
The TJS extension is still under development. It has not been tested extensively yet. Testers on different systems / platforms are welcomed. Please contact Thijs with any questions: 

t dot brentjens at geonovum dot nl.

Among things to do are:

- support for WFS output mechanism. This would enable all kinds of downloads as well.
- improve stability of the configuration and restarting. On some systems there have been issues with reloading 
- support for download of other output formats, like shapefiles and KML
- support for management of joined output files and removing joins / clearing the cache after a pre-configured time
- access restrictions on operations (TODO: test if Geoserver securtiy system works properly already)
- user manual
- fix tests for the postgres connection of data stores

Related projects
===========
For demonstraton and test purposes a basic tool is developed. This tool allows users to upload a CSV-file and join that to a spatial framework (spatial dataset)

See https://github.com/ojajoh/tjs-demonstrator for the demonstrator

Installation
=====================
The TJS extension currently requires two (small) changes in the Geoserver core code of 2.2.4. 
To install, you could either add the built libraries or compile the code yourself.

Download the following ZIP-file with jars and place them in geoserver/WEB-INF/lib/:

https://github.com/thijsbrentjens/geoserver/blob/tjs_2.2.x/binaries/libs-20131219.zip

=======

Currently, the plugin adds GeoJSON output. The build should include the required libraries. There is a WAR-file available online which includes these changes and the plugin. This needs testing as well.

Restart the geoserver application after adding the jars.

Compilation
=====================
Follow the general build instructions of Geoserver.
To build the TJS plugin seperately, go to the directory 

src/community/tjs/

and build using the regular command:

mvn clean install -DskipTests

Note: tests will probably fail at the moment because of Postgres connections in some of the tests. Skipping the tests will help for now.
