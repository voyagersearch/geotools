/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 */
package org.geotools.jdbc;

/**
 * Callback for {@link JDBCFeatureReader}.
 */
public interface JDBCReaderCallback {

  /**
   * Null callback. 
   */
  JDBCReaderCallback NULL = new JDBCReaderCallback() {};

  /**
   * Called when the reader is created.
   */
  default void init() {}

  /**
   * Called directly before the reader makes it's initial query to the database.
   */
  default void beforeQuery() {}

  /**
   * Called directly after the reader makes it's initial query to the database.
   */
  default void afterQuery() {}

  /**
   * Called when an error occurs making the initial query to the database.
   */
  default void queryError(Exception e) {}

  /**
   * Called before the reader makes a call to {@link java.sql.ResultSet#next()}.
   */
  default void beforeRow() {}

  /**
   * Called after the reader makes a call to {@link java.sql.ResultSet#next()}.
   * 
   * @param next Whether or not any more rows exist in the {@link java.sql.ResultSet}
   */
  default void afterRow(boolean next) {}

  /**
   * Called when an error occurs fetching the next row in the result set. 
   * @param e
   */
  default void rowError(Exception e) {}

  /**
   * Called after the last row from the reader {@link java.sql.ResultSet} is read.
   */
  default void finish() {}
}
