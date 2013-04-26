/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2013, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.xml.impl;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDSchemaLocationResolver;

/**
 * Resolves includes/imports that are relative to the main schema being parsed.
 * <p>
 * This resolver is used in cases where the user is trying to parse a custom schema, and that 
 * schema includes/imports other schema files.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RelativeSchemaLocationResolver implements XSDSchemaLocationResolver {

    String namespace;

    public RelativeSchemaLocationResolver(String namespace) {
        if (namespace == null) {
            throw new NullPointerException("namespace must not be null");
        }
        this.namespace = namespace;
    }

    @Override
    public String resolveSchemaLocation(XSDSchema xsdSchema, 
        String namespaceURI, String schemaLocationURI) {

        if (namespace.equals(namespaceURI)) {
            //ensure the raw uri is relative
            try {
                if (!new URI(schemaLocationURI).isAbsolute()) {
                    //resolve it relative to the schema location
                    String schemaLocation = xsdSchema.getSchemaLocation();
                    if (schemaLocation != null) {
                        int slash = schemaLocation.lastIndexOf('/');
                        if (slash != -1) {
                            return schemaLocation.substring(0, slash) + "/" + schemaLocationURI;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                //ignore
            }
        }

        return null;
        
    }
}
