package gmx.iderc.geoserver.tjs.data;

import com.sun.rowset.CachedRowSetImpl;
import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDatasource;
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
    TJSDatasource tjsDatasource = null;

    HashMap<Object, Integer> index = new HashMap<Object, Integer>();

    private Object lookup(Object keyValue, String fieldName) {
        if (keyValue != null) {
            try {
                int absRow = -1;
                try {
                    // Convert values to Strings for now, since all GDAS attributes are strings
                    // TODO: use GDAS types better, but this requires some conversion work
                    if(keyValue instanceof Double) {
                        // parse it to a string
                        keyValue = ((Double) keyValue).intValue();
                    }
                    if (keyValue instanceof Integer) {
                        keyValue = keyValue.toString();
                    }
                    absRow = index.get(keyValue);
                }  catch (Exception ex){
                    // Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
                }
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
            if (this.tjsDatasource==null)  {
                this.tjsDatasource = datasetInfo.getTJSDatasource();
            }
            RowSet remote = this.tjsDatasource.getRowSet();
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
        this.featureReader.close();
        try {
            rst.close();
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
    }

    public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {

        // TODO: do the loop the other way around? Use the rst to loop over features and not all features?
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        SimpleFeature wfsFeature = featureReader.next();
        featureBuilder.addAll(wfsFeature.getAttributes());

        SimpleFeature ft = null;

        String frameworkKey = datasetInfo.getFramework().getFrameworkKey().getName();
        Object keyValue = wfsFeature.getAttribute(frameworkKey);

        // TODO: decide if no result, then return null and skip the result? Or add all result and provide empty values (as done now)
        boolean match = false;

        // TODO: Thijs Brentjens, for codesprint
        // What if the value is an Integer? cast it?
        for (ColumnInfo column : datasetInfo.getColumns()) {
            // TODO: always to string? Or to the type as defined in the GDAS file?
            // NOTE: also see other classes for createing the GDAS cache where column names are changed
            /* String columnNameFT = column.getName();
            if (Character.isDigit(columnNameFT.charAt(0))) {
                columnNameFT = "_" + columnNameFT;
            } */
            // System.out.println("FeatureReader columnname: " + columnNameFT);
            Object newValue = null;
            try {
                if (keyValue != null) {
                    newValue = lookup(keyValue, column.getName());
                }
            } catch (Exception ex) {
                newValue = "";
            }
            if (newValue == null) {
                newValue = "";
            } else {
                // We do have at least one value, so we can continue building the feature
                match = true;
            }
            featureBuilder.set(column.getName(), newValue.toString());
        }

        if (!match) {
            Logger.getLogger(TJSFeatureReader.class).info("We don't have an object match for " + wfsFeature.getID() + ", let's continue with the next feature.");
        }

        try {
            ft = featureBuilder.buildFeature(wfsFeature.getID());
            Logger.getLogger(TJSFeatureReader.class).debug("We have a feature " + ft.getID());
            return ft;
        } catch (Exception ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }

        return ft;
    }

    public boolean hasNext() throws IOException {
        // TODO: this is important if only the number of joined objects should be returned?
        return featureReader.hasNext();
    }
}
