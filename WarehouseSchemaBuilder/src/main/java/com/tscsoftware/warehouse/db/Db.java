/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;




import com.tscsoftware.warehouse.WarehouseException;
import com.tscsoftware.warehouse.cfg.*;

/**
 * Handles a database connection and is capable of copying data from another database
 * into this one according to the set configuration.  Implements the minimum amount
 * of functionality necessary for database "plugins" to use.
 * 
 * To use:
 * 	1 - Call constructor, give configuration file (or call init() afterwards).
 * 	2 - Call copy and pass in a source database object.
 *  3 - Call close connection.
 * 
 * @author Jason S
 */
public abstract class Db {

	protected static final short 	MAX_CONNECTION_RETRY 	= 3;	// does not apply to initial connection
	protected static final short 	CONNECTION_RETRY_TIME 	= 10000;	// 10 seconds
	
	protected Logger		_logger;
	
	protected String		_name;
	private String		_driver;
	private String		_connectionURL;
	protected Config 		_cfg;
	protected Connection	_conn;
	protected Statement		_stmntReadOnly;
	protected Statement		_stmntWriteable;
	private HashMap 		_newTableMap = null;
	protected ArrayList		_noDropList = null;
	
	
	/* *******************************************************************************************
	 * CONSTRUCTION & DESTRUCTION
	 * *******************************************************************************************/
	
	
	/**
	 * Alternate constructor, does no initialization.  Another call to init(cfg, dbName) is required.
	 * Useful if class is instantiated at runtime.
	 */
	public Db(){
		_logger = Logger.getLogger(Db.class);
	}
	
	/**
	 * Constructor, initializes Db object with configuration information and connects to the database.
	 * 
	 * @param cfg Current Config object.
	 * @param dbName Name of database whose configuration information is to be used
	 * 
	 * @throws WarehouseException Any error encountered.
	 */
	public Db(Config cfg, String name, String driver, String connectionURL) throws WarehouseException{
		_logger = Logger.getLogger(Db.class);
		init (cfg, name, driver, connectionURL);
	}
	
	public void init(Config cfg, String name, String driver, String connectionURL) throws WarehouseException{
		_logger = Logger.getLogger(Db.class);
		
		_name = name;
		_driver = driver;
		_connectionURL = connectionURL;
		_cfg = cfg;
		
		getConnection();
	}
	
	/**
	 * Gets a connection to the database using the provided configuration information.
	 * 
	 * @throws WarehouseException
	 */
	private void getConnection() throws WarehouseException{
		//String driver = "";
		//String script = "";
		
		// get the connection information
		if(_name.compareTo(_cfg.getDstName()) == 0){
			//driver = _cfg.getDstDriver();
			//script = _cfg.getDstScript();
		}
		else if(_name.compareTo(_cfg.getSrcName()) == 0){
			//driver = _cfg.getSrcDriver();
			//script = _cfg.getSrcScript();
		}
		else{
			throw new WarehouseException("Database name " + _name + " not found in confiruation file.");
		}
		
		// make connection
	    try {
	    	Class.forName(_driver);
	    	_conn = DriverManager.getConnection(_connectionURL);
	    	_stmntWriteable	= _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			_stmntReadOnly 	= _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	    }
	    catch (Exception e) {
	    	//System.out.println("_driver " + _driver);
	    	//System.out.println("_connectionURL " + _connectionURL);
	    	
	    	throw new WarehouseException("Can't create connection to database " + _name + ".", e);
	    }
	}
	
	/**
	 * Safely try to reconnecto to the database.
	 * 
	 * @throws WarehouseException Any error encountered.
	 */
	public void reestablishConnection() throws WarehouseException{
		_logger.info("Re-establishing " + _name + "database connection");
		
		// release a connection if we're still connected
		close();
		
		// re-establish
		getConnection();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		// double check that connection has been closed
		close();
		
		super.finalize();
	}
	
