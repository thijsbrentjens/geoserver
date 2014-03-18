package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 9/22/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJS_1_0_0_DataStore extends AbstractDataStore {
    // WFSDataStore wfsDataStore;
    DataStore featureDataStore;
    FrameworkInfo frameworkInfo;
    TJSCatalog catalog;
    HashMap<String, SimpleFeatureType> storeTypeNames = new HashMap<String, SimpleFeatureType>();

    public FrameworkInfo getFrameworkInfo() {
        return frameworkInfo;
    }

    public TJS_1_0_0_DataStore(TJSCatalog catalog, DataStore featureDataStore, FrameworkInfo frameworkInfo) {
        this.catalog = catalog;
        this.featureDataStore = featureDataStore;
        this.frameworkInfo = frameworkInfo;
    }

    public String[] getTypeNames() throws IOException {
        List<String> typeNames = new ArrayList<String>();
        List<DatasetInfo> datasets = catalog.getDatasetsByFramework(frameworkInfo.getId());
        for (DatasetInfo dataset : datasets) {
            typeNames.add(dataset.getName());
        }
        String[] typeNamesArray = new String[typeNames.size()];
        typeNames.toArray(typeNamesArray);
        return typeNamesArray;
    }

    public SimpleFeatureType getSchema(String typeName) {
        if (storeTypeNames.containsKey(typeName)) {
            return storeTypeNames.get(typeName);
        }

        String wfsTypeName = frameworkInfo.getFeatureType().getNativeName();
        SimpleFeatureType wfsFeatureType = null;
        try {
            wfsFeatureType = featureDataStore.getSchema(wfsTypeName);
        } catch (IOException ex) {

        }

        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName(typeName);

        if (wfsFeatureType != null) {
            featureTypeBuilder.addAll(wfsFeatureType.getAttributeDescriptors());
        }
        DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);

        // Fixed the column?
        for (ColumnInfo column : datasetInfo.getColumns()) {
            // add the length to the attribute value
            // featureTypeBuilder.length(column.getLength());
            System.out.println(column.getName()+ ", column.getSQLClassBinding(): " + column.getSQLClassBinding().toString());
            // class attrClass = ;
            Class attrClass = String.class;
            // TODO: this class mapping is a workaround to avoid cutting off Strings (beacuase the SQL Class Binding seems to be java.lang.Character)
            // Find out where type mapping is done.
            // How to check the class and use that for the featureTypeBuilder??
            if (column.getSQLClassBinding().equals( (Class)java.lang.Character.class ) ) {
                attrClass = String.class;
            }   else {
                // TODO: integers, doubles, etc?
                // assume this is correct now..
                attrClass = column.getSQLClassBinding();
            }
            featureTypeBuilder.add(column.getName(), attrClass);
        }
        // TODO: deal with the namespace
        featureTypeBuilder.setNamespaceURI("");
        SimpleFeatureType newFt = featureTypeBuilder.buildFeatureType();
        storeTypeNames.put(typeName, newFt);
        return storeTypeNames.get(typeName);
    }

    // TODO: Thijs: implement something to get the featuretypeinfo here?      Separate class TJSFeatureSource?
    // or  create a feature source here? Dynamically or persist data in database and create a new datasource from that?

    /* public FeatureSource getFeatureSource(String typeName) {
        FeatureSource fs;

        return fs;
    }  */

    // Thijs: was protected, but need it elsewhere.
    // Is this appropriate?
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        String wfsTypeName = frameworkInfo.getFeatureType().getNativeName();
        FeatureReader<SimpleFeatureType, SimpleFeature> wfsFeatureReader = featureDataStore.getFeatureReader(new DefaultQuery(wfsTypeName), new DefaultTransaction());
        DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);
        return new TJSFeatureReader(getSchema(typeName), wfsFeatureReader, datasetInfo);
    }

}
