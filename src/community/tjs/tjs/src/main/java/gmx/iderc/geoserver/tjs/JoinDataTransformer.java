/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.JoinedMapInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.impl.DataStoreInfoImpl;
import gmx.iderc.geoserver.tjs.catalog.impl.DatasetInfoImpl;
import gmx.iderc.geoserver.tjs.catalog.impl.JoinedMapInfoImpl;
import gmx.iderc.geoserver.tjs.catalog.impl.TJSCatalogImpl;
import gmx.iderc.geoserver.tjs.data.TJS_1_0_0_DataStore;
import gmx.iderc.geoserver.tjs.data.TJS_WebMapServer;
import gmx.iderc.geoserver.tjs.data.TJSStore;
import gmx.iderc.geoserver.tjs.data.gdas.GDAS_DatasetInfo;
import gmx.iderc.geoserver.tjs.data.jdbc.hsql.HSQLDB_GDAS_Cache;
import gmx.iderc.geoserver.tjs.data.xml.ClassToXSDMapper;

import net.opengis.tjs10.*;
import org.apache.log4j.lf5.util.StreamUtils;
import org.apache.wicket.util.file.Files;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;


import org.geotools.data.*;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.ows.v1_1.OWS;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.geotools.sld.SLDConfiguration;
import org.geotools.sld.v1_1.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.tjs.TJS;
import org.geotools.tjs.TJSConfiguration;
import org.geotools.util.NullProgressListener;
import org.geotools.xlink.XLINK;
import org.geotools.xml.StreamingParser;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;

import org.geotools.feature.simple.SimpleFeatureImpl.*;

import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.capability.FunctionName;
import com.vividsolutions.jts.geom.Geometry;
// import org.opengis.geometry.Geometry;
// import org.vfny.geoserver.global.GeoserverDataDirectory;

import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job
 * of encoding a WFS 1.0 Capabilities document.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Chris Holmes
 * @author Justin Deoliveira
 * @version $Id: CapabilitiesTransformer.java 16404 2011-10-06 18:36:00Z jdeolive $
 */
public abstract class JoinDataTransformer extends TransformerBase {

    /**
     * logger
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(JoinDataTransformer.class.getPackage().getName());
    /**
     * identifer of a http get + post request
     */
    private static final String HTTP_GET = "Get";
    private static final String HTTP_POST = "Post";
    /**
     * wfs namespace
     */
    protected static final String TJS_PREFIX = "tjs";
    protected static final String TJS_URI = "http://www.opengis.net/tjs";
    /**
     * xml schema namespace + prefix
     */
    protected static final String XSI_PREFIX = "xsi";
    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    /**
     * filter namesapce + prefix
     */
    protected static final String OGC_PREFIX = "ogc";
    protected static final String OGC_URI = OGC.NAMESPACE;
    /**
     * wfs service
     */
    protected TJSInfo tjs;
    /**
     * catalog
     */
    protected TJSCatalog catalog;

    /**
     * Creates a new CapabilitiesTransformer object.
     */
    public JoinDataTransformer(TJSInfo tjs, TJSCatalog catalog) {
        super();
        setNamespaceDeclarationEnabled(false);

        this.tjs = tjs;
        this.catalog = catalog;
    }