	/**
	 * Removes the connection to the database.  Call this method when the object is no
	 * longer required.
	 */
	public void close(){
		try{
			if(_conn != null)
				_conn.close();
		}
		catch(Exception e){}
	}
	
	
	/* *******************************************************************************************
	 * COPY METHODS
	 * *******************************************************************************************/
	/**
	 * Copy data from the source database based on the configuration information we
	 * have.
	 * 
	 * @param dbSrc Source database to copy from.
	 * @throws Exception Any error encountered.
	 * @throws WarehouseException If connection to database cannot be made.
	 */
	public void copy(Db dbSrc) throws Exception {
		
		ResultSet tables 	= null;
		int tablesCopied 	= 0;
		ArrayList tablesList = new ArrayList();
		ColTypes colTypes 	= null;
		ArrayList createdTables = new ArrayList();
		ArrayList unsuccesfulTables = new ArrayList();

		try{
			// get a listing of all tables in the source
			DatabaseMetaData srcMeta = dbSrc.getMetaData();
			tables = srcMeta.getTables(null, null, "%", 
					new String[]{"TABLE"});	// this isn't necessarily universal
			colTypes = new ColTypes(getMetaData().getTypeInfo());
			
			// add status table to warehouse
			addWarehouseTable(colTypes);
			// update time status
			DateFormat dateFormat = new SimpleDateFormat(); 
			String startTime = dateFormat.format(new Date(System.currentTimeMillis()));
			addStatus("START TIME", startTime);
			
			try{
				tables.beforeFirst();
			}
			catch(SQLException e){
				// should start on the right row anyway, just making sure
			}
			
			while(tables.next()){
				tablesList.add(tables.getString("TABLE_NAME"));
			}
			tables.close();
			tables = null;
			// make global hashMap of tables in config file by newname to be used in this method
			// and in copyTable method
			_newTableMap = _cfg.getTablesByNewName();
			// allow reconnection to database if connection lost
			// TODO make configuratble
			int reconnectsRemaining = 3;
			
			// loop through each table
			_logger.info(tablesList.size() + " tables in source database " + dbSrc.getName() + ".");

//*LH* - TODO: change to:
//1. copy all tables defined in config file
//2. copy all source tables if required
//*RN* - 2008-01-15 - Suggested approach - Adding list of destination table names already
// processed. Then create list of "newName fields, minus those already process and
// loop through those, processing in the same manner as below
			for(int curTable = 0; curTable < tablesList.size(); curTable++){
				// check if we should copy this table
				// TODO make case insensitive
				if(_cfg.getCopyAllTables() || _cfg.getCfgTable((String)tablesList.get(curTable)) != null){
					try{
						String newName = _cfg.getCfgTable((String)tablesList.get(curTable)).getNewName();
						CfgTable newCfgTable = null;
						boolean newNameBoolean = false;
						// if a newName attribute exist on this table, use it to get the table properties
						if(newName != null){
							newNameBoolean = true;
							newCfgTable= (CfgTable)_newTableMap.get(newName);
						}
						String tableName = "";
						if(newNameBoolean){
							tableName = newName;
						}else{
							tableName = tablesList.get(curTable).toString();
						}
//						if(copyTable((String)tablesList.get(curTable), dbSrc, colTypes,newNameBoolean,newCfgTable)){
						if(copyTable(tableName, dbSrc, colTypes,newNameBoolean,newCfgTable)){
							createdTables.add(tableName);
							tablesCopied++;
							// TODO make configurable
							// reset retries for each table
							reconnectsRemaining = 3;
						}else{
							unsuccesfulTables.add(tableName);
						}
						
					}
					catch(SQLException e){
						_logger.error("Database error occurred.", e);
					}
					catch(WarehouseException e){
						_logger.error("Database error occurred.", e);
					}
					catch(Exception e){
						_logger.error("Unknown database error occurred.", e);
						if(reconnectsRemaining > 0){
							_logger.info("Attempting reconnect...", e);
							// TODO add configurable time to wait before each retry
							int sleepTime = 2000; // in ms
							long reconnectStartTime = System.currentTimeMillis();
							while(sleepTime + reconnectStartTime > System.currentTimeMillis());
							
							// attempt reconnect
							reconnectsRemaining--;
							curTable--;
							reestablishConnection();
							dbSrc.reestablishConnection();
						}
					}
				}
				
				//dbSrc.close();
			}
			
			// TODO: process new table entries
			// <new program logic goes here>

			java.util.Iterator newTableIt = _newTableMap.keySet().iterator();
			String tableName = "";
			while(newTableIt.hasNext()){
				tableName = newTableIt.next().toString();
				// skip if table has already been created
				if(createdTables.contains(tableName) || unsuccesfulTables.contains(tableName))continue;
				CfgTable newCfgTable= (CfgTable)_newTableMap.get(tableName);
				try{
					if(copyTable(tableName, dbSrc, colTypes,true,newCfgTable)){
						createdTables.add(tableName);
						tablesCopied++;
						// TODO make configurable
						// reset retries for each table
						reconnectsRemaining = 3;
					}
				}
				catch(SQLException e){
					_logger.error("Database error occurred.", e);
				}
				catch(WarehouseException e){
					_logger.error("Database error occurred.", e);
				}
				catch(Exception e){
					_logger.error("Unknown database error occurred.", e);
					if(reconnectsRemaining > 0){
						_logger.info("Attempting reconnect...", e);
						// TODO add configurable time to wait before each retry
						int sleepTime = 2000; // in ms
						long reconnectStartTime = System.currentTimeMillis();
						while(sleepTime + reconnectStartTime > System.currentTimeMillis());
						
						// attempt reconnect
						reconnectsRemaining--;
//						curTable--;
						reestablishConnection();
						dbSrc.reestablishConnection();
					}
				}
				
			}
			_logger.info("Tables not created: "+unsuccesfulTables);
			String endTime = dateFormat.format(new Date(System.currentTimeMillis()));
			addStatus("END TIME", endTime);
		}
		catch(SQLException e){
			throw e;
		}
		finally{
			if(tables != null){
				try{
					tables.close();
					tables = null;
				}catch(Exception e){}
			}
		}
		
		_logger.info("Copied " + tablesCopied + " tables.");
		
	}

	
	/**
	 * Copy data from the source database based on the configuration information we
	 * have.
	 * 
	 * @param dbSrc Source database to copy from.
	 * @throws Exception Any error encountered.
	 * @throws WarehouseException If connection to database cannot be made.
	 */
/*	public void copySave(Db dbSrc) throws Exception {
		
		ResultSet tables 	= null;
		int tablesCopied 	= 0;
		ArrayList tablesList = new ArrayList();
		ColTypes colTypes 	= null;

		try{
			// get a listing of all tables in the source
			DatabaseMetaData srcMeta = dbSrc.getMetaData();
			tables = srcMeta.getTables(null, null, "%", 
					new String[]{"TABLE"});	// this isn't necessarily universal
			colTypes = new ColTypes(getMetaData().getTypeInfo());
			
			// add status table to warehouse
			addWarehouseTable(colTypes);
			// update time status
			DateFormat dateFormat = new SimpleDateFormat(); 
			String startTime = dateFormat.format(new Date(System.currentTimeMillis()));
			addStatus("START TIME", startTime);
			
			try{
				tables.beforeFirst();
			}
			catch(SQLException e){
				// should start on the right row anyway, just making sure
			}
			
			while(tables.next()){
				tablesList.add(tables.getString("TABLE_NAME"));
			}
			tables.close();
			tables = null;
			
			// allow reconnection to database if connection lost
			// TODO make configuratble
			int reconnectsRemaining = 3;
			
			// loop through each table
			_logger.info(tablesList.size() + " tables in source database " + dbSrc.getName() + ".");

//*LH* - TODO: change to:
//1. copy all tables defined in config file
//2. copy all source tables if required
			
			for(int curTable = 0; curTable < tablesList.size(); curTable++){
				
				// check if we should copy this table
				// TODO make case insensitive
				if(_cfg.getCopyAllTables() || _cfg.getCfgTable((String)tablesList.get(curTable)) != null){
					try{
						if(copyTable((String)tablesList.get(curTable), dbSrc, colTypes)){
							tablesCopied++;
							// TODO make configurable
							// reset retries for each table
							reconnectsRemaining = 3;
						}
					}
					catch(SQLException e){
						_logger.error("Database error occurred.", e);
					}
					catch(WarehouseException e){
						_logger.error("Database error occurred.", e);
					}
					catch(Exception e){
						_logger.error("Unknown database error occurred.", e);
						if(reconnectsRemaining > 0){
							_logger.info("Attempting reconnect...", e);
							// TODO add configurable time to wait before each retry
							int sleepTime = 2000; // in ms
							long reconnectStartTime = System.currentTimeMillis();
							while(sleepTime + reconnectStartTime > System.currentTimeMillis());
							
							// attempt reconnect
							reconnectsRemaining--;
							curTable--;
							reestablishConnection();
							dbSrc.reestablishConnection();
						}
					}
				}
				
				//dbSrc.close();
			}
			
			// TODO: process new table entries
			// <new program logic goes here>
			
			String endTime = dateFormat.format(new Date(System.currentTimeMillis()));
			addStatus("END TIME", endTime);
		}
		catch(SQLException e){
			throw e;
		}
		finally{
			if(tables != null){
				try{
					tables.close();
					tables = null;
				}catch(Exception e){}
			}
		}
		
		_logger.info("Copied " + tablesCopied + " tables.");
		
	}
	*/
	
