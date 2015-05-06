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

Using Eclipse
=====================
To use the TJS plugin in Eclipse, follow the general guidelines of GeoServer but don't import the projects in Eclipse yet, http://docs.geoserver.org/2.6.x/en/developer/quickstart/index.html

Before importing the projects, you need to configure the TJS plugin for Eclipse as well:

To add the TJS plugin:

1. with a command line, setup the Eclipse project for TJS:

cd src/community/tjs/

mvn eclipse:eclipse

2. Now import the project Geoserver from file, as described http://docs.geoserver.org/2.6.x/en/developer/quickstart/index.html. You should see 

3. Run configuration:
After the first Run configuration with Eclipse, the TJS plugin needs to be added manually to the Run configuration. You can do this by going to Run > Run configurations...
There, add the TJS-projects to the Classpath, as projects:

* net.opengis.tjs
* tjs
* tjsdata
* tjs-web
* xsd-tjs

4. Running Geoserver with TJS should work now. NOTE: at this moment, there seems to be an issue using Eclipse's Run functionality, especially with the data dir configuration for TJS.





