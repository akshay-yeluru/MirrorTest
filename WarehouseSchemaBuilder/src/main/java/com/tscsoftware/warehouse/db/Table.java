/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.cfg.StrU;
import com.tscsoftware.warehouse.cfg.CfgColumn;
import com.tscsoftware.warehouse.cfg.CfgTable;
import com.tscsoftware.warehouse.WarehouseException;

import java.util.StringTokenizer;

/**
 * Processes all table handling such as copying, and provides information about a table in a database.
 * 
 * @author Jason S
 */
public abstract class Table {
	
	protected Logger		_logger;
	
	protected String		_name;
	protected CfgTable		_cfg;
	protected Connection	_conn;
	protected Statement		_stmnt;			// forward only, updatable statement created during construction of object
	protected Statement		_stmntReadOnly;	// forward only, read only
	protected String		_newName;
	/**
	 * Initialize object and set configuration data.
	 * @param name Table name.
	 * @param cfgTable Configuration information for this table.
	 * @param conn Connection to database.
	 * @param stmntWriteable Statment which is writable (may be forward only)
	 * @param stmntReadOnly Statment which is read only (may be forward only)
	 * @throws SQLException If unable to make a connection to the database.
	 */
	public Table(String name, CfgTable cfgTable, Connection conn, Statement stmntWriteable,
			Statement stmntReadOnly) throws SQLException{
		
		_logger 		= Logger.getLogger(Table.class);
		_cfg 			= cfgTable;
		_name 			= name;
		_conn 			= conn;
		_stmnt			= stmntWriteable;
		_stmntReadOnly	= stmntReadOnly;
		_newName		= name;
		
		if (_cfg.getNewName() != null)
			_newName = _cfg.getNewName();
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
		// right now, the only connection is to the database from the database object,
		// which we do not wish to close
	}
	
	
	/* *******************************************************************************************
	 * GET AND SET METHODS
	 * *******************************************************************************************/
	
	
	/**
	 * Retrieve a complete ResultSet which is read only and insensitive.
	 * 
	 * @return Read only ResultSet containing all data from this table.  The calling
	 * 			function is responsible for closing the ResultSet before closing / destroying this object.
	 * @throws SQLException Any error encountered.
	 */
	public ResultSet getAllData() throws SQLException{
		_logger.info("in getAllData query is: select * from " + _name + " where 1=1");
		return _stmntReadOnly.executeQuery("select * from " + _name + " where 1=1");
	}

	/** 
	 * Retrieve a ResultSet which is read only and insensitive.
	 * 
	 * @param whereClause SQL select WHERE clause.
	 * @return Read only ResultSet containing selected data from this table.  The calling
	 * 			function is responsible for closing the ResultSet before closing / destroying this object.
	 * @throws SQLException Any error encountered.
	 */
	public ResultSet getData(String selectStatement,String whereClause, String joinClause) throws SQLException{
		String strSQL = "select * from " + _name;
		if(selectStatement != null){
			strSQL = selectStatement;
		}
		if(joinClause != null){
			strSQL = strSQL +" join "+joinClause;
		}
		if(whereClause != null){
			strSQL = strSQL + " where "+whereClause;
		}
//		_logger.info("in getData sql query is:"+strSQL);
/*
		if(selectStatement != null && whereClause != null){
			return _stmntReadOnly.executeQuery(selectStatement+ " where "+whereClause);
		}else if(selectStatement == null){
			return _stmntReadOnly.executeQuery("select * from " + _name + " where " + whereClause);
		}else{
			// hopefully this will not happen - tables should be defined carefully
			return _stmntReadOnly.executeQuery(selectStatement);
		}
		*/
		return _stmntReadOnly.executeQuery(strSQL);
	}
	
/**
	 * Get the java.sql.Types value of a column in this table.
	 * 
	 * @param colNum Column number to retrieve.
	 * @return java.sql.Types value of column type.
	 * @throws SQLException Any error encountered.
	 */
	public int getColType(int colNum) throws SQLException{
		ResultSet emptyRs = _stmntReadOnly.executeQuery("select * from " + _name + " where 1=0");
		int sqlType = emptyRs.getMetaData().getColumnType(colNum);
		emptyRs.close();
		return sqlType;
	}
	
	/**
	 * Retrieve the tables primary keys.
	 * 
	 * @return ResultSet containing the primary keys as specified in DatabaseMetaData.getPrimaryKeys.  The calling
	 * 			function is responsible for closing the ResultSet before closing / destroying this object.  Null if
	 * 			primary keys are not supported or do not exist.
	 * @throws SQLException Any error encountered.
	 */
	public ResultSet getPrimaryKeys() throws SQLException{
		DatabaseMetaData dbMeta = _conn.getMetaData();
		try{
			return dbMeta.getPrimaryKeys(null, null, _name);
		}
		catch(SQLException e){
			return null;
		}
	}
	
	/**
	 * Retrieve the tables foreign keys.  The ResultSet contains more stuff, but importantly a PKCOLUMN_NAME,
	 * FKTABLE_NAME and a FKCOLUMN_NAME.
	 * 
	 * @return ResultSet containing the foreign keys as specified in DatabaseMetaData.getCrossReference.  The calling
	 * 			function is responsible for closing the ResultSet before closing / destroying this object.  Null if
	 * 			foreign keys are not supported or do not exist.
	 * @throws SQLException If an error occurs with the connection to the database.
	 */
	public ResultSet getForeignKeys() throws SQLException{
		DatabaseMetaData dbMeta = _conn.getMetaData();
		try{
			return dbMeta.getCrossReference(null, null , _name, null, null, null);
		}
		catch(SQLException e){
			return null;
		}
	}
	
