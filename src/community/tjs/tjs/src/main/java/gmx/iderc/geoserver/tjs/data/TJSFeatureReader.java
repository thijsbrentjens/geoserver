package gmx.iderc.geoserver.tjs.data;

import com.sun.rowset.CachedRowSetImpl;
import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import org.geotools.data.store.ContentState;

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
        try {
            int absRow = index.get(keyValue);
            if (absRow >= 0) {
                if (rst.absolute(absRow)) {
                    int findex = rst.findColumn(fieldName);
                    if (findex >= 0) {
                        return rst.getObject(findex);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }  catch (Exception ex) {
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

    public TJSFeatureReader(ContentState contentState) {
        // TODO Thijs: check for nulls / not available parts  ?
        SimpleFeatureType ft =  contentState.getFeatureType();

        new TJSFeatureReader(ft, this.featureReader, this.datasetInfo)  ;
    }

    public TJSFeatureReader(SimpleFeatureType type, FeatureReader<SimpleFeatureType, SimpleFeature> featureReader, DatasetInfo datasetInfo) {
        this.featureReader = featureReader;
        this.datasetInfo = datasetInfo;
        this.type = type;
        // TODO: determine if features without joined results should be skipped  / removed or have empty values
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

        String frameworkKey = datasetInfo.getFramework().getFrameworkKey().getName();
        Object keyValue = wfsFeature.getAttribute(frameworkKey);

        // TODO: decide if no result, then return null and skip the result? Or add all result and provide empty values (as done now)
        for (ColumnInfo column : datasetInfo.getColumns()) {
            Object newValue = null;
            try {
                if (keyValue!=null) {
                    newValue = lookup(keyValue, column.getName());
                }
            } catch (Exception ex) {
                System.out.println("Exception for : " + keyValue + " ------- " + ex.getMessage());
                newValue = "";
            }
            if (newValue == null) {
                newValue = "";
            }
            featureBuilder.set(column.getName(), newValue.toString());
        }

        try {
            SimpleFeature ft = featureBuilder.buildFeature(wfsFeature.getID());
            return ft;
        } catch (Exception ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
        return null;
    }

    public boolean hasNext() throws IOException {
        return featureReader.hasNext();
    }
}
