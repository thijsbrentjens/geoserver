Geoserver for TJS
=====================
Geoserver TJS repository.

The community plugin TJS will add support for Table Joining Services to Geoserver. Code is based on code of GeoCuba/GeoMix, imported by Thijs Brentjens on request of GeoCuba/GeoMix. 

This extension is under development. Please read this carefully. Any feedback is welcome.

Features
========
The TJS extension adds TJS support to Geoserver (2.6.x at the moment). It adds TJS operations to join data and adds web administration options. The web admin allows for management / configuration of:
- spatial frameworks (spatial datasets to join data on) and non-spatial tabular data to be published in GDAS encoding
- pre-configured joins.

The TJS OGC webservice interface supports almost all TJS operations. It allows for example for:
- GetCapabilities
- getting data in GDAS encoding
- creating joins using JoinData
- output of joins as WMS layers, WFS featuretypes. The TJS extension has (some) support for SLD, but this is untested currently

Te above features have initially been developed by GeoCuba/GeoMix. Geonovum has added WFS support and has rewritten parts of the source code.

Work in Progress
==========
The TJS extension is still under development. It has not been tested extensively yet. Testers on different systems / platforms are welcomed. Please contact Thijs with any questions: 

t dot brentjens at geonovum dot nl.

Among things to do are:

- improve stability of the configuration and restarting. On some systems there have been issues with reloading the config.
- support for management of joined output files and removing joins / clearing the cache after a pre-configured time
- access restrictions on operations (TODO: test if Geoserver securtiy system works properly already)
- user manual
- fix tests for the postgres connection of data stores

Related projects
===========
For demonstraton and test purposes a basic tool is developed. This tool allows users to upload a CSV-file and join that to a spatial framework (spatial dataset)

See https://github.com/joostvenema/tjs-demonstrator for the demonstrator

Installation
=====================
The extra libraries from the TJS extension can be build from the community/tjs/ directory, see Compilation below.

Compilation
=====================
Follow the general build instructions of Geoserver.
To build the TJS plugin seperately, go to the directory 

src/community/tjs/

and build using the regular command:

mvn clean install -DskipTests

Note: tests will probably fail at the moment because of Postgres connections in some of the tests. Skipping the tests will help for now.