	/**
	 * Retrieve the tables indices.
	 * 
	 * @return ResultSet containing the indices as specified in DatabaseMetaData.getIndexInfo().  The calling
	 * 			function is responsible for closing the ResultSet before closing / destroying this object.  Null if
	 * 			indices are not supported or do not exist.
	 * @throws SQLException If an error occurs with the connection to the database.
	 */
	public ResultSet getIndices() throws SQLException{
		DatabaseMetaData dbMeta = _conn.getMetaData();
		try{
			return dbMeta.getIndexInfo(null,null,_name,false,false);
		}
		catch(SQLException e){
			return null;
		}
	}
	
	/**
	 * Retrive the tables meta information.
	 * 
	 * @return ResultSetMetaData for this table.
	 * @throws SQLException Any error encountered.
	 */
	public ResultSetMetaData getMetaData() throws SQLException{
		ResultSet emptyRs = _stmntReadOnly.executeQuery("select * from " + _name + " where 1=0");
		ResultSetMetaData tmpMeta = emptyRs.getMetaData();
		//emptyRs.close();	// MS SQL will not allow access to meta data if this is closed
		// TODO read meta data into a class to return, instead of a result set
		return tmpMeta;
	}
	
	/**
	 * Get the name of the table.
	 * 
	 * @return Name of table.
	 */
	public String getName(){
		return _name;
	}
	
	/**
	 * Get the new name of the table.
	 * 
	 * @return newName of table.
	 */
	public String getNewName(){
		return _newName;
	}
	/**
	 * Get the number of rows in the table.
	 * 
	 * @return Number of rows in this table, -1 on error.
	 * @throws SQLException Any error encountered.
	 */
	public int getRowCount(boolean ignoreJoinAndWhere) throws SQLException{
		String strSQL = "select count(*) from " + _name;
// ignore join and where is used for comparing the final value after the copy finishes
// warehouse takes a count before starting the copy and after and subtracts the two 
// to ensure the count is accurate
		if(!ignoreJoinAndWhere){
			if(_cfg.getJoinClause() != null){
				strSQL += " join "+_cfg.getJoinClause();
			} 
		}		
		if(!ignoreJoinAndWhere){
			if(_cfg.getWhereClause() != null){
				strSQL += " where " + _cfg.getWhereClause();
			} 
		}
		_logger.debug("in getRowCount sql: "+strSQL);
//		ResultSet tmpRs = _stmntReadOnly.executeQuery("select count(*) from " + _name);
		ResultSet tmpRs = _stmntReadOnly.executeQuery(strSQL);
		if(tmpRs.next()){
			_logger.info("in getRowCount - rowCount is:"+tmpRs.getInt(1));
			return tmpRs.getInt(1);
		}
		else{
			_logger.warn("Unable to get row count from table " + _name);
			return -1;
		}
	}
	
	/**
	 * Get the number of rows in destination table.
	 * 
	 * @return Number of rows in this table, -1 on error.
	 * @throws SQLException Any error encountered.
	 */
	public int getRowCountDstTable() throws SQLException{
		ResultSet tmpRs = _stmntReadOnly.executeQuery("select count(*) from " + _newName);
		if(tmpRs.next()){
			return tmpRs.getInt(1);
		}
		else{
			_logger.warn("Unable to get row count from table " + _newName);
			return -1;
		}
	}
	
	/**
	 * UNIMPLEMENTED
	 * 
	 * Get the number of rows which match the given select statement.
	 * 
	 * @param where Standard SQL where expresion indicating which rows to select.
	 * @return Number of rows matching given where clause in this table.
	 */
	public int getRowCount(String where){
		// TODO
		return -1;
	}
	
	/* *******************************************************************************************
	 * CREATE / COPY METHODS
	 * *******************************************************************************************/
	
	
	/**
	 * Create a table using the srcTable as a template if no configuration information
	 * exists for this table.  Will not create a table if the stored configuration's createTable
	 * is set to false.
	 * 
	 * @param srcTable Table to copy.
	 * @param colTypes JDBC column types.
	 * @return true if table created, else false.
	 * @throws SQLException Any error encountered.
	 * @throws WarehouseException Any error encountered.
	 */
	public boolean createTable(Table srcTable, ColTypes colTypes) throws SQLException, WarehouseException{
		// check configuration
		// if we are supposed to use the defaults, assume this was checked when we called.  This is
		// just to double check that we are supposed to do this.
		if(_cfg != null && _cfg.getCreateTable() != null && _cfg.getCreateTable() == Boolean.FALSE){
			return false;
		}
		
		// drop table (make sure it doesn't already exist)
		try{
//			_stmnt.execute("drop table " + _name);
			_stmnt.execute("drop table " + _newName);
		}catch(SQLException e){}
		
		// create table
		
		// TO-DO add foreign keys
		ResultSet srcPrimaryKeys = null;
		try{
			String createSql;
			ResultSetMetaData srcMeta = null;
			if(srcTable != null){
				srcPrimaryKeys = srcTable.getPrimaryKeys();
				srcMeta = srcTable.getMetaData();
			}
			createSql = buildCreateTableStmnt(srcMeta, srcPrimaryKeys, null, colTypes);
//			_logger.debug("Creating table " + _name);
//			_logger.debug(">" + createSql);
//			_logger.info("Create table sql" + createSql);

/*
 * I will want to parameterize this so that I can write out those files.			
			writeTableDDL(_newName + ".ddl",createSql);
	*/
			
			_stmnt.execute(createSql);
//			_logger.debug("Created table " + _name);
		}
		catch(SQLException e){
			_logger.error("Unable to create table " + _newName, e);
			return false;
		}
		finally{
			if(srcPrimaryKeys != null){
				try{
					srcPrimaryKeys.close();
				}
				catch(Exception e){}
			}
		}
		
		return true;
	}
	
	public void writeTableDDL(String fileName, String ddl){
		try {
	          File file = new File(fileName);
	          BufferedWriter output = new BufferedWriter(new FileWriter(file));
	          output.write(ddl);
	          output.close();
	        } catch ( IOException e ) {
	           e.printStackTrace();
	        }
		
	}
	