	/**
	 * Copy the table using the configuration parameters and defaults.  If either parameter
	 * is null, function returns.
	 * 
	 * @param tableName Name of table to be copied.
	 * @param dbSrc Source database to copy table from.
	 * @param colTypes Column types supported by the JDBC driver for this database.
	 * @return True if table created and copied, else false.
	 * 
	 * @throws Exception Any other error encountered while copying data.
	 */
	protected boolean copyTable(String tableName, Db dbSrc, ColTypes colTypes,boolean newName,
			CfgTable newCfgTable) throws Exception {
		if(tableName == null || dbSrc == null) return false;

		// figure out what our config settings are
		boolean tableCreated = true;
		boolean copyData = _cfg.getCopyData();
		boolean createTable = _cfg.getCopyAllTables();
		CfgTable cfgTable = null;
// if processing by newName CfgTable will be passed in (not null)
		if(newName == false && newCfgTable == null){
			cfgTable = _cfg.getCfgTable(tableName);
		}else{
			cfgTable = newCfgTable;
		}
		if(cfgTable != null){
			if(cfgTable.getCreateTable() != null){
			createTable = cfgTable.getCreateTable().booleanValue();
			}
			if(cfgTable.getCopyData() != null){
				copyData = cfgTable.getCopyData().booleanValue();
			}
		}
		
		if(!copyData && !createTable) return false;	// nothing to do
		
		Table srcTable = null;
		Table dstTable = null;
		
		try{
			// setup tables
			if(newName == false || newCfgTable == null){
				srcTable = dbSrc.getTable(tableName);
				dstTable = getTable(tableName);
			}else{
				String originalTableName = cfgTable.getName();
				srcTable = dbSrc.getTable(originalTableName,cfgTable);
				dstTable = getTable(tableName,cfgTable);
				
			}
			// TODO make number of retries configurable
			int copyRetries = 3;
			while(copyRetries > 0){
			
				// create table
				if(createTable){
					tableCreated = dstTable.createTable(srcTable, colTypes);
				}
			
				if(tableCreated){
					// create indices
					if(createTable && (_cfg.getCopyIndices() || (
							cfgTable != null && cfgTable.getNumIndices() > 0))){
						dstTable.createIndices(srcTable);
					}
					
					// copy data
					if(copyData){
						
						try{
							dstTable.copyData(srcTable);
							copyRetries = 0; // no errors, don't retry
						}
						catch(WarehouseException e){
							// no sql or critical exception, but failed to copy, try again
							copyRetries--;
							if(copyRetries > 0)
								_logger.warn("Error occurred copying data, retrying...");
							else
								_logger.error("Could not copy data into table " + _name);
							// TODO make delay configurable
							int sleepTime = 3000;
							long startTime = System.currentTimeMillis();
							while(startTime + sleepTime > System.currentTimeMillis());
						}
					}
					else{
						copyRetries = 0;
					}
				}
				else{
					copyRetries--;
					if(copyRetries > 0)
						_logger.warn("Error occurred creating table, retrying...");
					else
						_logger.error("Could not create table " + _name);
					// TODO make delay configurable
					int sleepTime = 3000;
					long startTime = System.currentTimeMillis();
					while(startTime + sleepTime > System.currentTimeMillis());
				}
			}
		}
		catch(Exception e){
			throw e;
		}
		finally{
			// finalize tables
			if(srcTable != null){
				try{
					srcTable.close();
					srcTable = null;
				}catch(Exception e){}
			}
			if(dstTable != null){
				try{
					dstTable.close();
					dstTable = null;
				}catch(Exception e){}
			}
		}
		
		return tableCreated;
	}

