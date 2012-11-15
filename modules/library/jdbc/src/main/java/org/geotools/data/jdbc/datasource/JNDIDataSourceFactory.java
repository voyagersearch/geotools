/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.jdbc.datasource;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.geotools.data.DataSourceException;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.factory.GeoTools;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

/**
 * A datasource factory SPI doing JDNI lookups
 * @author Administrator
 *
 *
 *
 *
 * @source $URL$
 */
public class JNDIDataSourceFactory extends AbstractDataSourceFactorySpi {

    public static final String J2EERootContext = "java:comp/env/";

    public static final Param DSTYPE = new Param(DataSourceFactorySpi.DSTYPE.key,
            DataSourceFactorySpi.DSTYPE.getType(), "Must be JNDI", false);

    public static final Param JNDI_REFNAME = new Param("jdniReferenceName", String.class,
            "The path where the connection pool must be located", true);

    private static final Param[] PARAMS = new Param[] { DSTYPE, JNDI_REFNAME };

    public DataSource createDataSource(Map params) throws IOException {
        return createNewDataSource(params);
    }
    
    public boolean canProcess(Map params) {
        return super.canProcess(params) && getDataSourceId().equals(params.get("dstype"));
    }

    public DataSource createNewDataSource(Map params) throws IOException {
        String jndiName = (String) JNDI_REFNAME.lookUp(params);
        if (jndiName == null)
            throw new IOException("Missing " + JNDI_REFNAME.description);

        Context ctx = null;
        DataSource ds = null;        
        
        try {
            ctx = GeoTools.getInitialContext(GeoTools.getDefaultHints());
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
            
        try {    
            ds = (DataSource) ctx.lookup(jndiName);
        } catch (NamingException e1) {
            // check if the user did not specify "java:comp/env" 
            // and this code is running in a J2EE environment
            try {
                if (jndiName.startsWith(J2EERootContext)==false)  {
                    ds = (DataSource) ctx.lookup(J2EERootContext+jndiName);
                    // success --> issue a waring
                    Logger.getLogger(this.getClass().getName()).log(
                                Level.WARNING,"Using "+J2EERootContext+jndiName+" instead of " +
                                jndiName+ " would avoid an unnecessary JNDI lookup");
                }    
            } catch (NamingException e2) {
                // do nothing, was only a try
            }                            
        }    
        
        if (ds == null)
            throw new IOException("Cannot find JNDI data source: " + jndiName);
        else
            return ds;
    }

    @Override
    public String getDataSourceId() {
        return "JNDI";
    }

    public String getDescription() {
        return "A JNDI based DataSource locator. Provide the JDNI location of a DataSource object in order to make it work";
    }

    public Param[] getParametersInfo() {
        return PARAMS;
    }

    /**
     * Make sure a JNDI context is available
     */
    public boolean isAvailable() {
        try {
            GeoTools.getInitialContext(GeoTools.getDefaultHints());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