    Set<FunctionName> getAvailableFunctionNames() {
        //Sort them up for easier visual inspection
        SortedSet sortedFunctions = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {
                String n1 = ((FunctionName) o1).getName();
                String n2 = ((FunctionName) o2).getName();

                return n1.toLowerCase().compareTo(n2.toLowerCase());
            }
        });

        Set<FunctionFactory> factories = CommonFactoryFinder.getFunctionFactories(null);
        for (FunctionFactory factory : factories) {
            sortedFunctions.addAll(factory.getFunctionNames());
        }

        return sortedFunctions;
    }

    /**
     * Transformer for wfs 1.0 capabilities document.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static class TJS1_0 extends JoinDataTransformer {

        public TJS1_0(TJSInfo tjs, TJSCatalog catalog) {
            super(tjs, catalog);
        }

        public Translator createTranslator(ContentHandler handler) {
            return new JoinDataTranslator(handler);
        }

        class JoinDataTranslator extends TranslatorSupport {

            //            private static final String GML_3_1_1_FORMAT = "text/xml; subtype=gml/3.1.1";
            JoinDataType request;

            protected String getBaseURL() {
                try {
                    Request owsRequest = ((ThreadLocal<Request>) Dispatcher.REQUEST).get();
                    if (owsRequest != null){
                        return owsRequest.getHttpRequest().getRequestURL().toString();
                    }else{
                        //ocurre cuando se realizan los test  y en el AutoJoin
                        return tjs.getTjsServerBaseURL();
                    }
                } catch (Exception ex) {
                    return tjs.getTjsServerBaseURL();
                }
            }

            public JoinDataTranslator(ContentHandler handler) {
                super(handler, null, null);
            }

            AttributesImpl attributes(String[] nameValues) {
                AttributesImpl atts = new AttributesImpl();

                for (int i = 0; i < nameValues.length; i += 2) {
                    String name = nameValues[i];
                    String valu = nameValues[i + 1];

                    atts.addAttribute(null, null, name, null, valu);
                }

                return atts;
            }

            Catalog geoserverCatalog;
            HashMap<String, WMSStoreInfo> layerStoreMap = new HashMap<String, WMSStoreInfo>();

            private Catalog getGeoserverCatalog() {
                if (geoserverCatalog == null) {
                    geoserverCatalog = tjs.getGeoServer().getCatalog();
                }
                return geoserverCatalog;
            }

            private WorkspaceInfo createTempWorkspace() {
                WorkspaceInfo workspaceInfo = getGeoserverCatalog().getWorkspaceByName(TJSExtension.TJS_TEMP_WORKSPACE);
                if (workspaceInfo == null) {
                    NamespaceInfo namespaceInfo = getGeoserverCatalog().getNamespaceByPrefix(TJSExtension.TJS_TEMP_WORKSPACE);
                    if (namespaceInfo == null) {
                        namespaceInfo = new NamespaceInfoImpl();
                        namespaceInfo.setPrefix(TJSExtension.TJS_TEMP_WORKSPACE);
                        namespaceInfo.setURI("http://www.iderc.co.cu/geomix/tjs/temp");
                        getGeoserverCatalog().add(namespaceInfo);
                    }
                    workspaceInfo = new WorkspaceInfoImpl();
                    workspaceInfo.setName(TJSExtension.TJS_TEMP_WORKSPACE);
                    getGeoserverCatalog().add(workspaceInfo);
                }
                return workspaceInfo;
            }

            public void setUpGetDataURL() throws IOException {
                URL url = new URL(request.getAttributeData().getGetDataURL());
                System.out.println("Loading GDAS from: " + url.toString());
                InputStream is = url.openStream();
                //is = copy(is);
                if (is != null) {
                    TJSConfiguration tjsConfiguration = new TJSConfiguration();
                    try {
                        StreamingParser parser = new StreamingParser(tjsConfiguration, is, TJS.GDAS);
                        GDASType gdas = (GDASType) parser.parse();
                        if (gdas == null) {
                            return;
                        }

                        GDAS_DatasetInfo gdas_datasetInfo = new GDAS_DatasetInfo(gdas, catalog, request.getAttributeData().getGetDataURL());
                        if (request.getMapStyling() != null){
                            handleMapStyling(gdas_datasetInfo, request.getMapStyling());
                        }

                        String frameworkURI = gdas.getFramework().getFrameworkURI();
                        FrameworkInfo frameworkInfo = catalog.getFrameworkByUri(frameworkURI);
                        if (frameworkInfo == null) {
                            throw new TJSException("This version only support hosted framework's URI");
                        }
                        handleFramework(frameworkInfo);

                        start("JoinedOutputs");

                        //setUpWMSMechanism(frameworkInfo, gdas.getFramework().getDataset().getDatasetURI());
                        // Thijs: TODO: enable again, now just for testing
                        setUpWMSMechanism(frameworkInfo, gdas_datasetInfo);
                        
                        // Thijs: TODO: create a WFS mechanism here. For output in other formats. See the setupWMSMechanism as example.

                        // Thijs: for now we only have a shape and JSON mechanism
                        setUpFileMechanism(frameworkInfo, gdas_datasetInfo);


                        end("JoinedOutputs");

                        makeJoinedMapByGetDataURL(request.getAttributeData().getGetDataURL(),
                                                         gdas.getFramework().getFrameworkURI(),
                                                         gdas.getFramework().getDataset().getDatasetURI());

                    } catch (ParserConfigurationException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage());
                    } catch (SAXException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage());
                    }
                }
            }

            private void handleMapStyling(DatasetInfo datasetInfo, MapStylingType mapStyling) {
                if (mapStyling.getStylingURL() != null){
                    URL url = null;
                    try {
                        url = new URL(mapStyling.getStylingURL());
                        // System.out.println("Loading Style from: " + url.toString());
                        LOGGER.log(Level.INFO, "Loading Style from: " + url.toString());
                        InputStream is = url.openStream();

                        //hago una copia del SLD y cierro la conexión
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        StreamUtils.copy(is, out);
                        out.close();
                        is.close();

                        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                        //valido que el SLD esté correcto para no trabajar por gusto
                        SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());
                        sldParser.setInput(in);
                        StyledLayerDescriptor styledLayerDescriptor = sldParser.parseSLD();

                        StyleInfo newStyleInfo = getGeoserverCatalog().getFactory().createStyle();
                        String styleName = "GDASStyle"+String.valueOf(System.currentTimeMillis());
                        newStyleInfo.setName(styleName);
                        String styleFileName = newStyleInfo.getName() + ".sld";
                        newStyleInfo.setFilename(styleFileName);
                        getGeoserverCatalog().add(newStyleInfo);

                        //hago persistente el estilo en el catálogo para que se pueda trabajar con él
                        in.reset();
                        getGeoserverCatalog().getResourcePool().writeStyle(newStyleInfo, in);
                        in.close();

                        datasetInfo.setDefaultStyle(styleName);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }

            JoinedMapInfo makeJoinedMapByGetDataURL(String getDataURL, String frameworkURI, String datasetURI) {
                //supongo que si llegamos aquí es porque el mapa se creó correctamente, Alvaro Javier
                //así que vamos a guardar la información correspondiente que permita manipular este mapa
                //en un futuro, sobre to.do reconstruirlo
                JoinedMapInfo joinedMap;

                final List<JoinedMapInfo> joinedMapsByGetDataURL = catalog.getJoinedMapsByGetDataURL(getDataURL);
                if (joinedMapsByGetDataURL != null && joinedMapsByGetDataURL.size() > 0) {
                    joinedMap = joinedMapsByGetDataURL.get(0);
                } else {
                    joinedMap = new JoinedMapInfoImpl(catalog);
                }
                //joinedMap.setGetDataURL(request.getAttributeData().getGetDataURL());
                joinedMap.setGetDataURL(getDataURL);
                joinedMap.setFrameworkURI(frameworkURI); // ?
                joinedMap.setCreationTime(System.currentTimeMillis());
                joinedMap.setServerURL(getServerURL());
                joinedMap.setDatasetUri(datasetURI);
                //joinedMap.setLifeTime(60*60*1000);//una hora
                catalog.save(joinedMap);
                return joinedMap;
            }

            void handleShapeMechanism() {
                start(TJS.Mechanism.getLocalPart());
                element(TJS.Identifier.getLocalPart(), "shapefile");
                element(TJS.Title.getLocalPart(), "ESRI shapefile");
                element(TJS.Abstract.getLocalPart(), "ESRI shapefile with spatial dataset");
                element("Reference", "ESRI shapefile with spatial dataset");
                end(TJS.Mechanism.getLocalPart());
            }

            void handleJSONMechanism() {
                start(TJS.Mechanism.getLocalPart());
                element(TJS.Identifier.getLocalPart(), "GeoJSON");
                element(TJS.Title.getLocalPart(), "GeoJSON file");
                element(TJS.Abstract.getLocalPart(), "GeoJSON file in original CRS with spatial dataset");
                element("Reference", "GeoJSON spec");
                end(TJS.Mechanism.getLocalPart());
            }

            void handleWMSMechanism() {
                start(TJS.Mechanism.getLocalPart());
                element(TJS.Identifier.getLocalPart(), "WMS");
                element(TJS.Title.getLocalPart(), "WMS Server v1.1.1");
                element(TJS.Abstract.getLocalPart(), "The OpenGIS® Web Map Service Interface Standard (WMS) provides a simple HTTP interface for requesting geo-registered map images from one or more distributed geospatial databases. A WMS request defines the geographic layer(s) and area of interest to be processed. The response to the request is one or more geo-registered map images (returned as JPEG, PNG, etc) that can be displayed in a browser application. The interface also supports the ability to specify whether the returned images should be transparent so that layers from multiple servers can be combined or no");
                element("Reference", "http://schemas.opengis.net/wms/1.1.1/");
                end(TJS.Mechanism.getLocalPart());
            }

            // Thijs: setup a WFS
            void handleWFSMechanism() {
                System.out.println("Start WFS mechanism in the JoinData output.. ");
                start(TJS.Mechanism.getLocalPart());
                element(TJS.Identifier.getLocalPart(), "WFS");
                element(TJS.Title.getLocalPart(), "WFS Server v2.0");
                element(TJS.Abstract.getLocalPart(), "TODO");
                element("Reference", "http://schemas.opengis.net/wfs/2.0/");
                end(TJS.Mechanism.getLocalPart());
            }

            // Thijs: WORK IN PROGRESS
            // This could be rewritten to a shapefile output
            private void setUpWFSMechanism(FrameworkInfo frameworkInfo, DatasetInfo datasetInfo) throws IOException {
                 // TODO: implement, think of stores etc
            }

            private void setUpFileMechanism(FrameworkInfo frameworkInfo, DatasetInfo datasetInfo) throws IOException {

                try {
                    WorkspaceInfo tempWorkspaceInfo = createTempWorkspace();
                    CatalogBuilder builder = new CatalogBuilder(getGeoserverCatalog());

                    // TODO: get Geoserver temp directory? Or add it to the www dir?
                    File datadir = GeoserverDataDirectory.accessor().findDataRoot();
                    // TODO: configurable
                    String wwwPath = "/www/tjsoutput/";
                    String tjsOutputDatadir = datadir.getPath() + wwwPath;

                    File testFile = new File(tjsOutputDatadir+"dummy.txt");
                    // now create any missing directory
                    testFile.getParentFile().mkdirs() ;

                    TJS_1_0_0_DataStore tjs100DataStore = createTJSDataStore(frameworkInfo);
                    TJSStore tempTJSStore = new TJSStore(tjs100DataStore,getGeoserverCatalog());
                    // TODO: how is the workspace set? In TJSSStore this method seems to do nothing
                    tempTJSStore.setWorkspace(tempWorkspaceInfo);

                    String newFeatureTypeName = datasetInfo.getName();
                    catalog.add(datasetInfo);

                    // TODO: do we need this TJSStore to be added to the GeoserverCatalog?
                    // or is this done already?
                    // getGeoserverCatalog().add(tempTJSStore);
                    // getGeoserverCatalog().add(tempTJSStore);

                    //tengo en tempWmsStore el almacen con el servidor de TJS.
                    //Falta publicar la capa del datasetInfo
                    // TODO: get the featuretypes from the TJS catalog
                    List<FeatureTypeInfo> featureTypes = getGeoserverCatalog().getResourcesByStore(tempTJSStore, FeatureTypeInfo.class);
                    FeatureTypeInfo featureTypeInfo = getFeatureTypeInfoIfExists(featureTypes, datasetInfo.getName());
                    // if (featureTypeInfo == null) {
                    builder.setWorkspace(tempWorkspaceInfo);
                    builder.setStore(tempTJSStore);
                    // TODO: how to deal with the joined data? Create new features in some datastore? Copy data?
                    SimpleFeatureType joinedType = ((TJSStore)tempTJSStore).getStore().getSchema(newFeatureTypeName);

                    CoordinateReferenceSystem dataCRS =  joinedType.getCoordinateReferenceSystem(); // schema.getCoordinateReferenceSystem();
                    // System.out.println("dataCRS: " + dataCRS.getName().toString());

                    // try to get the target feature type (might have slightly different
                    // name and structure)

                    // TODO: just create a file first, Then maybe later try to create a datastore properly
                    //
                    // featureTypeInfo.getAttributes();
                    try {
                        FeatureReader reader = tjs100DataStore.getFeatureReader(joinedType.getName().getLocalPart());
                        // FeatureReader reader = fsShape.reader();
                        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
                        SimpleFeatureCollection collection = new ListFeatureCollection(joinedType, features);
                        System.out.println("Start building collection ");

                        while (reader.hasNext()) {
                            try {
                                SimpleFeature feature = (SimpleFeature)reader.next();
                                features.add(feature);
                            }  catch (Exception ex) {
                                Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.WARNING, "Error reading one of the features: " + ex.getMessage());
                            }
                        }
                        reader.close();
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "Collection built.");
                        // System.out.println("Number of objects in collection: " + collection.size());

                        // System.out.println("Output TJS datadir: " + tjsOutputDatadir);
                        String shpFileName = tjsOutputDatadir + newFeatureTypeName+".shp";
                        File newFile = new File(shpFileName);

                        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

                        Map<String, Serializable> params = new HashMap<String, Serializable>();
                        params.put("url", newFile.toURI().toURL());
                        params.put("create spatial index", Boolean.TRUE);

                        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
                        newDataStore.createSchema(joinedType);
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "Shapefile datastore created");

                        // FeatureWriter<SimpleFeatureType, SimpleFeature> writer = null;

                        Transaction transaction = new DefaultTransaction("create");

                        String typeName = newDataStore.getTypeNames()[0];
                        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

                        // SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "Start writing to shapefile");
                        if (featureSource instanceof SimpleFeatureStore) {
                            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                            /*
                             * SimpleFeatureStore has a method to add features from a
                             * SimpleFeatureCollection object, so we use the ListFeatureCollection
                             * class to wrap our list of features.
                             */
                            featureStore.setTransaction(transaction);
                            try {
                                featureStore.addFeatures(collection);
                                transaction.commit();
                            } catch (Exception problem) {
                                problem.printStackTrace();
                                transaction.rollback();
                            } finally {
                                transaction.close();
                            }
                        } else {
                            System.out.println(typeName + " does not support read/write access");
                            // TODO: do not create the output mechanism
                        }
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "End creation of shapefile");
                        // end writing to shapefile

                        // Try GeoJSON reprojected

                        // Reproject to EPSG:84
                        // List<SimpleFeature> features4326 = new ArrayList<SimpleFeature>();

                        /*
                        CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:4326");
                        boolean lenient = true; // allow for some error due to different datums
                        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);

                        while (collection.features().hasNext()) {
                            SimpleFeature sf4326 = ((SimpleFeature)collection.features().next());
                            Geometry geometry = (Geometry) sf4326.getDefaultGeometry();
                            Geometry geometry2 = JTS.transform(geometry, transform);
                            sf4326.setDefaultGeometry(geometry2);
                            features4326.add(sf4326);
                        }
                        SimpleFeatureCollection collection4326 = new ListFeatureCollection(joinedType, features4326);
                        */

                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "Start JSON output");
                        // TODO: Document, this means an extra dependency from gt-geojson, which is currently not available in geoserver
                        // Have added this dependency in pom.xml
                        // TODO: document, include gt-geojson 8.6 in geoserver build
                        // TODO: determine decimals for RD and other projs? If 4326, then more decimals are required
                        int decimals = 3;
                        // GeometryJSON gjson = new GeometryJSON(decimals);

                        GeometryJSON gjson = new GeometryJSON(decimals);
                        FeatureJSON fjson = new FeatureJSON(gjson);

                        StringWriter writer = new StringWriter();
                        // TODO: write CRS information
                        // fjson.writeCRS(dataCRS, writer);
                        // fjson.setEncodeFeatureCollectionCRS(true);
                        // fjson.setEncodeFeatureCollectionBounds(true);

                        // looping over the features
                        /* */
                        /* while (collection.features().hasNext() ){
                              fjson.writeFeature((SimpleFeature)(collection.features().next()), writer);
                              // writer.write("\\n");
                        }
                        */
                        // shorter notation: all on one line
                        fjson.writeFeatureCollection(collection, writer);

                        String json = writer.toString();

                        File jsonFile = new File(tjsOutputDatadir+newFeatureTypeName+".json");
                        FileWriter jsonFileWriter = new FileWriter(jsonFile);
                        jsonFileWriter.write(json);
                        jsonFileWriter.close();
                        writer.close();


                    } catch (Exception ex) {
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.SEVERE, "Error writing features to a file: " + ex.getMessage());
                        throw ex;
                    }

                    // handleShapeMechanism();
                    handleJSONMechanism();
                    start("Output");
                    start("Resource");
                    // TODO: determine outputdir better ?
                    // getBaseURL().replace("/tjs/","/");
                    String outputLocation = getServerURL().concat(wwwPath);

                    element("URL", outputLocation + newFeatureTypeName + ".json");

                    end("Resource");
                    end("Output");

                } catch (Exception ex) {
                    Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.SEVERE, "Something failed in the file output: " + ex.getMessage());
                }
            }

            protected void clone(WMSStoreInfo source, WMSStoreInfo target) {
                target.setDescription(source.getDescription());
                target.setEnabled(source.isEnabled());
                target.setName(source.getName());
                target.setType(source.getType());
                target.setCapabilitiesURL(source.getCapabilitiesURL());
                target.setWorkspace(source.getWorkspace());
                target.setUsername(source.getUsername());
                target.setPassword(source.getPassword());
                target.setUseConnectionPooling(source.isUseConnectionPooling());
                target.setMaxConnections(source.getMaxConnections());
                target.setConnectTimeout(source.getConnectTimeout());
                target.setReadTimeout(source.getReadTimeout());
            }

            private boolean equalWMSStore(WMSStoreInfo s1, WMSStoreInfo s2) {
                try {
                    WebMapServer wms = s1.getWebMapServer(new NullProgressListener());
                    WebMapServer wms2 = s2.getWebMapServer(new NullProgressListener());
                    return wms.equals(wms2);
                } catch (IOException ex) {
                    return false;
                }
            }

            public void setUpGetDataXML() throws IOException {
                GetDataXMLType getDataXML = request.getAttributeData().getGetDataXML();
                String frameworkURI = getDataXML.getFrameworkURI();
                FrameworkInfo frameworkInfo = catalog.getFrameworkByUri(frameworkURI);
                if (frameworkInfo == null) {
                    throw new TJSException("This version only support hosted framework's URI");
                }
                handleFramework(frameworkInfo);

                start("JoinedOutputs");
                setUpWMSMechanism(frameworkInfo, getDataXML.getDatasetURI());
                end("JoinedOutputs");
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = (JoinDataType) object;

                AttributesImpl attributes = attributes(new String[]{
                                                                           "version", "1.0",
                                                                           "lang", "es",
                                                                           "service", "TJS",
                                                                           "capabilities", "http://sis.agr.gc.ca/pls/meta/tjs_1x0_getcapabilities",
                                                                           "xmlns:xsi", XSI_URI,
                                                                           "xmlns", TJS_URI,
                                                                           "xmlns:ows", OWS.NAMESPACE, //"xmlns:gml", GML.NAMESPACE,
                                                                           "xmlns:ogc", OGC.NAMESPACE, "xmlns:xlink", XLINK.NAMESPACE,
                                                                           "xsi:schemaLocation", TJS.NAMESPACE + " "
                                                                                                         + "http://schemas.opengis.net/tjs/1.0/tjsDescribeDatasets_response.xsd"
                });

                List<NamespaceInfo> namespaces = catalog.getNamespaces();
                for (NamespaceInfo namespace : namespaces) {
                    String prefix = namespace.getPrefix();
                    String uri = namespace.getURI();

                    //ignore xml prefix
                    if ("xml".equals(prefix)) {
                        continue;
                    }

                    String prefixDef = "xmlns:" + prefix;

                    attributes.addAttribute("", prefixDef, prefixDef, "", uri);
                }

                start(TJS.JoinDataResponse.getLocalPart(), attributes);
                attributes = attributes(new String[]{
                                                            "xlink:href", "http://www.iderc.co.cu/geomix/tjs",
                                                            "creationTime", (new Date().toString())});
                start("Status", attributes);
                start("completed");
                end("completed");
                end("Status");

                //Grandes cambios
                if (request.getAttributeData().getGetDataURL() != null) {
                    try {
                        setUpGetDataURL();
                    } catch (IOException ex) {
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.SEVERE, ex.getMessage());
                    }
                } else if (request.getAttributeData().getGetDataXML() != null) {
                    try {
                        setUpGetDataXML();
                    } catch (IOException ex) {
                        Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.SEVERE, ex.getMessage());
                    }
                }
                end(TJS.JoinDataResponse.getLocalPart());


            }

            void handleFrameworkKey(AttributeTypeInfo frameworkKey) {
                if (frameworkKey == null) {
                    return;
                }
                start(TJS.FrameworkKey.getLocalPart());
                //   <Column name="ecozone" type="http://www.w3.org/TR/xmlschema-2/#integer" length="2" decimals="0" />
                AttributesImpl attributes = attributes(new String[]{
                                                                           "name", frameworkKey.getName(),
                                                                           "type", ClassToXSDMapper.map(frameworkKey.getBinding()),
                                                                           "length", String.valueOf(frameworkKey.getLength()),
                                                                           "decimals", "0"});
                element("Column", "", attributes);
                end(TJS.FrameworkKey.getLocalPart());
            }

            void handleBoundingCoordinates(ReferencedEnvelope envelope) {
                if (envelope == null) {
                    return;
                }
                start(TJS.BoundingCoordinates.getLocalPart());
                element("North", String.valueOf(envelope.getMaxY()));
                element("South", String.valueOf(envelope.getMinY()));
                element("East", String.valueOf(envelope.getMaxX()));
                element("West", String.valueOf(envelope.getMinX()));
                end(TJS.BoundingCoordinates.getLocalPart());
            }

            void handleFramework(FrameworkInfo framework) {
                start(TJS.Framework.getLocalPart());
                element(TJS.FrameworkURI.getLocalPart(), framework.getUri());
                element(TJS.Organization.getLocalPart(), framework.getOrganization());
                element(TJS.Title.getLocalPart(), framework.getName());
                element(TJS.Abstract.getLocalPart(), framework.getDescription());
                if (framework.getRefererenceDate() != null) {
                    element(TJS.ReferenceDate.getLocalPart(), framework.getRefererenceDate().toString());
                }
                element(TJS.Version.getLocalPart(), String.valueOf(framework.getVersion()));
                element(TJS.Documentation.getLocalPart(), framework.getDocumentation());
                handleFrameworkKey(framework.getFrameworkKey());
                handleBoundingCoordinates(framework.getBoundingCoordinates());
                element(TJS.DescribeDatasetsRequest.getLocalPart(), getBaseURL() + "?request=DescribeDatasets&Service=TJS&Version=1.0.0&FrameworkURI=" + framework.getUri());
                end(TJS.Framework.getLocalPart());
            }

            private String replaceIgnoreCase(String base, String what, String newContent){
                int index = base.toUpperCase().indexOf(what.toUpperCase());
                if (index >= 0){
                    String before = base.substring(0, index);
                    String after = base.substring(index+what.length(), base.length());

                    return before+newContent+after;
                }
                return base;
            }

            private String getLocalWMSUrl(String workswpace) {
                String baseURL = getBaseURL();
                String wms;
                if (baseURL.toUpperCase().endsWith("OWS")){
                    wms = replaceIgnoreCase(baseURL,  "OWS", workswpace + "/wms");
                }else{
                    if (baseURL.toUpperCase().endsWith("TJS")){
                        wms = replaceIgnoreCase(baseURL,  "TJS", workswpace + "/wms");
                    }else{
                        wms = getBaseURL().concat("/"+workswpace + "/wms");
                    }
                }
                return wms + "?request=GetCapabilities&service=WMS";
            }

            private String getTempWMSUrl(WorkspaceInfo workspaceInfo) {
                String baseURL = getBaseURL();
                String wms;
                if (baseURL.toUpperCase().endsWith("OWS")){
                    wms = replaceIgnoreCase(baseURL,  "OWS", TJSExtension.TJS_TEMP_WORKSPACE + "/wms");
                }else{
                    if (baseURL.toUpperCase().endsWith("TJS")){
                        wms = replaceIgnoreCase(baseURL,  "TJS", TJSExtension.TJS_TEMP_WORKSPACE + "/wms");
                    }else{
                        wms = getBaseURL().concat("/"+TJSExtension.TJS_TEMP_WORKSPACE + "/wms");
                    }
                }
                return wms + "?request=GetCapabilities&service=WMS";
            }

            // Thijs: TODO: refactor for WFS and WMS
            private String getTempWFSUrl(WorkspaceInfo workspaceInfo) {
                String baseURL = getBaseURL();
                Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "BaseURL for the service: " + baseURL);
                String wfs;
                if (baseURL.toUpperCase().endsWith("OWS")){
                    wfs = replaceIgnoreCase(baseURL,  "OWS", TJSExtension.TJS_TEMP_WORKSPACE + "/wfs");
                }else{
                    if (baseURL.toUpperCase().endsWith("TJS")){
                        wfs = replaceIgnoreCase(baseURL,  "TJS", TJSExtension.TJS_TEMP_WORKSPACE + "/wfs");
                    }else{
                        wfs = getBaseURL().concat("/"+TJSExtension.TJS_TEMP_WORKSPACE + "/wfs");
                    }
                }
                return wfs + "?request=GetCapabilities&service=WFS";
            }

            private WebMapServer createWebMapServer(FrameworkInfo frameworkInfo) {
                try {
                    LayerInfo layer = frameworkInfo.getAssociatedWMS();
                    //esto me paece que es una fuente de error!, Alvaro Javier Fuentes Suarez
                    //String prefixedLayerName = layer.getResource().getPrefixedName();
                    //se hace así, Alvaro Javier Fuentes Suarez
                    String layerName = layer.getName();
                    String prefix = layer.getResource().getNamespace().getPrefix();

                    CatalogBuilder builder = new CatalogBuilder(getGeoserverCatalog());
                    URL wmsServerUrl = new URL(getLocalWMSUrl(prefix));
                    //WebMapServer wms = null;
                    //Si no existe el store en el catalogo lo creo
                    WMSStoreInfo wmsStoreInfo = getGeoserverCatalog().getStoreByName(prefix, WMSStoreInfo.class);
                    if (wmsStoreInfo == null) {
                        wmsStoreInfo = builder.buildWMSStore(prefix);
                        wmsStoreInfo.setCapabilitiesURL(wmsServerUrl.toString());
                        wmsStoreInfo.setWorkspace(createTempWorkspace());
                        getGeoserverCatalog().add(wmsStoreInfo);
                    }
                    builder.setStore(wmsStoreInfo);
                    //no usar el prefixed!, Alvaro Javier Fuentes Suarez
                    //WMSLayerInfo wmsLayerInfo = builder.buildWMSLayer(prefixedLayerName);
                    //use reste!, Alvaro Javier Fuentes Suarez
                    WMSLayerInfo wmsLayerInfo = builder.buildWMSLayer(layerName);
                    WMSLayerInfo exists = getGeoserverCatalog().getResourceByStore(wmsStoreInfo, frameworkInfo.getAssociatedWMS().getName(), WMSLayerInfo.class);
                    if (exists != null) {
                        builder.updateWMSLayer(exists, wmsLayerInfo);
                    } else {
                        LayerInfo layerInfo = builder.buildLayer(wmsLayerInfo);
                        getGeoserverCatalog().add(wmsLayerInfo);
                        getGeoserverCatalog().add(layerInfo);
                    }
                    return wmsStoreInfo.getWebMapServer(new NullProgressListener());
                } catch (MalformedURLException ex) {

                } catch (IOException ex) {

                }
                return null;
            }

            private TJS_1_0_0_DataStore createTJSDataStore(FrameworkInfo frameworkInfo) {
                try {
                    Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE,"Creating TJS Data store for WFS");
                    // LayerInfo layer = frameworkInfo.getAssociatedWMS();
                    FeatureTypeInfo featureTypeInfo = frameworkInfo.getFeatureType();
                    CatalogBuilder builder = new CatalogBuilder(getGeoserverCatalog());

                    // we have a WFS Datastore already  for the featuretype
                    // assume it is a wfsDataStore?
                    DataStore featureDataStore = (DataStore) featureTypeInfo.getStore().getDataStore(null);
                    Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "Datastore: " + featureDataStore.toString());

                    //esto me paece que es una fuente de error!, Alvaro Javier Fuentes Suarez
                    //String prefixedLayerName = layer.getResource().getPrefixedName();
                    //se hace así, Alvaro Javier Fuentes Suarez
                   /* String layerName = layer.getName();
                    String prefix = layer.getResource().getNamespace().getPrefix();

                    URL wmsServerUrl = new URL(getLocalWMSUrl(prefix));
                    //WebMapServer wms = null;
                    //Si no existe el store en el catalogo lo creo
                    WMSStoreInfo wmsStoreInfo = getGeoserverCatalog().getStoreByName(prefix, WMSStoreInfo.class);
                    if (wmsStoreInfo == null) {
                        wmsStoreInfo = builder.buildWMSStore(prefix);
                        wmsStoreInfo.setCapabilitiesURL(wmsServerUrl.toString());
                        wmsStoreInfo.setWorkspace(createTempWorkspace());
                        getGeoserverCatalog().add(wmsStoreInfo);
                    }
                    builder.setStore(wmsStoreInfo);
                    //no usar el prefixed!, Alvaro Javier Fuentes Suarez
                    //WMSLayerInfo wmsLayerInfo = builder.buildWMSLayer(prefixedLayerName);
                    //use reste!, Alvaro Javier Fuentes Suarez
                    WMSLayerInfo wmsLayerInfo = builder.buildWMSLayer(layerName);
                    WMSLayerInfo exists = getGeoserverCatalog().getResourceByStore(wmsStoreInfo, frameworkInfo.getAssociatedWMS().getName(), WMSLayerInfo.class);
                    if (exists != null) {
                        builder.updateWMSLayer(exists, wmsLayerInfo);
                    } else {
                        LayerInfo layerInfo = builder.buildLayer(wmsLayerInfo);
                        getGeoserverCatalog().add(wmsLayerInfo);
                        getGeoserverCatalog().add(layerInfo);
                    }
                     */
                    // should be a TJS Catalog here...
                    TJSCatalog tjsCatalog = TJSExtension.getTJSCatalog();
                    Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.FINE, "TJS Catalog: " + tjsCatalog.toString());

                    return new TJS_1_0_0_DataStore(tjsCatalog,featureDataStore,frameworkInfo);
                    // return wmsStoreInfo.getWebMapServer(new NullProgressListener());
                } catch (Exception ex) {
                    Logger.getLogger(JoinDataTransformer.class.getName()).log(Level.SEVERE, "TJS Datastore error: " + ex.getMessage());
                }
                return null;
            }

            private WMSLayerInfo getIfExists(List<WMSLayerInfo> wmsLayers, String layerName) {
                for (WMSLayerInfo layerInfo : wmsLayers) {
                    if (layerInfo.getName().equalsIgnoreCase(layerName)) {
                        return layerInfo;
                    }
                }
                return null;
            }

            private FeatureTypeInfo getFeatureTypeInfoIfExists(List<FeatureTypeInfo> featureTypes, String typeName) {
                for (FeatureTypeInfo featureType : featureTypes) {
                    if (featureType.getName().equalsIgnoreCase(typeName)) {
                        return featureType;
                    }
                }
                return null;
            }


            private InputStream copy(InputStream source) {
                try {
                    File file = File.createTempFile("gdas", ".xml");
                    FileOutputStream fos = new FileOutputStream(file);
                    StreamUtils.copy(source, fos);
                    fos.close();
                    System.out.println("file copied in: " + file.toString());
                    return new FileInputStream(file);
                } catch (IOException ex) {
                    System.out.println("Problem coping file: " + ex.getMessage());
                }
                return null;
            }

            /*Prepara el mecanismo de WMS como resultado de un JoinData. Para ello crea un workspace temporal
            donde poner los mapas que va vinculando con los datos. Crea el TJS_WebMapServer que enlaza la capa
            WMS en cascada y los datos del dataset.
            * */
            private void setUpWMSMechanism(FrameworkInfo frameworkInfo, String datasetURI) throws IOException {
                start("Output");

                handleWMSMechanism();
                WorkspaceInfo tempWorkspaceInfo = createTempWorkspace();
                try {
                    CatalogBuilder builder = new CatalogBuilder(getGeoserverCatalog());

                    String tempWms = getTempWMSUrl(tempWorkspaceInfo);
                    URL tempWmsUrl = new URL(tempWms);
                    DatasetInfo datasetInfo = catalog.getDatasetByUri(datasetURI);
                    WMSStoreInfo tempWmsStore = getGeoserverCatalog().getStoreByName(TJSExtension.TJS_TEMP_WORKSPACE, WMSStoreInfo.class);
                    if (tempWmsStore == null) {
                        tempWmsStore = builder.buildWMSStore(TJSExtension.TJS_TEMP_WORKSPACE);
                        tempWmsStore.setWorkspace(tempWorkspaceInfo);
                        getGeoserverCatalog().add(tempWmsStore);
                        WebMapServer wms = createWebMapServer(frameworkInfo);
                        TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, wms, datasetInfo);
                        getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                    } else {
                        Object wms = null;
                        try {
                            wms = tempWmsStore.getWebMapServer(new NullProgressListener());
                            if (!(wms instanceof TJS_WebMapServer)) {
                                wms = createWebMapServer(frameworkInfo);
                                TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, (WebMapServer) wms, datasetInfo);
                                getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                            } else {
                                TJS_WebMapServer tjswms = (TJS_WebMapServer) tempWmsStore.getWebMapServer(new NullProgressListener());
                                tjswms.update((WebMapServer) wms, datasetInfo);
                            }
                        } catch (MalformedURLException ex) {
                            wms = createWebMapServer(frameworkInfo);
                            TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, (WebMapServer) wms, datasetInfo);
                            getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                        }
                    }

                    //tengo en tempWmsStore el almacen con el servidor de TJS.
                    //Falta publicar la capa del datasetInfo
                    List<WMSLayerInfo> wmsLayers = getGeoserverCatalog().getResourcesByStore(tempWmsStore, WMSLayerInfo.class);
                    WMSLayerInfo exists = getIfExists(wmsLayers, datasetInfo.getName());
                    if (exists == null) {
                        builder.setStore(tempWmsStore);
                        exists = builder.buildWMSLayer(datasetInfo.getName());
                        LayerInfo layer = builder.buildLayer(exists);
                        getGeoserverCatalog().add(exists);
                        getGeoserverCatalog().add(layer);
                    }

                    start("Resource");
                    element("URL", TJSExtension.TJS_TEMP_WORKSPACE + "/wms");

                    AttributesImpl attributes = attributes(new String[]{"name", "domainName"});
                    element("Parameter", "www.iderc.co.cu/geomix/tjs", attributes);

                    attributes = attributes(new String[]{"name", "layerName"});
                    element("Parameter", datasetInfo.getName(), attributes);

                    end("Resource");

                } catch (ServiceException ex) {

                }

                end("Output");
            }

            private void setUpWMSMechanism(FrameworkInfo frameworkInfo, DatasetInfo datasetInfo) throws IOException {
                start("Output");

                handleWMSMechanism();
                WorkspaceInfo tempWorkspaceInfo = createTempWorkspace();
                try {
                    CatalogBuilder builder = new CatalogBuilder(getGeoserverCatalog());

                    String tempWms = getTempWMSUrl(tempWorkspaceInfo);
                    URL tempWmsUrl = new URL(tempWms);
                    WMSStoreInfo tempWmsStore = getGeoserverCatalog().getStoreByName(TJSExtension.TJS_TEMP_WORKSPACE, WMSStoreInfo.class);
                    if (tempWmsStore == null) {
                        tempWmsStore = builder.buildWMSStore(TJSExtension.TJS_TEMP_WORKSPACE);
                        tempWmsStore.setWorkspace(tempWorkspaceInfo);
                        getGeoserverCatalog().add(tempWmsStore);
                        WebMapServer wms = createWebMapServer(frameworkInfo);
                        TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, wms, datasetInfo);
                        getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                    } else {
                        Object wms = null;
                        try {
                            wms = tempWmsStore.getWebMapServer(new NullProgressListener());
                            if (!(wms instanceof TJS_WebMapServer)) {
                                wms = createWebMapServer(frameworkInfo);
                                TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, (WebMapServer) wms, datasetInfo);
                                getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                            } else {
                                TJS_WebMapServer tjswms = (TJS_WebMapServer) tempWmsStore.getWebMapServer(new NullProgressListener());
                                tjswms.update((WebMapServer) wms, datasetInfo);
                            }
                        } catch (MalformedURLException ex) {
                            wms = createWebMapServer(frameworkInfo);
                            TJS_WebMapServer tjswms = new TJS_WebMapServer(tempWmsUrl, (WebMapServer) wms, datasetInfo);
                            getGeoserverCatalog().getResourcePool().setWebMapServer(tempWmsStore, tjswms);
                        }
                    }

                    //tengo en tempWmsStore el almacen con el servidor de TJS.
                    //Falta publicar la capa del datasetInfo
                    List<WMSLayerInfo> wmsLayers = getGeoserverCatalog().getResourcesByStore(tempWmsStore, WMSLayerInfo.class);
                    WMSLayerInfo exists = getIfExists(wmsLayers, datasetInfo.getName());
                    if (exists == null) {
                        builder.setStore(tempWmsStore);
                        exists = builder.buildWMSLayer(datasetInfo.getName());
                        LayerInfo layer = builder.buildLayer(exists);
                        getGeoserverCatalog().add(exists);
                        getGeoserverCatalog().add(layer);
                    }

                    start("Resource");
                    element("URL", getServerURL().concat("/"+TJSExtension.TJS_TEMP_WORKSPACE + "/wms"));

                    AttributesImpl attributes = attributes(new String[]{"name", "domainName"});
                    element("Parameter", "www.iderc.co.cu/geomix/tjs", attributes);

                    attributes = attributes(new String[]{"name", "layerName"});
                    element("Parameter", datasetInfo.getName(), attributes);

                    end("Resource");

                } catch (ServiceException ex) {

                }
                end("Output");
            }

            public String getServerURL() {
                String res = null;
                try {
                    URL handy = new URL(getBaseURL());
                    final String[] pathSegs = handy.getPath().split("/");
                    res = handy.getProtocol() + "://" +
                                  handy.getHost() +
                                  (handy.getPort() != -1 ? ":" + handy.getPort() : "") + "/" +
                                  (pathSegs.length > 0 ? pathSegs[1] : "");
                } catch (MalformedURLException e) {
                    ;
                }
                return res;
            }
        }

    }
}
