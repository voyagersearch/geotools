/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.shapefile;

import java.util.Date;
import org.geotools.feature.type.DateUtil;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.Hints;


/**
 * Converter used by {@link ShapefileDumperTest}.
 */
public class ShapefileDumperTestConverterFactory implements ConverterFactory {
    @Override
    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if (source == String.class && Date.class.isAssignableFrom(target)) {
            return new StringToDateConverter();
        }
        return null;
    }
}

/**
 * Simple converter that calls 
 */
class StringToDateConverter implements Converter {

    @Override
    public <T> T convert(Object source, Class<T> target) throws Exception {
        return target.cast(new Date(DateUtil.parseDateTime(source.toString())));
    }
}