	/**
	 * Create indices for this table using the srcTable as a template if no configuration
	 * information exists for this table.
	 * 
	 * @param srcTable Table to copy.
	 * @throws SQLException Any error encountered.
	 */
	public void createIndices(Table srcTable) throws SQLException{
		// get indices to be created (takes config into consideration)
//		Iterator sqlStrs = Index.buildCreateSql(srcTable, _cfg, _name);
		Iterator sqlStrs = Index.buildCreateSql(srcTable, _cfg, _newName);
		
		// create indices
		String sqlStr;
		while(sqlStrs.hasNext()){
			sqlStr = (String)sqlStrs.next();
			_logger.warn("sqlStr:"+sqlStr);		
			
			try{
				_stmnt.execute(sqlStr);
			}
			catch(SQLException e){
				_logger.warn("Unable to create index with SQL: " + sqlStr);
				_logger.error("Unable to create index with SQL: " + sqlStr, e);
			}
		}
	}
	
	/**
	 * Copy data from the srcTable to this table.
	 * 
	 * Will not copy data in a table if our stored configuration has copyData set to false.
	 * 
	 * @param srcTable Table to copy.
	 * @throws Exception Any error encountered.
	 */
	public void copyData(Table srcTable) throws Exception{
		// check configuration
		if(_cfg != null && _cfg.getCopyData() == Boolean.FALSE){
			return;
		}
		
		ResultSet srcData = null;
		PreparedStatement stmnt = null;
		
		// do some initial logging for copy
		long startTime = System.currentTimeMillis();
		//Date startDate = new Date(startTime);
		//DateFormat formattedDate = new SimpleDateFormat();
		int srcRowCount = 0;
		String doCopyCount = _cfg.getDoCopyCount();
		if(doCopyCount.trim().equalsIgnoreCase("Y")){
			srcRowCount = srcTable.getRowCount(false); 
		}
		int preCopyRowCount =0;
		if(doCopyCount.trim().equalsIgnoreCase("Y")){
			preCopyRowCount = getRowCount(true);
		}
_logger.debug("preCopyRowCount:"+preCopyRowCount);		
		int lastRow = 0;
		int startBatch = 0;
		int endBatch = 0;
		_logger.info("Copying " + srcTable.getName() + " as "+srcTable.getNewName()+" : " + srcRowCount + " rows. doCopyCount:"+doCopyCount); // at " + 
		//_logger.debug("Copying " + srcTable.getName() + ": " + srcRowCount + " rows."); // at " + 
		//		formattedDate.format(startDate));
			
		// copy data
		try{
			
			// get source data
//			srcData = srcTable.getAllData();

			if(_cfg.getWhereClause() != null || _cfg.getSelectStatement() != null || _cfg.getJoinClause() != null){
				// B2K: Check if a WHERE clause should be used when collecting the data. eg: where year > '2005'
_logger.debug("selecting data with WHERE clause [" + _cfg.getWhereClause() + "]");
				srcData = srcTable.getData(_cfg.getSelectStatement(),_cfg.getWhereClause(),_cfg.getJoinClause());
			} 
			else{
_logger.debug("selecting ALL data");
				srcData = srcTable.getAllData();
			}
			
			try{
				srcData.beforeFirst();	// just to make sure
			} catch(Exception e){}
			
			// get statement
			int colCount = 0;
			int srcColCount = 0;
//			String statement = "insert into " + _name + "(";
			String statement = "insert into " + _newName + "(";
			boolean useSrcCols = _cfg == null || _cfg.getAllSrcColumns().booleanValue();
			boolean useCfgCols = _cfg != null; 
			if(useSrcCols){
				// use columns from source database
				srcColCount = srcData.getMetaData().getColumnCount();
				colCount += srcColCount;
			}
			if(useCfgCols){
				colCount += _cfg.getNumColumns();
			}
			// build column names
			if(useSrcCols){
				statement += srcData.getMetaData().getColumnName(1);
				for (int i = 1; i < srcColCount; i++) {
				    statement += ", " + srcData.getMetaData().getColumnName(i + 1);
				}
			}
			if(useCfgCols){
				for (int i = 0; i < _cfg.getNumColumns(); i++) {
					// B2K: add new columns to the destination table
					if(statement.endsWith("(")){
						statement += _cfg.getCfgColumn(i).getName();
					}
					else{
						statement += ", " + _cfg.getCfgColumn(i).getName();
					}
				}
			}
			// build column values
			statement += ") values (?";
			for (int i = 1; i < colCount; i++) {
			    statement += ", ?";
			}	
		    statement += ")";
//		    _logger.info("copy data prepared statement: " + statement);
		    _conn.setAutoCommit(false);
			stmnt = _conn.prepareStatement(statement);
			String debug = _cfg.getDebugTable();
			// add rows
			int rowCount = 0;
			int [] updatesRslt;
			while(srcData.next()){
				startBatch = endBatch;
				rowCount++;
				lastRow = rowCount;
				// copy each column
				// add data from source database columns
				for(int curCol = 1; curCol <= colCount; curCol++){
					Object srcDataObject = null;
					if(useSrcCols && curCol <= srcColCount){
						srcDataObject = srcData.getObject(curCol);
					}
					else{
						int curCfgCol = curCol - srcColCount - 1;
//						_logger.debug("getting cfg column #" + curCfgCol);
						if(_cfg.getCfgColumn(curCfgCol).getValue() != null){
							// B2K: this is a static value in a new column. (e.g. payID='TEAC')
							srcDataObject = _cfg.getCfgColumn(curCfgCol).getValue(); 
//							_logger.error("here 1 in if srcDataObject:"+srcDataObject);
						}
						else{
//							_logger.debug("here 1 in else");
							Object copyValue = null;
							if (_cfg.getCfgColumn(curCfgCol).getCopyFrom() != null){
//								_logger.debug("here 2 in else");
								// B2K: copy a value from another source column name.
								// this may also be used to change the column size or data type
								
								// check if copy from requires concatenating multiple columns
								// e.g. copyFrom="GL_00,GL_01,GL_02,GL_03,GL_04"
								if (_cfg.getCfgColumn(curCfgCol).getCopyFrom().contains(",")) {
//									_logger.error("here 3 in else");
									copyValue = "";
									// parse each column name and append the value to copyValue
									StringTokenizer st = new StringTokenizer(_cfg.getCfgColumn(curCfgCol).getCopyFrom(),",");
									while (st.hasMoreTokens()) {
//										_logger.debug("here 4 in else");
										String newValue = "";
										String nextToken = st.nextToken();
										newValue = copyValue.toString();
//										_logger.debug("here 4a in else newValue:"+newValue);
										// adding try to see if I can see what is wrong - getting null pointer exceptions in some data
										// rn - talk to Les about how this should work
//										try{
										if(srcData.getObject(nextToken) == null){
//											_logger.info("getcopyFrom: "+_cfg.getCfgColumn(curCfgCol).getCopyFrom());
											newValue += "";
										}else{
											newValue += srcData.getObject(nextToken).toString();
										}
//										}catch(Exception e){
//											_logger.info("error getting string:"+e.getMessage()+"; newValue: "+newValue);
//										}
//										newValue += srcData.getObject(st.nextToken()).toString();

										copyValue = newValue;
//										_logger.debug("here 4b in else copyValue:"+copyValue);
									}
								}
								else {
//									_logger.debug("here 5 in else:"+_cfg.getCfgColumn(curCfgCol).getCopyFrom());
										
									copyValue = srcData.getObject(_cfg.getCfgColumn(curCfgCol).getCopyFrom());
//									_logger.debug("here 5a in else copyValue:"+copyValue);
								}
							}
							else{
//								_logger.debug("here 6 in else");
								// load value from source column with the same column name
								copyValue = srcData.getObject(_cfg.getCfgColumn(curCfgCol).getName());
//								_logger.debug("here 6a in else copyValue:"+copyValue);
							}

							if (_cfg.getCfgColumn(curCfgCol).getStartEndPos() != null){
								// extract a segment of the String. (e.g. "0,6" extracts the left 6 chars)
//_logger.info("stendpos: "+_cfg.getCfgColumn(curCfgCol).getStartEndPos());								
								StringTokenizer st=new StringTokenizer(_cfg.getCfgColumn(curCfgCol).getStartEndPos(),",");
								int stPos = (new Integer(st.nextToken())).intValue();
								int enPos = (new Integer(st.nextToken())).intValue();
//_logger.debug("stPos:"+stPos+"; enPos:"+enPos+"; value:"+copyValue);

//								String xyz = copyValue.toString().substring(stPos,enPos);
						
//_logger.info("Data value before [" + copyValue.toString() + "], after [" + xyz + "]");
								if(copyValue == null){
									copyValue = "";
								}else{
//_logger.debug("copyValue size: " +copyValue.toString().length());
									copyValue = copyValue.toString().substring(stPos,enPos);
								}
							}
							
							if (_cfg.getCfgColumn(curCfgCol).getBitYN() != null){
								// transform bit data into YN value
								copyValue = convertBitToYN(copyValue,_cfg.getCfgColumn(curCfgCol).getBitYN());
							}
							if (_cfg.getCfgColumn(curCfgCol).getBitYNSpecial() != null){
								// transform bit data into YN value
								copyValue= convertBitToYNSpecial(copyValue,_cfg.getCfgColumn(curCfgCol).getBitYNSpecial());
							}
							srcDataObject = copyValue;
						}
//						_logger.debug("set col" + curCol + " " + _cfg.getCfgColumn(curCfgCol).getName()
//								+ " val " + srcDataObject);
					}
					if(srcDataObject != null){
//						_logger.info("srcDataObject:"+srcDataObject.toString()+"!");
					}
					//_logger.debug("Set col " + curCol + ":" + srcDataObject); // TODO comment out
					if(srcDataObject == null){
						// replacing null values with blanks
						
//						stmnt.setNull(curCol, getColType(curCol));
						if(getColType(curCol) == 2){
							stmnt.setObject(curCol, 0);
//							stmnt.setNull(curCol, getColType(curCol));
						}else{
							stmnt.setObject(curCol, " ");
						}
					}
					else{
						if(srcDataObject instanceof String){
//							srcDataObject = StrU.rTrim(srcDataObject.toString());
							stmnt.setString(curCol, StrU.rTrim(srcDataObject.toString()));
						}else{
							stmnt.setObject(curCol, srcDataObject);
						}
					}
					if(debug.equalsIgnoreCase("Y")){
						if(curCol < 4){
							_logger.warn("Row:"+rowCount+"; srcData: " + srcDataObject+"; Table:"+_name);
							_logger.warn("colType: " + getColType(curCol));
						}
					}
				}
				
				// submit a batch every 50 rows whether finished or not
				stmnt.addBatch();
				if(debug.equalsIgnoreCase("Y")){
					endBatch = rowCount;
					updatesRslt = stmnt.executeBatch();
					_conn.commit();
					stmnt.clearBatch();
				}else{
					if((rowCount % 50) == 0){
						endBatch = rowCount;
						updatesRslt = stmnt.executeBatch();
						if(!checkBatchErrs(updatesRslt)){
							// no errors
							_conn.commit();
						}
						else{
							throw new WarehouseException("Unable to copy data to " + _newName + ".");
						}
						stmnt.clearBatch();
					}
				}				
				// TO-DO this should probably be configurable
				if((rowCount % 20000) == 0){
					_logger.debug("\t" + rowCount + " rows completed");
				}
			}
			endBatch = lastRow;
			// submit last batch
			updatesRslt = stmnt.executeBatch();
			if(!checkBatchErrs(updatesRslt)){
				// no errors
				_conn.commit();
			}
			else{
				throw new WarehouseException("Unable to copy data to " + _newName + ".");
			}

			// report progress
			long endTime = System.currentTimeMillis();
			float totalTime = (endTime - startTime) / 1000;
			int copiedRowCount = 0;
			if(doCopyCount.trim().equalsIgnoreCase("Y")){
				copiedRowCount = getRowCount(true);
				_logger.info("\t" + copiedRowCount + " rows copied in " + totalTime + " seconds.");
			}else{
				_logger.info("\t" + rowCount + " rows copied in " + totalTime + " seconds.");
				
			}
			
			int thisCount = copiedRowCount - preCopyRowCount;
			_logger.debug("thisCount:"+thisCount);
//			if(srcRowCount != copiedRowCount){
			if(doCopyCount.trim().equalsIgnoreCase("Y")){
				if(rowCount != thisCount){
					_logger.warn("Only " + rowCount + " of " + thisCount + " rows copied for table " +
							_name + ".");
	//				_logger.warn("Only " + copiedRowCount + " of " + srcRowCount + " rows copied for table " +
	//						_name + ".");
	
					// throw error to prompt for retry
					throw new WarehouseException("Only " + thisCount + " of " + rowCount +
							" rows copied for table " + _newName + ".");
	//				throw new WarehouseException("Only " + copiedRowCount + " of " + srcRowCount +
	//						" rows copied for table " + _newName + ".");
				}
			}			
		} // try
		catch(BatchUpdateException e){
			// not caught by a normal exception
			_logger.info("Batch update failed - row between :"+startBatch+" and "+endBatch);
//			int cCount = srcData.getMetaData().getColumnCount();
//			for(int a=1;a<cCount;a++){
//				_logger.info(srcData.getMetaData().getColumnName(a)+": "+srcData.getObject(a));
//			}
			_logger.error("SQL error occurred.", e);
		}
		catch(Exception e){
			_logger.info("failed row between :"+startBatch+" and "+endBatch);
			throw e;
		}
		finally{
			try{
				_conn.setAutoCommit(true);
			}catch(Exception e){
				_logger.error("Unable to set auto commit to true.", e);
			}
			
			if(srcData != null){
				try{
					srcData.close();
				}catch(Exception e){}
			}
			
			if(stmnt != null){
				try{
					stmnt.close();
				}catch(Exception e){}
			}
		}

	}
	
	
	/* *******************************************************************************************
	 * BUILD SQL STATEMENT METHODS
	 * *******************************************************************************************/
	
	
	/**
	 * Using the member configuration data and the supplied meta data, build a SQL create table statement
	 * as a String.
	 * 
	 * Both ResultSets passed in must be set to beforefirst or be scrollable.  When returned,
	 * position could be anywhere.
	 * 
	 * @param tableMetaData ResultSet of ResultSetMetaData containing the data of the table to be created.
	 * 			If there is configuration information (>0 column tags), only the specified columns will be added
	 * 			using the data from tableMetaData.  If null, only column tag data will be used.
	 * @param srcPrimaryKeys ResultSet containing all primary keys for this table.
	 * @param srcForeignKeys ResultSet containing all foreign keys for this table.  NOT IMPLEMENTED TO-DO
	 * @param colTypes Column types supported by this (destination) JDBC driver.
	 * @return SQL statement to create table.
	 * 
	 * @throws SQLException Any error encountered.
	 * @throws WarehouseException Any error encountered.
	 */
	protected String buildCreateTableStmnt(ResultSetMetaData tableMetaData, ResultSet srcPrimaryKeys,
			ResultSet srcForeignKeys, ColTypes colTypes) throws SQLException, WarehouseException{
		
		// build beginning
		String createSql = "create table " + _name + " (";
		
		String newName = _cfg.getNewName();
		if (newName != null){
			createSql = "create table " + newName + " (";
		}
		
		String primaryKeys = "";
		ArrayList addedColumns = new ArrayList();
//		_logger.info("varchar type number "+Types.VARCHAR+"; char type number:"+Types.CHAR+"; longvarchar type number:"+Types.LONGVARCHAR);
		// build columns
		if(tableMetaData != null){
			// build using source table as a template

String dbgInfo = "  " + "Table " + _name;
if (newName != null) dbgInfo += ", new table name " + newName;
_logger.debug(dbgInfo);

			int numCols = tableMetaData.getColumnCount();
			String colName;
			String colNameToCreate = "";
			
			for(int curCol = 1; curCol <= numCols; curCol++){
				// column name and type
				colName = tableMetaData.getColumnName(curCol);

//_logger.debug("\t" + "Col " + curCol + ", colName [" + colName + "]");

				// check if this column should be created
				if(_cfg == null || _cfg.getAllSrcColumns().booleanValue() ||
						_cfg.getCfgColumn(colName) != null){ //_cfg.getNumColumns() <= 0){

					colNameToCreate = colName;
					addedColumns.add(colName);
					
					String colType = buildCreateColType(tableMetaData, colTypes, curCol, colName);
//*LH* there is a bug in the above statement... it is setting the 34 char GL description to 72 char

					createSql += colName + " " + colType + " ";
_logger.debug("   - Col " + colName + ", type [" + colType + "]");
					
					// check for null
					if(tableMetaData.isNullable(curCol) == ResultSetMetaData.columnNoNulls){
						createSql += "not null ";
					}
					
					// check for primary key NOT TESTED, vortex doesn't have PKs
					if(srcPrimaryKeys != null){
						try{
							srcPrimaryKeys.beforeFirst();
						}
						catch(SQLException e){
							// can't go before first, probably already there
						}
						
					} // if primary keys
					
					//if(curCol < numCols)
					createSql += ", ";
					
				} // if making column
				
			} // for curCol
			/*
			//_logger.debug("\t" + "1. SQL> [" + createSql + "]");
			//_logger.debug("\t" + "1. Primary Keys> [" + primaryKeys + "]");	

			ResultSetMetaData rsmd = srcPrimaryKeys.getMetaData();
			rsmd.getColumnCount();
			for (int x =1; x <= rsmd.getColumnCount(); x++){
				_logger.debug("\t" + "1. MetaData> [" + rsmd.getColumnName(x) + "]");	
				
			}
			*/
			
			// add all primary keys
			if(srcPrimaryKeys != null){
				ArrayList arrayListPrimaryKeys = new ArrayList();
				while(srcPrimaryKeys.next()){
					//_logger.debug("\t" + "1. MetaData> [" + 
					//srcPrimaryKeys.getString("COLUMN_NAME") + " " +
					//srcPrimaryKeys.getString("KEY_SEQ") + " " +
					//srcPrimaryKeys.getString("PK_NAME") + " " +
					//"]");	
					String currentPrimaryKey = srcPrimaryKeys.getString("COLUMN_NAME");
					
					if (!arrayListPrimaryKeys.contains(currentPrimaryKey)){
						arrayListPrimaryKeys.add(currentPrimaryKey);
						// only add ',' after first column
						if(primaryKeys.length() > 0){
							primaryKeys += ",";
						}
						primaryKeys += srcPrimaryKeys.getString("COLUMN_NAME");
					}
				}
			}
			//_logger.debug("\t" + "2. Primary Keys> [" + primaryKeys + "]");
		}

		//_logger.debug("\t" + "2. SQL> [" + createSql + "]");
		// build remaining columns using column tags
		
		if(_cfg != null){
			int numCols = _cfg.getNumColumns();
			for(int curCol = 0; curCol < numCols; curCol++){
				// get column info
				CfgColumn cfgCol = _cfg.getCfgColumn(curCol);
				
				if(!addedColumns.contains(cfgCol.getName())){
					addedColumns.add(cfgCol.getName());
					
					// add name
					createSql += cfgCol.getName() + " ";	// TODO error check if name is empty
					//_logger.debug("\t" + "adding Column [" + cfgCol.getName() + "]");
					
					// add type and parameters
					createSql += buildCreateColType(null, colTypes, curCol, cfgCol.getName()) + " ";
				
					// add primary key
					if(cfgCol.isPrimaryKey()){
						if(primaryKeys.length() > 0){
							primaryKeys += ",";

						}
						primaryKeys += cfgCol.getName();
					}
					
					if(curCol < numCols-1)
						createSql += ", ";
				}
			}
			
//_logger.debug("\t" + "2. Create SQL> [" + createSql + "]");

// add primary keys
			if(primaryKeys.length() > 1){
//				createSql += ", PRIMARY KEY (" + primaryKeys + ") ";
				createSql += " PRIMARY KEY (" + primaryKeys + ") ";
//_logger.debug("\t" + "3. Primary Keys> [" + primaryKeys + "]");
			}
		}
//_logger.debug("\t" + "4. Create SQL> [" + createSql + "]");
		
		// build end
		// clip trailing ", "
		if(createSql.endsWith(", ")){
			createSql = createSql.substring(0, createSql.length() - 2);
		}
		createSql += ");";

// B2K TODO: check if this table requires a WHERE clause for data transformation
		
		_logger.debug("Create table with sql: " + createSql);
		
		return createSql;
	}
	
