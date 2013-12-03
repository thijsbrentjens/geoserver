package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.impl.TJSCatalogFactoryImpl;
import org.geoserver.catalog.*;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/8/12
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TJSStore implements DataStoreInfo {

    TJS_1_0_0_DataStore store;
    Catalog catalog;
    String id;

    public TJSStore(TJS_1_0_0_DataStore store, Catalog catalog) {
        this.store = store;
        this.catalog = catalog;
        this.id = TJSCatalogFactoryImpl.getIdForObject(this);
    }

    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(ProgressListener listener) throws IOException {
        return store;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public String getName() {
        return store.getFrameworkInfo().getName();
    }

    public void setName(String name) {
        //this.name = name;
    }

    public String getDescription() {
        return store.getFrameworkInfo().getDescription();
    }

    public void setDescription(String description) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getType() {
        return "TJS Store";
    }

    public void setType(String type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public MetadataMap getMetadata() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEnabled() {
        return store.getFrameworkInfo().getEnabled();
    }

    public void setEnabled(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public WorkspaceInfo getWorkspace() {
        return catalog.getWorkspaceByName(TJSExtension.TJS_TEMP_WORKSPACE);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Serializable> getConnectionParameters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Throwable getError() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setError(Throwable t) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void accept(CatalogVisitor visitor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getId() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
