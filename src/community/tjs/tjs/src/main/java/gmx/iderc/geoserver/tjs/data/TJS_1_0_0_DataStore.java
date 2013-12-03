package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import org.geotools.data.AbstractDataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 9/22/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJS_1_0_0_DataStore extends AbstractDataStore {
    WFSDataStore wfsDataStore;
    FrameworkInfo frameworkInfo;
    TJSCatalog catalog;
    HashMap<String, SimpleFeatureType> storeTypeNames = new HashMap<String, SimpleFeatureType>();

    public FrameworkInfo getFrameworkInfo() {
        return frameworkInfo;
    }

    public TJS_1_0_0_DataStore(TJSCatalog catalog, WFSDataStore wfsDataStore, FrameworkInfo frameworkInfo) {
        this.catalog = catalog;
        this.wfsDataStore = wfsDataStore;
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
            wfsFeatureType = wfsDataStore.getSchema(wfsTypeName);
        } catch (IOException ex) {

        }
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        if (wfsFeatureType != null) {
            featureTypeBuilder.addAll(wfsFeatureType.getAttributeDescriptors());
        }
        DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);
        for (ColumnInfo column : datasetInfo.getColumns()) {
            featureTypeBuilder.add(column.getName(), column.getSQLClassBinding());
        }
        storeTypeNames.put(typeName, featureTypeBuilder.buildFeatureType());
        return storeTypeNames.get(typeName);
    }

    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        String wfsTypeName = frameworkInfo.getFeatureType().getNativeName();
        FeatureReader<SimpleFeatureType, SimpleFeature> wfsFeatureReader = wfsDataStore.getFeatureReader(new DefaultQuery(wfsTypeName), new DefaultTransaction());
        DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);
        return new TJSFeatureReader(getSchema(typeName), wfsFeatureReader, datasetInfo);
    }

}