	/**
	 * Copy the table using the configuration parameters and defaults.  If either parameter
	 * is null, function returns.
	 * 
	 * @param tableName Name of table to be copied.
	 * @param dbSrc Source database to copy table from.
	 * @param colTypes Column types supported by the JDBC driver for this database.
	 * @return True if table created and copied, else false.
	 * 
	 * @throws Exception Any other error encountered while copying data.
	 */
	protected boolean copyTableSave(String tableName, Db dbSrc, ColTypes colTypes) throws Exception {
		if(tableName == null || dbSrc == null) return false;
		
		// figure out what our config settings are
		boolean tableCreated = true;
		boolean copyData = _cfg.getCopyData();
		boolean createTable = _cfg.getCopyAllTables();
		CfgTable cfgTable = _cfg.getCfgTable(tableName);
		if(cfgTable != null){
			if(cfgTable.getCreateTable() != null){
				createTable = cfgTable.getCreateTable().booleanValue();
			}
			if(cfgTable.getCopyData() != null){
				copyData = cfgTable.getCopyData().booleanValue();
			}
		}
		
		if(!copyData && !createTable) return false;	// nothing to do
		
		Table srcTable = null;
		Table dstTable = null;
		
		try{
			// setup tables
			srcTable = dbSrc.getTable(tableName);
			dstTable = getTable(tableName);
			
			// TODO make number of retries configurable
			int copyRetries = 3;
			while(copyRetries > 0){
			
				// create table
				if(createTable){
					tableCreated = dstTable.createTable(srcTable, colTypes);
				}
			
				if(tableCreated){
					// create indices
					if(createTable && (_cfg.getCopyIndices() || (
							cfgTable != null && cfgTable.getNumIndices() > 0))){
						dstTable.createIndices(srcTable);
					}
					
					// copy data
					if(copyData){
						
						try{
							dstTable.copyData(srcTable);
							copyRetries = 0; // no errors, don't retry
						}
						catch(WarehouseException e){
							// no sql or critical exception, but failed to copy, try again
							copyRetries--;
							if(copyRetries > 0)
								_logger.warn("Error occurred copying data, retrying...");
							else
								_logger.error("Could not copy data into table " + _name);
							// TODO make delay configurable
							int sleepTime = 3000;
							long startTime = System.currentTimeMillis();
							while(startTime + sleepTime > System.currentTimeMillis());
						}
					}
					else{
						copyRetries = 0;
					}
				}
				else{
					copyRetries--;
					if(copyRetries > 0)
						_logger.warn("Error occurred creating table, retrying...");
					else
						_logger.error("Could not create table " + _name);
					// TODO make delay configurable
					int sleepTime = 3000;
					long startTime = System.currentTimeMillis();
					while(startTime + sleepTime > System.currentTimeMillis());
				}
			}
		}
		catch(Exception e){
			throw e;
		}
		finally{
			// finalize tables
			if(srcTable != null){
				try{
					srcTable.close();
					srcTable = null;
				}catch(Exception e){}
			}
			if(dstTable != null){
				try{
					dstTable.close();
					dstTable = null;
				}catch(Exception e){}
			}
		}
		
		return tableCreated;
	}
	
	
	/* *******************************************************************************************
	 * GET AND SET METHODS
	 * *******************************************************************************************/
	
	
	/**
	 * Gets the database's metadata.
	 * 
	 * @return DatabaseMetaData.
	 * @throws SQLException Any error encountered.
	 */
	public DatabaseMetaData getMetaData() throws SQLException{
		return _conn.getMetaData();
	}
	
