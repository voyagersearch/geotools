package org.geotools.data.ogr.bridj;

import org.geotools.data.ogr.GeometryMapperTest;
public class BridjGeometryMapperTest extends GeometryMapperTest {

    protected BridjGeometryMapperTest() {
        super(new BridjOGRDataStoreFactory());
    }

    @Override
    protected void setUp() throws Exception {
        GdalInit.init();
    }

}