	/**
	 * Build create table syntax for the type specified in the source column
	 * 
	 * @param srcMeta Metadata of template to use to build syntax.  May be null if only using configuration
	 * 	information to build column.
	 * @param colTypes is the column types supported by the DB.
	 * @param colNum Number of column in srcMeta to build create syntax for.
	 * 
	 * @return String with proper syntax for the column portion of a create table statement.
	 * 
	 * @throws SQLException Any SQL exception encountered.
	 * @throws WarehouseException SQL type not found.
	 */
	protected String buildCreateColType(ResultSetMetaData srcMeta, ColTypes colTypes, 
			int colNum, String colName) throws SQLException, WarehouseException{
		String type = "";
		
		// get source/config parameters
		int sqlColType;
		int srcFieldSize;
		boolean srcFieldAutoNum;
		
		CfgColumn cfgCol = null;
		if(_cfg != null){
//_logger.info("_cfg is not null");
//			cfgCol = _cfg.getCfgColumn(colNum);     bug, cfg file lookup should be done by colName, not colNum
			cfgCol = _cfg.getCfgColumn(colName);
		}
		
/*		
 Question: what does colNum=current column in the metadata have to do with _cfg which is the 
 user-defined fields in the config file???  These are not related.
 Instead the cfg column should be checked for custom values
 If no custom values, the column name (or copyFrom) should be looked up in the metadata
*/	
		
		
//_logger.debug("colNum " + colNum + " " + cfgCol.getName());
		// get type
		if(cfgCol != null && cfgCol.getSqlType() != null){
			sqlColType = cfgCol.getSqlType().intValue();
//_logger.debug("     -- type loaded from config file, " + sqlColType);
		}
		else{
			sqlColType = srcMeta.getColumnType(colNum);
//_logger.debug("     -- type loaded from metadata, " + sqlColType);
		}
		
		// get width
		if(cfgCol != null && cfgCol.getLength() != null){
			// load size from config file
			srcFieldSize = cfgCol.getLength().intValue();
//_logger.debug("     -- size loaded from config file, " + srcFieldSize);
		}
		else{
			// load size from metadata
			srcFieldSize = getColLength(srcMeta, cfgCol, colNum);
//_logger.debug("     -- size loaded from metadata, " + srcFieldSize);
		}
		
		// get auto increment
		if(srcMeta == null){
			srcFieldAutoNum = false;
		}
		else{
			srcFieldAutoNum	= srcMeta.isAutoIncrement(colNum);
		}
//_logger.info("sqlColtype 1: "+sqlColType);
//_logger.debug("\t" + "1 sqlColType: "+sqlColType);

		// if varchar greater than 255 use longvarchar
		if(srcFieldSize > 255 && (sqlColType == Types.VARCHAR || sqlColType == Types.CHAR)){
			// attempt to use larger size
			sqlColType = Types.LONGVARCHAR;
		}
		
		// testing varchar size diff
		if(sqlColType == Types.CHAR)sqlColType = Types.VARCHAR;
	//	_logger.info("sqlColtype 2: "+sqlColType);		
	// to here
		
		
//_logger.debug("colNum " + colNum + " " + cfgCol.getName() + " type:" + sqlColType + " size:" + srcFieldSize);
		
		// lookup sql column type in db specific syntax
		String createParams;
		ColType colType = findColType(sqlColType, colTypes, srcFieldAutoNum);
//_logger.debug("\t" + "2 colType: "+colType);
		// check if found match
		if(colType != null){
			
//_logger.debug("\t" + "3 parseCreateParams ["+parseCreateParams(srcMeta, colType, colNum) + "]");
			String colTypeDesc = colType.getName();
			String parms = parseCreateParams(srcMeta, colType, colNum, colName);  
			_logger.debug("colTypeName: "+colType.getName());
			if(colType.getType() == Types.NUMERIC){
				colTypeDesc = colTypeDesc.substring(0,7)+parms;
			}else{
				colTypeDesc = colTypeDesc+" "+parms;
			}
_logger.debug("colTypeDesc: "+colTypeDesc);
			return colTypeDesc;
					// colType.getCreateParams(), colNum);
		}
		else{
			throw new WarehouseException("Source SQL data type " + sqlColType + " not found in destination JDBC.");
		}
		
		//return null;
	}
	
