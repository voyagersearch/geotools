/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2014 - 2016, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.solr;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.solr.SolrTestSupport;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ColorMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.test.OnlineTestCase;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class SolrHeatmapReaderTest extends OnlineTestCase {

    SolrHeatmapReader reader;

    public void test() throws Exception {
        GridCoverage2D cov = reader.read(null);
        assertNotNull(cov);

        Envelope2D env = cov.getEnvelope2D();
        assertEquals(-180d, env.getMinX(), 0.1);
        assertEquals(180d, env.getMaxX(), 0.1);
        assertEquals(-90d, env.getMinY(), 0.1);
        assertEquals(90d, env.getMaxY(), 0.1);
        
        GridGeometry2D gg = cov.getGridGeometry();
        assertEquals(32d, gg.getGridRange2D().getWidth(), 0.1);
        assertEquals(32d, gg.getGridRange2D().getHeight(), 0.1);

        //draw(cov);
    }

    void draw(final GridCoverage2D cov) throws IOException {
        StyleBuilder sb = new StyleBuilder();

        FilterFactory ff = sb.getFilterFactory();
        Function convolve = ff.function("ras:ConvolveCoverage",
            ff.function("parameter", ff.literal("data")), 
            ff.function("parameter", ff.literal("kernelWidth"), ff.literal(5))
        );

        Function normalize = ff.function("ras:NormalizeCoverage", 
            ff.function("parameter", ff.literal("data"), convolve)
        );

        FeatureTypeStyle fts = sb.getStyleFactory().createFeatureTypeStyle();
        fts.setTransformation(normalize);

        ColorMap colorMap = sb.createColorMap(
            new String[]{"white", "red"}, new double[]{0,1}, new Color[]{Color.white, Color.red}, ColorMap.TYPE_RAMP);

        fts.rules().add(sb.createRule(sb.createRasterSymbolizer(colorMap, 1d)));

        Style style = sb.getStyleFactory().createStyle();
        style.featureTypeStyles().add(fts);

        MapContent map = new MapContent();
        map.addLayer(new GridCoverageLayer(cov, style));

        StreamingRenderer r = new StreamingRenderer();
        r.setMapContent(map);

        BufferedImage img = new BufferedImage(1024, 512, BufferedImage.TYPE_4BYTE_ABGR);
        r.paint(img.createGraphics(), new Rectangle(0, 0, img.getWidth(), img.getHeight()), new ReferencedEnvelope(cov.getEnvelope()));

        //javax.imageio.ImageIO.write(img, "png", new java.io.File("/Users/jdeolive/heatmap.png"));
        //java.lang.Runtime.getRuntime().exec("open /Users/jdeolive/heatmap.png");
    }

    @Override
    protected String getFixtureId() {
        return SolrTestSupport.FIXTURE_ID;
    }

    @Override
    protected void connect() throws Exception {
        String solrUrl = fixture.getProperty("solr_url");

        String heatmapField = fixture.getProperty("heatmap_field");
        if (heatmapField == null) heatmapField = "geo";

        solrUrl += "?facet.heatmap="+heatmapField +"&facet.heatmap.geom=[\"-180 -90\" TO \"180 90\"]";
        //solrUrl += "?facet.heatmap="+heatmapField +"&facet.heatmap.geom=[\"0 0\" TO \"20 20\"]";
        
        reader = new SolrHeatmapReader(new URL(solrUrl), null);
    }
}
