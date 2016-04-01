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

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.factory.Hints;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Grid coverage format backed by a Solr heatmap. 
 */
public class SolrHeatmapFormat extends AbstractGridFormat {

    static Logger LOG = Logging.getLogger(SolrHeatmapFormat.class);

    public SolrHeatmapFormat() {
        mInfo = new HashMap<String, String>();
        mInfo.put("name", "SolrHeatmap");
        mInfo.put("description","Solr Heatmap Faceting Format");
        mInfo.put("vendor", "Geotools");
        mInfo.put("version", "1.0");
        mInfo.put("docURL",	"https://cwiki.apache.org/confluence/display/solr/Spatial+Search");

        // reading parameters
        readParameters = new ParameterGroup(
                new DefaultParameterDescriptorGroup(
                        mInfo,
                        new GeneralParameterDescriptor[] { READ_GRIDGEOMETRY2D/*,INPUT_TRANSPARENT_COLOR*/}));
        
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object source) {
        return getReader(source, null);
    }

    @Override
    public SolrHeatmapReader getReader(Object source, Hints hints) {
        URL url = url(source);
        if (url != null) {
            try {
                return new SolrHeatmapReader(url, hints);
            } catch (IOException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public boolean accepts(Object source, Hints hints) {
        return url(source) != null;
    }

    URL url(Object source) {
        return Converters.convert(source, URL.class);
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        return null;
    }

    @Override
    public GridCoverageWriter getWriter(Object destination) {
        return getWriter(destination, null);
    }

    @Override
    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        throw new UnsupportedOperationException("Write not supported");
    }
}