	/**
	 * Get an applicable column type from the JDBC.  The implementing method will use the input
	 * srcSqlColType parameter and return the correct ColType for that database.
	 * 
	 * @param srcSqlColType java.sql.Types type to find.
	 * @param colTypes ArrayList of available types.
	 * @param autoNum True if this is an autonumber field, else false.
	 * @return Applicable column type, null if none found.
	 */
	protected abstract ColType findColType(int srcSqlColType, ColTypes colTypes, boolean autoNum);
	
	/**
	 * Parse our create parameters string for building a column and fill in the
	 * appropriate values where necessary.
	 * 
	 * @param srcMeta Metadata of template to use to build syntax.  May be null if building exclusively
	 * 	from configuration data.
	 * @param colType Type of column to create parameters for.
	 * @param colNum Number of column in srcMeta to build create syntax for.
	 * @return
	 * @throws SQLException Accessing source table's meta data. 
	 */
	protected abstract String parseCreateParams(ResultSetMetaData srcMeta, ColType colType, 
			int colNum, String colName) throws SQLException;
	
	
	/* *******************************************************************************************
	 * UTILITY FUNCTIONS
	 * *******************************************************************************************/
	
	
	/**
	 * Find the length (max size) of the column based on the configuration information.  If a length is
	 * specified, then it is used, otherwise the srcMeta table is used as a tempalte.
	 * 
	 * @param srcMeta Table meta data to use as a template.
	 * @param cfgCol Configuration information for column.
	 * @param colNum Number of column in source table if applicable.
	 * @return Value destination column length should be, -1 on error.
	 * 
	 * @throws SQLException Any error encountered.
	 */
	protected final int getColLength(ResultSetMetaData srcMeta, CfgColumn cfgCol, int colNum)
			throws SQLException{
		if(cfgCol != null && cfgCol.getLength() != null){
//System.out.println("1->" + cfgCol.getName() + ", size: " + cfgCol.getLength().toString());
			return cfgCol.getLength().intValue();
		}
		else if(srcMeta == null && cfgCol != null){
			return 0;
		}
		else if(srcMeta != null){
//System.out.println("2->" + srcMeta.getColumnName(colNum) + ", size: " + srcMeta.getColumnDisplaySize(colNum));
			return srcMeta.getColumnDisplaySize(colNum);
		}
		
		return -1;	// error
	}
	