	/**
	 * Gest the name of the database as specified in the XML configuration.
	 * 
	 * @return Name of database.
	 */
	public String getName(){
		return _name;
	}
	
	/**
	 * Get a Table from a database.
	 * 
	 * @param tableName Name of table to retrieve.
	 * @return Table retrieved, null on error.  Calling function must call table.close().
	 * @throws SQLException If unable to connect to database.
	 * @throws WarehouseException If unable to connect to database.
	 */
	public abstract Table getTable(String tableName) throws SQLException,WarehouseException;
	
	/**
	 * Get a Table from a database.
	 * 
	 * @param tableName Name of table to retrieve.
	 * @param cfg Configuration data to use for the table.
	 * @return Table retrieved, null on error.  Calling function must call table.close().
	 * @throws SQLException If unable to connect to database.
	 * @throws WarehouseException If unable to connect to database.
	 */
	public abstract Table getTable(String tableName, CfgTable cfg) throws SQLException,WarehouseException;
	
	
	/* *******************************************************************************************
	 * UTILITY METHODS
	 * *******************************************************************************************/
	
	/**
	 * Drop all tables in this database.
	 * 
	 * @throws SQLException Any SQL exception that occurs except for failing to drop a table.
	 */
	public void clearNewTables() throws SQLException{
		
		_logger.info("Deleting only tables specified for creation in " + _name + ".");
		_noDropList = _cfg.getTablesNotToDrop();
		
		Iterator tableIterator= _cfg.getTables().iterator();
		int count = 0;
		
		while (tableIterator.hasNext()){

			CfgTable cfgTable = (CfgTable )tableIterator.next();
			_logger.info(cfgTable.getName());

			String tableName = cfgTable.getName();
			Statement stmnt = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			try{
				_logger.info("nodropList:"+_noDropList);
				_logger.info("dropping table: "+tableName);
				if(!_noDropList.contains(tableName)){
					count++;
					stmnt.execute("drop table " + tableName);
				}
			}
			catch(SQLException e){
				String err = "Unable to drop table " + tableName + ".  Message from database: " + 
					e.getMessage();
				_logger.error(err, e);
			}
			finally{
				try{
					stmnt.close();
				}
				catch(Exception e){}
			}
		}

		_logger.debug("Removed " + count + " tables from " + _name + ".");

	} 

	
	
