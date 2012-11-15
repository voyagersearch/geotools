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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataAccessFactory.Param;

/**
 * A datasource factory using DBCP connection pool
 * 
 * @author Andrea Aime - TOPP
 * 
 *
 *
 *
 * @source $URL$
 */
public class DBCPDataSourceFactory extends AbstractDataSourceFactorySpi {

    public static final Param DSTYPE = new Param(DataSourceFactorySpi.DSTYPE.key,
        DataSourceFactorySpi.DSTYPE.getType(), "Must be DBCP", false);
    
    public static final Param USERNAME = new Param("username", String.class,
            "User name to login as", false);

    public static final Param PASSWORD = new Param("password", String.class,
            "Password used to login", false);

    public static final Param JDBC_URL = new Param("jdbcUrl", String.class,
            "The JDBC url (check the JDCB driver docs to find out its format)", true);

    public static final Param DRIVERCLASS = new Param("driverClassName", String.class,
            "The JDBC driver class name (check the JDCB driver docs to find out its name)", true);

    public static final Param MAXACTIVE = new Param("maxActive", Integer.class,
            "The maximum number of active connections in the pool", false, new Integer(10));

    public static final Param MAXIDLE = new Param("maxIdle", Integer.class,
            "The maximum number of idle connections in the pool", false, new Integer(1));

    public static final Param MINIDLE = new Param("minIdle", Integer.class,
            "The maximum number of idle connections in the pool", false);

    public static final Param MAXWAIT = new Param("maxWait", Integer.class,
            "Timeout to wait for a new connection, default is 20 seconds", false, 20);

    public static final Param POOLPS = new Param("poolPreparedStatements", Boolean.class,
            "Pool prepared statements", false);
    
    public static final Param MAXOPENPS = new Param("maxOpenPreparedStatements", Integer.class,
            "Pool prepared statements", false, new Integer(50));
    
    public static final Param VALIDATECONN = new Param("validateConnection", Boolean.class,
            "Check/validate connection before use", false);
    
    public static final Param VALIDATEQUERY = new Param("validateQuery", String.class,
        "Query to validate connection", false);

    public static final Param TESTONCREATE = new Param("testOnCreate", Boolean.class,
        "Whether to test the connection when creating it", false);

    private static final Param[] PARAMS = new Param[] { DSTYPE, DRIVERCLASS, JDBC_URL, USERNAME, PASSWORD,
            MAXACTIVE, MINIDLE, MAXIDLE, POOLPS, MAXWAIT, MAXOPENPS, VALIDATECONN, VALIDATEQUERY, TESTONCREATE };

    @Override
    public String getDataSourceId() {
        return "DBCP";
    }

    public DataSource createDataSource(Map params) throws IOException {
        return createNewDataSource(params);
    }
    
    public boolean canProcess(Map params) {
        return super.canProcess(params) && getDataSourceId().equals(params.get("dstype"));
    }

    public DataSource createNewDataSource(Map params) throws IOException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName((String) DRIVERCLASS.lookUp(params));
        dataSource.setUrl((String) JDBC_URL.lookUp(params));
        dataSource.setUsername((String) USERNAME.lookUp(params));
        dataSource.setPassword((String) PASSWORD.lookUp(params));

        Integer maxActive = (Integer) MAXACTIVE.lookUp(params);
        maxActive = maxActive != null ? maxActive : (Integer) MAXACTIVE.sample;
        dataSource.setMaxActive(maxActive);
        
        Integer maxIdle = (Integer) MAXIDLE.lookUp(params);
        maxIdle = maxIdle != null ? maxIdle : (Integer) MAXIDLE.sample;
        dataSource.setMaxIdle(maxIdle);
        
        Integer minIdle = (Integer) (Integer) MINIDLE.lookUp(params);
        if (minIdle != null) {
            dataSource.setMinIdle(minIdle.intValue());
        }

        // max wait
        Integer maxWait = (Integer) MAXWAIT.lookUp(params);
        if (maxWait != null && maxWait != -1) {
            dataSource.setMaxWait(maxWait * 1000);
        }

        Boolean poolPs = (Boolean) POOLPS.lookUp(params);
        if (poolPs != null && poolPs.booleanValue()) {
            dataSource.setPoolPreparedStatements(true);

            Integer maxOpenPs = (Integer) MAXOPENPS.lookUp(params);
            if (maxOpenPs != null && maxOpenPs > 0) {
                dataSource.setMaxOpenPreparedStatements(maxOpenPs);
            }
            if (maxOpenPs != null && maxOpenPs < 0) {
                dataSource.setPoolPreparedStatements(false);
            }
        }
       
        
        Boolean validateConn = (Boolean) VALIDATECONN.lookUp(params);
        String validateQuery = (String) VALIDATEQUERY.lookUp(params);
        
        if (validateConn != null && validateConn.booleanValue() && validateQuery != null) {
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery(validateQuery);
        }

        Boolean testOnCreate = (Boolean) TESTONCREATE.lookUp(params);
        if (testOnCreate == null || testOnCreate.booleanValue()) {
            // check the data source is properly setup by trying to gather a connection out of it
            Connection conn = null;
            try {
                conn = dataSource.getConnection();
            } catch (SQLException e) {
                throw new DataSourceException("Connection pool improperly set up: " + e.getMessage(), e);
            } finally {
                // close the connection at once
                if (conn != null)
                    try {
                        conn.close();
                    } catch (SQLException e) {
                }
            }
        }

        // some datastores might need this
        dataSource.setAccessToUnderlyingConnectionAllowed(true);

        configureDataSource(dataSource, params);
        return new DBCPDataSource(dataSource);
    }

    /**
     * Subclass hook method.
     */
    protected void configureDataSource(BasicDataSource dataSource, Map params) throws IOException {
    }

    public String getDescription() {
        return "A BDCP connection pool.";
    }

    public Param[] getParametersInfo() {
        return PARAMS;
    }

    public boolean isAvailable() {
        try {
            new BasicDataSource();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
