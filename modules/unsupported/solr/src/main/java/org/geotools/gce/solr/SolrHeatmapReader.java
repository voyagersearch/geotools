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

import com.google.common.base.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.Format;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coverage reader for Solr heatmaps.
 */
public class SolrHeatmapReader extends AbstractGridCoverage2DReader {

    static Logger LOG = Logging.getLogger(SolrHeatmapReader.class);

    static String FLOAT = "(-?[0-9]+\\.?[0-9]*(?:[Ee][+-]?[0-9]+)?)";
    static Pattern HEATMAP_GEOM_RE = Pattern.compile(String.format(Locale.ROOT,
            "\\[\"%s\\s+%s\"\\s+TO\\s+\"%s\\s+%s\"\\]", FLOAT, FLOAT, FLOAT, FLOAT));

    SolrClient solr;
    String field;

    GridCoverageFactory covFactory = CoverageFactoryFinder.getGridCoverageFactory(GeoTools.getDefaultHints());

    public SolrHeatmapReader(URL url, Hints hints) throws IOException {
        solr = new HttpSolrClient(String.format(Locale.ROOT, "%s://%s:%d/%s", 
            url.getProtocol(), url.getHost(), url.getPort(), url.getPath()));
        try {
            init(url.getQuery());
        } catch (Exception e) {
            throw new IOException(e);
        }

    }
    
    void init(String queryString) throws Exception {
        double[] bounds = null;
        if (queryString != null) {
            for (NameValuePair kvp : URLEncodedUtils.parse(queryString, Charsets.UTF_8)) {
                String val = kvp.getValue();
                switch(kvp.getName()) {
                    case "facet.heatmap":
                        field = val;
                        break;
                    case "facet.heatmap.geom":
                        Matcher m = HEATMAP_GEOM_RE.matcher(val);
                        if (m.matches()) {
                            bounds = new double[]{
                                Double.parseDouble(m.group(1)),
                                Double.parseDouble(m.group(2)),
                                Double.parseDouble(m.group(3)),
                                Double.parseDouble(m.group(4))
                            };
                        }
                        else {
                            LOG.warning(String.format("Unrecognized value for %s, falling back to default bounds", val));
                        }
                        break;
                    case "crs":
                        crs = CRS.decode(val);
                        break;
                }
            }
        }

        field = field != null ? field : "geo";

        coverageName = field + "_heatmap";
        crs = crs != null ? crs : CRS.decode("EPSG:4326");
        
        bounds = bounds != null ? bounds : initBounds(crs);
        originalEnvelope = new GeneralEnvelope(new ReferencedEnvelope(bounds[0], bounds[2], bounds[1], bounds[3], crs));

        originalGridRange = initGridRange(); 
    }

    double[] initBounds(CoordinateReferenceSystem crs) {
        Envelope env = CRS.getEnvelope(crs);
        if (env != null) {
            if (env.getDimension() < 2) {
                throw new IllegalArgumentException("CRS must have two dimensions:" + crs);
            }

            return new double[]{env.getMinimum(0), env.getMinimum(1), env.getMaximum(0), env.getMaximum(1)};
        }

        if (crs instanceof GeographicCRS) {
            return new double[]{-180,-90, 180,90};
        }

        throw new IllegalArgumentException("Unable to determine bounds from crs: " + crs);
    }

    GridEnvelope2D initGridRange() throws IOException {
        // read heatmap at original bounds
        QueryResponse rsp = query(null);
        NamedList hm = heatmap(rsp);
        return new GridEnvelope2D(rect(hm));
    }

    @Override
    public Format getFormat() {
        return new SolrHeatmapFormat();
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        // query and build coverage from the result
        return coverage(query(parameters));
    }

    QueryResponse query(GeneralParameterValue[] parameters) throws IOException {
        // get the bounds to request, falling back on default
        GeneralEnvelope bounds = originalEnvelope;
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                final ParameterValue param = (ParameterValue) parameters[i];
                final ReferenceIdentifier name = param.getDescriptor().getName();
                if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName())) {
                    final GridGeometry2D gg = (GridGeometry2D) param.getValue();
                    bounds = new GeneralEnvelope((Envelope) gg.getEnvelope2D());
                    continue;
                }
            }
        }

        if (bounds != originalEnvelope && !originalEnvelope.contains(bounds, true)) {
            // solr is picky if a bbox falls outside of the max bounds, so clip
            bounds.intersect(originalEnvelope);
        }

        // build up the heatmap facet query
        SolrQuery q = new SolrQuery("*:*");
        q.setFacet(true);
        q.setRows(0);
        q.set("facet.heatmap", field);
        q.set("facet.heatmap.geom", String.format(Locale.ROOT, "[\"%f %f\" TO \"%f %f\"]",
                bounds.getMinimum(0), bounds.getMinimum(1), bounds.getMaximum(0), bounds.getMaximum(1)));
        q.set("facet.heatmap.format", "ints2D");

        try {
            return solr.query(q);
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    GridCoverage2D coverage(QueryResponse rsp) {
        NamedList heatmap = heatmap(rsp);
        float[][] grid = grid(heatmap);
        
        return covFactory.create("heatmap", grid, bounds(heatmap));
    }

    /**
     * Extract the bounds from the heatmap.
     */
    Envelope bounds(NamedList hm) {
        return new ReferencedEnvelope(doub(hm, "minX"), doub(hm, "maxX"), doub(hm, "minY"), doub(hm, "maxY"), crs);
    }

    /**
     * Extract the dimensions from the heatmap.
     */
    Rectangle rect(NamedList hm) {
        return new Rectangle(0,0, integer(hm, "columns"), integer(hm, "rows"));
    }

    /**
     * Extract the grid data from the heatmap.
     */
    float[][] grid(NamedList heatmap) {
        Rectangle rect = rect(heatmap);
        
        float[][] grid = new float[rect.height][rect.width];
        List<List<Integer>> counts = (List<List<Integer>>) heatmap.get("counts_ints2D");
        if (counts != null) {
            for (int i = 0; i < rect.height; i++) {
                List<Integer> row = counts.get(i);
                if (row != null) {
                    for (int j = 0; j < rect.width; j++) {
                        float val = row.get(j);
                        grid[i][j] = val;
                    }
                }
            }
        }

        return grid;
    }

    /**
     * Grab the heatmap object from the response.
     * 
     * @param rsp The full query response.
     */
    NamedList heatmap(QueryResponse rsp) {
        return find(rsp.getResponse(), "facet_counts", "facet_heatmaps", field);
    }

    /**
     * Pull a value out of the named list.
     * 
     * @param l The list.
     * @param path The path.
     */
    NamedList find(NamedList l, String... path) {
        for (int i = 0; i < path.length; i++) {
           if (l == null) {
               throw new IllegalArgumentException("Bad path: " + path);
           }

           l = (NamedList) l.get(path[i]);
        }

        return l;
    }

    /**
     * Grab a double value from the list.
     */
    double doub(NamedList l, String key) {
        Number n = (Number) l.get(key);
        return n != null ? n.doubleValue() : 0;
    }

    /**
     * Grab an integer value from the list.
     */
    int integer(NamedList l, String key) {
        Number n = (Number) l.get(key);
        return n != null ? n.intValue() : 0;
    }

}