	/**
	 * Drop all tables in this database.
	 * 
	 * @throws SQLException Any SQL exception that occurs except for failing to drop a table.
	 */
	public void clearDb() throws SQLException{
		
		_logger.info("Deleting all tables from " + _name + ".");
		_noDropList = _cfg.getTablesNotToDrop();
		// get a listing of all tables
		DatabaseMetaData metadata = _conn.getMetaData();
		ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"});	// this isn't necessarily universal
		
		int count=0;
		try{
			tables.beforeFirst();
		}
		catch(SQLException e){
			// probably vortex or something else dumb, just continue. should start on the right row
		}
//		ArrayList tableList = new ArrayList();
		String tableName = "";
		// make list of table names to drop from meta data
//		while(tables.next()){
//			tableName = tables.getString("TABLE_NAME");
//			tableList.add(tableName);
//		}
//		HashMap newTableMap = _cfg.getTablesByNewName();
//		java.util.Iterator newTableIt = newTableMap.keySet().iterator();
//		// add list of table names to drop by newName
//		while(newTableIt.hasNext()){
//			tableName = newTableIt.next().toString();
//			tableList.add(tableName);
//		}
		// TO-DO in future release account for linked tables and drop in correct order
		
		// delete each table
//		java.util.Iterator tableIt = tableList.iterator();
//		while(tableIt.hasNext()){
		while(tables.next()){
//			count++;
//			tableName = tableIt.next().toString();
			tableName = tables.getString("TABLE_NAME");
			Statement stmnt = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
//			Statement stmnt = _conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			try{
				_logger.info("nodropList:"+_noDropList);
				_logger.info("dropping table: "+tableName);
				if(!_noDropList.contains(tableName)){
					count++;
					stmnt.execute("drop table " + tableName);
				}
			}
			catch(SQLException e){
				// couldn't drop table for some reason, report and continue
				//System.err.println("Unable to drop table " + tableName + ".  Message from database: "
				//		+ e.getMessage());
				String err = "Unable to drop table " + tableName + ".  Message from database: " + 
					e.getMessage();
				_logger.error(err, e);
				// TO-DO in future release have option to abort if table drop unsuccessful
			}
			finally{
				try{
					stmnt.close();
				}
				catch(Exception e){}
			}
		}
		tables.close();
		
