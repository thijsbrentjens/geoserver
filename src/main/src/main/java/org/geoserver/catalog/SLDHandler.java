/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geoserver.ows.util.RequestUtils;
import org.geotools.data.DataUtilities;
import org.geotools.sld.v1_1.*;
import org.geotools.sld.v1_1.SLD;
import org.geotools.styling.*;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * SLD style handler.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class SLDHandler extends StyleHandler {

    static Logger LOGGER = Logging.getLogger(SLDHandler.class);

    /**
     * number of bytes to "look ahead" when pre parsing xml document.
     * TODO: make this configurable, and possibley link it to the same value
     * used by the ows dispatcher.
     */
    static int XML_LOOKAHEAD = 8192;

    public static final String FORMAT = "sld";

    public static final Version VERSION_10 = new Version("1.0.0");
    public static final Version VERSION_11 = new Version("1.1.0");

    public static final String MIMETYPE_10 = "application/vnd.ogc.sld+xml";
    public static final String MIMETYPE_11 = "application/vnd.ogc.se+xml";

    public SLDHandler() {
        super("SLD", FORMAT);
    }

    @Override
    public List<Version> getVersions() {
        return Arrays.asList(VERSION_10, VERSION_11);
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "xml";
    }

    @Override
    public String mimeType(Version version) {
        if (version != null && VERSION_11.equals(version)) {
            return MIMETYPE_11;
        }
        return MIMETYPE_10;
    }

    @Override
    public Version versionForMimeType(String mimeType) {
        if (mimeType.equals(MIMETYPE_11)) {
            return VERSION_11;
        }
        return VERSION_10;
    }

    @Override
    public StyledLayerDescriptor parse(Object input, Version version, ResourceLocator resourceLocator, EntityResolver entityResolver) throws IOException {
        if (version == null) {
            Object[] versionAndReader = getVersionAndReader(input);
            version = (Version) versionAndReader[0];
            input = versionAndReader[1];
        }

        if (VERSION_11.compareTo(version) == 0) {
            return parse11(input, resourceLocator, entityResolver);
        }
        else {
            return parse10(input, resourceLocator, entityResolver);
        }
    }

    StyledLayerDescriptor parse10(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
        throws IOException {

        SLDParser p = createSld10Parser(input, resourceLocator, entityResolver);
        StyledLayerDescriptor sld = p.parseSLD();
        if (sld.getStyledLayers().length == 0) {
            //most likely a style that is not a valid sld, try to actually parse out a
            // style and then wrap it in an sld
            Style[] style = p.readDOM();
            if (style.length > 0) {
                NamedLayer l = styleFactory.createNamedLayer();
                l.addStyle(style[0]);
                sld.addStyledLayer(l);
            }
        }

        return sld;
    }

    StyledLayerDescriptor parse11(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
        throws IOException {
        Parser parser = createSld11Parser(input, resourceLocator, entityResolver);
        try {
            parser.setEntityResolver(entityResolver);
            return (StyledLayerDescriptor) parser.parse(toReader(input));
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }

    SLDParser createSld10Parser(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
        throws IOException {
        SLDParser parser;
        if (input instanceof File) {
            parser = new SLDParser(styleFactory, (File) input);
        }
        else {
            parser = new SLDParser(styleFactory, toReader(input));
        }

        if (resourceLocator != null) {
            parser.setOnLineResourceLocator(resourceLocator);
        }
        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        return parser;
    }

    Parser createSld11Parser(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver) {
        if (resourceLocator == null && input instanceof File) {
            // setup for resolution of relative paths
            final java.net.URL surl = DataUtilities.fileToURL((File) input);
            DefaultResourceLocator defResourceLocator = new DefaultResourceLocator();
            defResourceLocator.setSourceUrl(surl);
            resourceLocator = defResourceLocator;
        }

        final ResourceLocator locator = resourceLocator;
        SLDConfiguration sld;
        if (locator != null) {
            sld = new SLDConfiguration() {
                protected void configureContext(org.picocontainer.MutablePicoContainer container) {
                    container.registerComponentInstance(ResourceLocator.class, locator);
                };
            };
        }
        else {
            sld = new SLDConfiguration();
        }

        return new Parser(sld);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output) throws IOException {
        if (version != null && VERSION_11.compareTo(version) == 0) {
            encode11(sld, pretty, output);
        }
        else {
            encode10(sld, pretty, output);
        }
    }

    void encode10(StyledLayerDescriptor sld, boolean pretty, OutputStream output)
        throws IOException {
        SLDTransformer tx = new SLDTransformer();
        if (pretty) {
            tx.setIndentation(2);
        }
        try {
            tx.transform( sld, output );
        }
        catch (TransformerException e) {
            throw (IOException) new IOException("Error writing style").initCause(e);
        }
    }

    void encode11(StyledLayerDescriptor sld, boolean pretty, OutputStream output) throws IOException {
        Encoder e = new Encoder(new SLDConfiguration());
        e.setIndenting(pretty);
        e.encode(sld, SLD.StyledLayerDescriptor, output);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        if (version == null) {
            Object[] versionAndReader = getVersionAndReader(input);
            version = (Version) versionAndReader[0];
            input = versionAndReader[1];
        }

        if (version != null && VERSION_11.compareTo(version) == 0) {
            return validate11(input, entityResolver);
        }
        else {
            return validate10(input, entityResolver);
        }
    }

    List<Exception> validate10(Object input, EntityResolver entityResolver) throws IOException {
        return new SLDValidator().validateSLD(new InputSource(toReader(input)));
    }

    List<Exception> validate11(Object input, EntityResolver entityResolver) throws IOException {
        Parser p = createSld11Parser(input, null, null);
        try {
            p.validate(toReader(input));
            return p.getValidationErrors();
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Version version(Object input) throws IOException {
        Object[] versionAndReader = getVersionAndReader(input);
        return (Version) versionAndReader[0];
    }

    /**
     * Helper method for finding which style handler/version to use from the actual content.
     */
    Object[] getVersionAndReader(Object input) throws IOException {
        //need to determine version of sld from actual content
        BufferedReader reader = null;

        if (input instanceof InputStream) {
            reader = RequestUtils.getBufferedXMLReader((InputStream) input, XML_LOOKAHEAD);
        }
        else {
            reader = RequestUtils.getBufferedXMLReader(toReader(input), XML_LOOKAHEAD);
        }

        if (!reader.ready()) {
            return null;
        }

        String version;
        try {
            //create stream parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            //parse root element
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(reader);
            parser.nextTag();

            version = null;
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if ("version".equals(parser.getAttributeName(i))) {
                    version = parser.getAttributeValue(i);
                }
            }

            parser.setInput(null);
        }
        catch (XmlPullParserException e) {
            throw (IOException) new IOException("Error parsing content").initCause(e);
        }

        //reset input stream
        reader.reset();

        if (version == null) {
            LOGGER.warning("Could not determine SLD version from content. Assuming 1.0.0");
            version = "1.0.0";
        }

        return new Object[]{new Version(version), reader};
    }
}