	/**
	 * Take the given string and remove everything between and including all '[' and ']'.
	 * 
	 * @param str String to be stripped.
	 * @return Stripped string without unused optional components.
	 */
	protected final String removeOptionalSections(String str){
		String out = "";
		boolean inOptional = false;
		
		for(int curChar = 0; curChar < str.length(); curChar++){
			if(inOptional){
				if(str.charAt(curChar) == ']')
					inOptional = false;
			}
			else{
				if(str.charAt(curChar) == '[')
					inOptional = true;
				else
					out += str.charAt(curChar);
			}
		}
		
		return out;
	}
	
	/**
	 * Replace all instances of a character (src) with a String (dst).  Specifically
	 * programmed for column syntax parsing.  Will not check first or last letters.
	 * 
	 * @param str String to be converted.
	 * @param src Character to be replaced.
	 * @param dst String to replace src character with.
	 * @return String str with all src characters replaced with dst strings.
	 */
	protected final String singleLetterReplace(String str, char src, String dst){
		
		String out = "";
		int inOptional = -1; // -1 for not, otherwise starting bracket location
		boolean removedFirstOptionalBracket = false;
		
		// can't use a builtin replace because the M must not be in-front or behind another letter
		// assumed that it will never be the first or last letter in the string either
		
		// check each character
		for(int curChar = 0; curChar < str.length(); curChar++){
			// deal with optional brackets
			if(str.charAt(curChar) == '['){
				inOptional = curChar;
				out += str.charAt(curChar);
			}
			else if(str.charAt(curChar) == ']'){
				inOptional = -1;
				if(removedFirstOptionalBracket){
					// then we need to remove this one
					removedFirstOptionalBracket = false;
				}
				else{
					out += str.charAt(curChar);
				}
			} else
			{
				// check character
				if(str.charAt(curChar) == src &&
						// check previous character
						curChar > 0 && !isCharAlphanumeric(str.charAt(curChar-1)) &&
						// check next character
						curChar < str.length() - 1 && !isCharAlphanumeric(str.charAt(curChar+1))){
					
					// replace
					out += dst;
					//curChar ++;//= dst.length();
					
					// remove optional brackets if applicable
					if(inOptional >= 0){
						out = removeOptionalBrackets(out, inOptional);
						removedFirstOptionalBracket = true;
						//curChar--;	// correct for removal of opening bracket
					}
				}
				else{
					out += str.charAt(curChar);
					//curChar++;
				}
			}
		}
		
		return out;
	}
	
