package gmx.iderc.geoserver.tjs.data;

import com.sun.rowset.CachedRowSetImpl;
import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import org.apache.log4j.Logger;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import javax.sql.RowSet;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 9/22/12
 * Time: 11:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJSFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    FeatureReader<SimpleFeatureType, SimpleFeature> featureReader;
    DatasetInfo datasetInfo;
    SimpleFeatureType type;
    CachedRowSetImpl rst;

    HashMap<Object, Integer> index = new HashMap<Object, Integer>();

    private Object lookup(Object keyValue, String fieldName) {
        int absRow = index.get(keyValue);
        try {
            if (rst.absolute(absRow)) {
                int findex = rst.findColumn(fieldName);
                return rst.getObject(findex);
            }
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
        return null;
    }

    private void indexRowSet() throws SQLException {
        int keyIndex = rst.findColumn(datasetInfo.getGeoKeyField());
        while (rst.next()) {
            index.put(rst.getObject(keyIndex), rst.getRow());
        }
    }

    public TJSFeatureReader(SimpleFeatureType type, FeatureReader<SimpleFeatureType, SimpleFeature> featureReader, DatasetInfo datasetInfo) {
        this.featureReader = featureReader;
        this.datasetInfo = datasetInfo;
        this.type = type;
        try {
            rst = new CachedRowSetImpl();
            RowSet remote = datasetInfo.getTJSDatasource().getRowSet();
            rst.populate(remote);
            remote.close();
            indexRowSet();
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
    }

    public SimpleFeatureType getFeatureType() {
        return this.type;
    }

    public void close() throws IOException {
        featureReader.close();
        try {
            rst.close();
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
    }

    public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        SimpleFeature wfsFeature = featureReader.next();
        featureBuilder.addAll(wfsFeature.getAttributes());

        Name frameworkKey = datasetInfo.getFramework().getFrameworkKey().getAttribute().getName();
        Object keyValue = wfsFeature.getAttribute(frameworkKey);

        for (ColumnInfo column : datasetInfo.getColumns()) {
            featureBuilder.set(column.getName(), lookup(keyValue, column.getName()));
        }
        return featureBuilder.buildFeature(wfsFeature.getID());
    }

    public boolean hasNext() throws IOException {
        return featureReader.hasNext();
    }
}