		//System.out.println("Removed " + count + " tables.");
		_logger.debug("Removed " + count + " tables from " + _name + ".");
	} //clearDb
	
	/**
	 * Add warehouse status table to warehouse based on configuration.
	 *
	 * @param colTypes Types of columns supported by this database.
	 *
	 * @throws SQLExcpetion Any error encountered.
	 * @throws WarehouseException Any error encountered.
	 */
	protected void addWarehouseTable(ColTypes colTypes) throws SQLException, WarehouseException{
		// setup table configuration
		CfgTable cfgTable = new CfgTable();
		cfgTable.setName(_cfg.getDstWarehouseTable());
		cfgTable.setCopyData(false);
		cfgTable.setCreateTable(true);
		
		CfgColumn cfgCol = new CfgColumn();
		cfgCol.setName("STATUS_KEY");
		cfgCol.setIsPrimaryKey(true);
		cfgCol.setSqlType(Types.VARCHAR);
		cfgCol.setLength(255);
		cfgTable.addColumn(cfgCol);
		cfgCol = null;
		
		cfgCol = new CfgColumn();
		cfgCol.setName("STATUS_VALUE");
		cfgCol.setSqlType(Types.VARCHAR);
		cfgCol.setLength(255);
		cfgTable.addColumn(cfgCol);
		
		// create table
		Table warehouseTable = getTable(_cfg.getDstWarehouseTable(), cfgTable);
		//Table warehouseTable = new Table(_cfg.getDstWarehouseTable(), cfgTable, _conn, _stmntWriteable,
		//		_stmntReadOnly);
		warehouseTable.createTable(null, colTypes);
	}
	
	/**
	 * Adds the key and value to the warehouse table.
	 * 
	 * @param key Key used to retrieve value.
	 * @param value Value.
	 * @throws SQLException Any error encountered.
	 */
	protected void addStatus(String key, String value) throws SQLException{
		_stmntWriteable.executeUpdate("insert into " + _cfg.getDstWarehouseTable() +
				" (STATUS_KEY, STATUS_VALUE) values (\'" + key + "\',\'" + value + "\')");
	}
}