	/**
	 * Test if the given character is 0-9, A-Z, or a-z.
	 * 
	 * @param testCh Character to check.
	 * @return True if matches the ranges, else false.
	 */
	protected final boolean isCharAlphanumeric(char testCh){
		if(testCh >= '0' && testCh <= '9')
			return true;
		else if(testCh >= 'A' && testCh <= 'Z')
			return true;
		else if(testCh >= 'a' && testCh <= 'z')
			return true;
		else
			return false;
	}
	
	/**
	 * Replace all instances of a string (src) with a String (dst).  Specifically
	 * programmed for column syntax parsing, removing optional brackets etc if required.
	 * 
	 * @param str String to be converted.
	 * @param src String to be replaced.
	 * @param dst String to replace src character with.
	 * @return String str with all src characters replaced with dst strings.
	 */
	protected final String stringReplace(String str, String src, String dst){
		String out = "";
		int srcLoc;
		int lastCopied = 0;
		int copyStart, copyLast;
		
		// can't use a builtin replace because the src must not be in-front or behind another letter
		// only removes '[' & ']' directly surrounding src
		
		// check for all src strings
		while((srcLoc = str.indexOf(src, lastCopied)) >= 0){
			// check for optional opening brackets
			if(srcLoc > 0 && src.charAt(srcLoc - 1) == '['){
				copyLast = srcLoc - 1;
			}
			else{
				copyLast = srcLoc;
			}
			
			// copy previous part of string to out
			copyStart = lastCopied;
			out += str.substring(copyStart, copyLast);
			
			// copy dst to out
			out += dst;
			
			// move to next position
			lastCopied += src.length();
			if(lastCopied < str.length() && str.charAt(lastCopied) == ']'){
				lastCopied++;
			}
		}
		
		// copy end of string
		if(lastCopied < str.length()){
			out += str.substring(lastCopied);
		}
		
		return out;
	}
	
