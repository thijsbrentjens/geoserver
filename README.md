Geoserver for TJS
=====================

Geoserver TJS repository.

The community plugin TJS will add support for Table Joining Services to Geoserver. Code is based on code of GeoCuba, code imported by Thijs Brentjens on request of GeoCuba. Minor changes on the Geoserver core code for the WMS and Catalog are required.

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

All the above features hev been developed by GeoCuba.

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

Related projects
===========
For demonstraton and test purposes a basic tool is developed. This tool allows users to upload a CSV-file and join that to a spatial framework (spatial dataset)
TODO: reference to the tool.

Installation
=====================
The TJS extension currently requires two (small) changes in the Geoserver core code of 2.2.4. 
Download the following jars and place them in geoserver/WEB-INF/lib/:

- dd
- dd

Currently, the plugin adds GeoJSON output. 

Then add the plugin jars, including 



There is a WAR-file available online which includes these changes and the plugin: 


Alternatively, there is a ZIP-file containing all these jars.

Compilation
=====================
Compilation 


