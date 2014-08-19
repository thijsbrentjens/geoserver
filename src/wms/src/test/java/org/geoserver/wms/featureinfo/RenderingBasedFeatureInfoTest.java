package org.geoserver.wms.featureinfo;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;

import javax.xml.namespace.QName;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;

public class RenderingBasedFeatureInfoTest extends WMSTestSupport {

    public static QName GRID = new QName(MockData.CITE_URI, "grid", MockData.CITE_PREFIX);
    public static QName REPEATED = new QName(MockData.CITE_URI, "repeated", MockData.CITE_PREFIX);

    
	@Override
	protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
	}

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        testData.addStyle("box-offset", "box-offset.sld",this.getClass(), getCatalog());
        File styles = getDataDirectory().findOrCreateStyleDir();
        File symbol = new File("./src/test/resources/org/geoserver/wms/featureinfo/box-offset.png");
        FileUtils.copyFileToDirectory(symbol, styles);
        
        testData.addVectorLayer(GRID, Collections.EMPTY_MAP, "grid.properties",
                RenderingBasedFeatureInfoTest.class, getCatalog());
        testData.addVectorLayer(REPEATED, Collections.EMPTY_MAP, "repeated_lines.properties",
                RenderingBasedFeatureInfoTest.class, getCatalog());
        
        testData.addStyle("ranged", "ranged.sld",this.getClass(), getCatalog());
        testData.addStyle("dynamic", "dynamic.sld",this.getClass(), getCatalog());
        testData.addStyle("symbol-uom", "symbol-uom.sld", this.getClass(), getCatalog());
        testData.addStyle("two-rules", "two-rules.sld", this.getClass(), getCatalog());
        testData.addStyle("two-fts", "two-fts.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed", "dashed.sld",this.getClass(), getCatalog());
        testData.addStyle("polydash", "polydash.sld", this.getClass(), getCatalog());
        testData.addStyle("doublepoly", "doublepoly.sld", this.getClass(), getCatalog());
    }
    
    @After 
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
    }
    
    @Test
    public void testBoxOffset() throws Exception {
        // try the old way clicking in the area of the symbol that is transparent
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        String url = "wms?REQUEST=GetFeatureInfo&BBOX=1.9E-4,6.9E-4,2.1E-4,7.1E-4&SERVICE=WMS&INFO_FORMAT=application/json"
                + "&QUERY_LAYERS=cite%3ABridges&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100"
                + "&format=image%2Fpng&styles=box-offset&srs=EPSG%3A4326&version=1.1.1&x=50&y=63&feature_count=50";
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(1, result1.getJSONArray("features").size());
     
        // the new aware is aware that we're clicking into "nothing"
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(0, result2.getJSONArray("features").size());
    }
    
    @Test
    public void testRangedSize() throws Exception {
        // use a style that has a rule with a large symbolizer, but the point is 
        // actually painted with a much smaller one
        String url = "wms?REQUEST=GetFeatureInfo&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704"
        + "&SERVICE=WMS&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50&Layers=cite%3ABridges"
        + "&WIDTH=100&HEIGHT=100&format=image%2Fpng&styles=ranged&srs=EPSG%3A4326&version=1.1.1&x=49&y=65&feature_count=50";
        
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(1, result1.getJSONArray("features").size());
     
        // the new aware is aware that we're clicking into "nothing"
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(0, result2.getJSONArray("features").size());
    }
    
    @Test
    public void testDynamicSize() throws Exception {
        // use a style that has a rule with a attribute dependent size, the old code 
        // will fallback on the default size since the actual one is not known
        String url = "wms?REQUEST=GetFeatureInfo"
                + "&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50"
                + "&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100&format=image%2Fpng"
                + "&styles=dynamic&srs=EPSG%3A4326&version=1.1.1&x=49&y=60&feature_count=50";
        
        // the default buffer is not large enough to realize we clicked on the mark
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(0, result1.getJSONArray("features").size());
     
        // the new is aware that we're clicking onto the feature instead
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(1, result2.getJSONArray("features").size());
    }
    
    @Test
    public void testUom() throws Exception {
        // this results in a very large symbol (the map 8m wide and 100 pixels), but if you
        // don't handle uom, you don't get to know that
        String url = "wms?REQUEST=GetFeatureInfo"
                + "&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50"
                + "&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100&format=image%2Fpng"
                + "&styles=symbol-uom&srs=EPSG%3A4326&version=1.1.1&x=49&y=60&feature_count=50";
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testTwoRules() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&styles=two-rules"
                + "&width=20&height=20&x=10&y=10" + "&info_format=application/json&feature_count=50";

        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testTwoFeatureTypeStyles() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&styles=two-fts"
                + "&width=20&height=20&x=10&y=10&info_format=application/json";

        System.out.println("The response iTESTs: " + getAsString(request));
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testFillStrokeDashArray() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&styles=polydash" + "&width=20&height=20&x=10&y=10&info_format=application/json";

        System.out.println("The response iTESTs: " + getAsString(request));
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testGenericGeometry() throws Exception {
    	String layer = getLayerId(MockData.GENERICENTITY);
    	String request = "wms?REQUEST=GetFeatureInfo&BBOX=-2.73291%2C55.220703%2C8.510254%2C69.720703&SERVICE=WMS"
    			+ "&INFO_FORMAT=application/json&QUERY_LAYERS=" + layer + "&Layers=" + layer 
    			+ "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=line&srs=EPSG%3A4326&version=1.1.1&x=284&y=269";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results 
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testDashed() throws Exception {
    	String layer = getLayerId(MockData.GENERICENTITY);
    	String request = "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
    			+ "&INFO_FORMAT=application/json&QUERY_LAYERS=" + layer + "&Layers=" + layer 
    			+ "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=dashed&srs=EPSG%3A4326&version=1.1.1&x=182&y=241";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results 
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testDoublePoly() throws Exception {
        String layer = getLayerId(GRID);
        String request = "wms?REQUEST=GetFeatureInfo&&BBOX=0,0,3,3&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                + layer
                + "&Layers="
                + layer
                + "&WIDTH=90&HEIGHT=90&format=image%2Fpng&styles=doublepoly&srs=EPSG%3A4326&version=1.1.1&x=34&y=34";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testRepeatedLine() throws Exception {
        String layer = getLayerId(REPEATED);
        String request = "wms?REQUEST=GetFeatureInfo&&BBOX=499900,499900,500100,500100&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                + layer
                + "&Layers="
                + layer
                + "&WIDTH=11&HEIGHT=11&format=image%2Fpng&styles=line&srs=EPSG%3A32615&version=1.1.1&x=5&y=5";
        JSONObject result = (JSONObject) getAsJSON(request);
        print(result);
        // we used to get two results
        assertEquals(2, result.getJSONArray("features").size());
    }

    
}