	/**
	 * Remove the opening and closing brackets around an optional section.
	 * 
	 * @param str String with optional brackets to be removed.
	 * @param optBracketStart Starting bracket to be removed.  Removes characters until
	 * 			close is found.
	 * @return String with optional section stripped.
	 */
	protected final String removeOptionalBrackets(String str, int optBracketStart){
		String out = "";
		
		// copy first portion of string
		out = str.substring(0, optBracketStart);
		
		// remove close bracket
		int curChar;
		for(curChar = optBracketStart + 1; curChar < str.length(); curChar++){
			if(str.charAt(curChar) == ']'){
				break;
			}
			out += str.charAt(curChar);
		}
		
		// copy end of string
		out += str.substring(curChar, str.length());
		
		return out;
	}
	
	/**
	 * Check the returned batch errors after submitting a batch for commit.  If any
	 * errors occured, returs true, else false.
	 * 
	 * @param Error codes to check.
	 * @return Error occurred (true) or did not (false).
	 */
	protected final boolean checkBatchErrs(int [] updatesRslt){
		// search for errs
		for(int curResult = 0; curResult < updatesRslt.length; curResult++){
			if (updatesRslt[curResult] >= 0) {
                // Successfully executed; the number represents number of affected rows
            } else if (updatesRslt[curResult] == Statement.SUCCESS_NO_INFO) {
                // Successfully executed; number of affected rows not available
            } else if (updatesRslt[curResult] == Statement.EXECUTE_FAILED) {
                // Failed to execute
            	_logger.error("Failed to execute row " + curResult + ".");
            	return true;
            }
		}
		
		return false;
	}
	protected final Object convertBitToYNSpecial(Object copyValue, String bitValue){
		Object out = "N";
//		_logger.error("bitValue:"+bitValue);
//		_logger.error("copyValue:"+copyValue);
		
		try{
			// assumes sql query has already converted first characters to ascii equivalents
			//converting copyvalue string to int and applying bitwise and
			int i = (int)(Integer.parseInt(copyValue.toString()) & 0xFF);
//			_logger.error("i:"+i);
			//int j = Integer.parseInt(bitValue.toString());
			int j = Integer.parseInt(bitValue);
//			_logger.error("j:"+j);

		    // perform bit masking to determine if required bit is active
		    if((i & j) != 0){
//		    	_logger.error("in i and j comparison");
		    	out = "Y";
		    }
//System.out.println(i + " & " + j + " = " + out);
		}
		catch(Exception e){
			
        	_logger.error("Error converting BIT value [" + copyValue + "] to Y/N value ");
		}
		return out;
	}
	
	/**
	 * Convert an ASCII Byte character to a Y or N value depending on whether bitValue is set.
	 * 
	 * @param value to copy to destination column.
	 * @param bitValue to check for.  eg. "8"
	 * @return Y or N.
	 */
	protected final Object convertBitToYN(Object copyValue, String bitValue){
		Object out = "N";
//		_logger.error("copyValue:"+copyValue);
//		_logger.error("bitValue:"+bitValue);
		
		try{
			// convert first characters to ascii equivalents
			String encoding = _cfg.getByteEncoding();
			byte[] b1 = null;
			if(encoding != null){
				b1 = copyValue.toString().getBytes(encoding);
			}else{
				b1 = copyValue.toString().getBytes();
			}
//			byte[] b1 = copyValue.toString().getBytes("UTF8");
//			_logger.error("byte array length:"+b1.length);
//			_logger.error("b1 before converting to int:"+b1[0]);
			int i = (int)(b1[0] & 0xFF);
//			_logger.error("i:"+i);
			//int j = Integer.parseInt(bitValue.toString());
			int j = Integer.parseInt(bitValue);
//			_logger.error("j:"+j);

		    // perform bit masking to determine if required bit is active
		    if((i & j) != 0){
//		    	_logger.error("in i and j comparison");
		    	out = "Y";
		    }
//System.out.println(i + " & " + j + " = " + out);
		}
		catch(Exception e){
			
        	_logger.error("Error converting BIT value [" + copyValue + "] to Y/N value ");
		}
		return out;
	}
}
