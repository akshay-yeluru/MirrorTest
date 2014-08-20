/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.tscsoftware.warehouse.cfg.DisconnectedResultSet;
import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.cfg.CfgIndex;
import com.tscsoftware.warehouse.cfg.CfgTable;

/**
 * Handle everything to do with an index for a table.
 * 
 * @author Jason S
 */
public class Index {
	
	protected static Logger		_logger = Logger.getLogger(Index.class);
	
	/**
	 * Build indices in srcIndices into SQL strings which can be executed to create
	 * the indices.  Uses the configuration information to override the srcIndices
	 * result set. 
	 * 
	 * @param srcTable Table containing indices to be created.  All indices
	 * 			will be created by default so do not include indices from other tables.
	 * 			May only be null if all configuration data is in cfgTable.
	 * @param cfgTable Configuration information for the destination table.  May be null
	 * 			to use defaults. 
	 * @param tableName Name of destination table to create indices on.
	 * @return Iterator of Strings with create index SQL statements which can be executed
	 * 			to create the indices on a table.
	 * @throws SQLException Any error encountered retreiving information from the srcIndices.
	 */
	public static Iterator buildCreateSql(Table srcTable, CfgTable cfgTable, String tableName)
			throws SQLException{
		
		String statement;
		int numCfgIndexes = 0;
		if(cfgTable != null){
			numCfgIndexes = cfgTable.getNumIndices();
		}
		int remainingCfgIndices = numCfgIndexes;
		
		ResultSet srcIndices = srcTable.getIndices();
		try{
			srcIndices.beforeFirst();
		}
		catch(SQLException e){}
		
		// populate hashmap with all indices
		// contains all statements to create indices associated by name
		HashMap statements = buildStartHashmap(srcIndices, cfgTable, tableName);
		srcIndices.close();
		
		// add prepend columns
		Iterator indexNames = statements.keySet().iterator();
		if(numCfgIndexes > 0){
			while(indexNames.hasNext()){
				String name = (String)indexNames.next();
				CfgIndex cfgIndex = cfgTable.getCfgIndex(name);
				
				if(cfgIndex != null && cfgIndex.getPrependColumns() != null && 
						cfgIndex.getPrependColumns().trim().length() > 0){
				
					statement = (String)statements.get(name)
							+ cfgIndex.getPrependColumns() + ",";
					 _logger.debug("Index with prepended columns: " + statement);
					statements.put(name, statement);
				}
			}
		}
		
		// add defined columns
		if(numCfgIndexes > 0){	
			// loop through each specified name
			for(int curIndex = 0; curIndex < numCfgIndexes; curIndex++){
				CfgIndex cfgIndex = cfgTable.getCfgIndex(curIndex);			
				
				// add manually defined columns without wildcards in the name
				if(cfgIndex.getColumns() != null && cfgIndex.getColumns().trim().length() > 0 &&
						cfgIndex.getName().indexOf('*') < 0 && cfgIndex.getName().indexOf('?') < 0){
					
					statement = (String)statements.get(cfgIndex.getName());
					if(statement != null){
						statement += cfgIndex.getColumns() + ",";
						statements.put(cfgIndex.getName(), statement);
						remainingCfgIndices--;
						 _logger.debug("Index with defined columns: " + statement);
					}
				}
			}
		}
		
		// add source columns
		if((numCfgIndexes > 0 && remainingCfgIndices > 0) || numCfgIndexes <= 0){
			String indexName;
			String columnName = "";
			String ordinalpos = "";
			String ascendval = "";
			String combinedname = "";
			CfgIndex cfgIndex = null;
			
			srcIndices = srcTable.getIndices();
			DisconnectedResultSet drs = new DisconnectedResultSet(srcIndices);
			HashMap indexNameMap = new HashMap();
			HashMap columnNameMap = new HashMap();
			HashMap columnNameSubMap = new HashMap();
			HashMap ordinalMap = new HashMap();
			HashMap ordinalSubMap = new HashMap();
			HashMap ascendMap = new HashMap();
			HashMap ascendSubMap = new HashMap();
			drs.beforeFirst();
			while(drs.next()){
				indexName = drs.getString("INDEX_NAME");
				columnName = drs.getString("COLUMN_NAME");
				ordinalpos = drs.getString("ORDINAL_POSITION");
				ascendval = drs.getString("ASC_OR_DESC");
				indexNameMap.put(indexName, indexName);
				if(ordinalMap.containsKey(indexName)){
					ordinalSubMap = (HashMap)ordinalMap.get(indexName);
					ordinalSubMap.put(ordinalpos, columnName);
					ordinalMap.put(indexName, ordinalSubMap);
				}else{
					ordinalSubMap = new HashMap();
					ordinalSubMap.put(ordinalpos, columnName);
					ordinalMap.put(indexName, ordinalSubMap);
				}
//				_logger.debug("putting into ascendmap indexname:"+indexName+"; ordinalpos:"+ordinalpos+"; ascendval:"+ascendval);
				ascendMap.put(indexName+ordinalpos, ascendval);
			}
			Iterator i = indexNameMap.keySet().iterator();
			while(i.hasNext()){
				indexName = i.next().toString();
				
				// check if this is an index we should copy
				if(cfgTable != null){
					cfgIndex = cfgTable.getCfgIndex(indexName);
					// only copy cfg indices without columns specified
					if(cfgIndex != null && cfgIndex.getColumns() != null && 
							cfgIndex.getColumns().length() > 0 ){
						cfgIndex = null;		
					}
				}
				
				if((cfgIndex != null || numCfgIndexes <= 0) && statements.containsKey(indexName)){
					// add column
					ordinalSubMap = (HashMap)ordinalMap.get(indexName);
//					_logger.debug("ascendMap:"+ascendMap);
//					_logger.debug("ordinalMap:"+ordinalMap);
//					_logger.debug("ordinalSubMap:"+ordinalSubMap);
					int j = ordinalSubMap.size();
					for(int k =0; k<j;k++){
						combinedname = indexName+k;
//						_logger.debug("combinedname:"+combinedname);
//						if(combinedname.startsWith(indexName)){
							columnName = ordinalSubMap.get(""+k).toString();
							ascendval = ascendMap.get(combinedname).toString();
//							_logger.debug("Index name:"+indexName);
//							_logger.debug("Index column:"+columnName);
//							_logger.debug("ascendval:"+ascendval);
							statement = (String)statements.get(indexName)
									+ buildAddColumn(columnName,ascendval);
							_logger.debug("Index with source columns: " + statement);
							statements.put(indexName, statement);
//						}
					}
				}

				
			}
//			try{
//				srcIndices.beforeFirst();
//			}
//			catch(SQLException e){}
			
			/*
			while(srcIndices.next()){
				indexName = srcIndices.getString("INDEX_NAME");
				
				// check if this is an index we should copy
				if(cfgTable != null){
					cfgIndex = cfgTable.getCfgIndex(indexName);
					// only copy cfg indices without columns specified
					if(cfgIndex != null && cfgIndex.getColumns() != null && 
							cfgIndex.getColumns().length() > 0 ){
						cfgIndex = null;		
					}
				}
				
				if((cfgIndex != null || numCfgIndexes <= 0) && statements.containsKey(indexName)){
					// add column
					_logger.debug("Index name:"+srcIndices.getString("INDEX_NAME"));
					_logger.debug("Index column:"+srcIndices.getString("COLUMN_NAME"));
					_logger.debug("ordinal position:"+srcIndices.getString("ORDINAL_POSITION"));
					statement = (String)statements.get(indexName)
							+ buildAddColumn(srcIndices.getString("COLUMN_NAME"),
									srcIndices.getString("ASC_OR_DESC"));
					_logger.debug("Index with source columns: " + statement);
					statements.put(indexName, statement);
				}
			}
			*/
			srcIndices.close();
		}
		
		// add append columns
		if(numCfgIndexes > 0){
			
			// loop through all indexes built and add appended columns as necessary
			indexNames = statements.keySet().iterator();
			while(indexNames.hasNext()){
				String indexName = (String)indexNames.next();
				CfgIndex cfgIndex = cfgTable.getCfgIndex(indexName);
				
				if(cfgIndex != null && cfgIndex.getAppendColumns() != null &&
						cfgIndex.getAppendColumns().trim().length() > 0){

					statement = (String)statements.get(indexName)
							+ cfgIndex.getAppendColumns() + ",";
					statements.put(indexName, statement);
					 _logger.debug("Index with appended columns: " + statement);
				}
			}
		}
		
		// add termination to all indices
		return buildEndAllStatements(statements.values().iterator());
	}



	
	/**
	 * Build a hashmap containing the beginning (as provided by buildStart()) of all indexes that should
	 * be created, including either those from configuration or the source table as necessary.
	 * 
	 * @param srcIndices ResultSet containing indices to be created.  All indices
	 * 			will be created by default so do not include indices from other tables.
	 * 			May only be null if all configuration data is in cfgTable.  Must be on
	 * 			beforeFirst row.
	 * @param cfgTable Configuration information for the destination table.  May be null
	 * 			to use defaults.
	 * @param tableName Name of destination table to create indices on.
	 * @return HashMap containing all indices to be created for this table, with the beginning of the
	 * 			necessary SQL statement.
	 * @throws SQLException Any error encountered retreiving information from the srcIndices.
	 */
	protected static HashMap buildStartHashmap(ResultSet srcIndices, CfgTable cfgTable, String tableName)
			throws SQLException {
		
		HashMap statements = new HashMap();
		
		CfgIndex cfgIndex;
		
		// [B2K] check if tableName must be transformed
		String iTableName = tableName;
		if (cfgTable.getNewName() != null){
			iTableName = cfgTable.getNewName();
			_logger.debug("Index will be applied to new tablename : " + iTableName);
		}
		else{
			_logger.debug("Index will be applied to current tablename : " + iTableName);
		}
		
		// figure out if there are any config indexes
		int numCfgIndexes = 0;
		if(cfgTable != null){
			numCfgIndexes = cfgTable.getNumIndices();
		}
		int remainingCfgIndexes = numCfgIndexes;
		
		// loop through config indexes and add
		if(numCfgIndexes > 0){
			for(int curIndex = 0; curIndex < numCfgIndexes; curIndex++){
				cfgIndex = cfgTable.getCfgIndex(curIndex);
				
				// do not add config columns with wildcards or without columns defined
				if(cfgIndex.getName().indexOf('*') < 0 && cfgIndex.getName().indexOf('?') < 0 &&
						cfgIndex.getColumns() != null && cfgIndex.getColumns().trim().length() > 0){
					// add
//					statements.put(cfgIndex.getName(), 
//							buildStart(cfgIndex.getName(), cfgIndex.isUnique(), tableName));
					statements.put(cfgIndex.getName(), 
							buildStart(cfgIndex.getName(), cfgIndex.isUnique(), iTableName));

					 _logger.debug("Adding defined index: " + cfgIndex.getName());
					remainingCfgIndexes--;	// this is just so we can skip the source table lookup if there
											// are only manually defined columns
				}
			}
		}
		
		// loop through source table and add all applicable indexes
		if((numCfgIndexes > 0 && remainingCfgIndexes > 0) || numCfgIndexes <= 0){
			String indexName;
			cfgIndex = null;
			
			while(srcIndices.next()){
				indexName = srcIndices.getString("INDEX_NAME");
				if(cfgTable != null){
					cfgIndex = cfgTable.getCfgIndex(indexName);
					// only copy cfg indices without columns specified
					if(cfgIndex != null && cfgIndex.getColumns() != null && 
							cfgIndex.getColumns().length() > 0 ){
						cfgIndex = null;		
					}
				}
				
				// check if this is an index we should copy
				if((numCfgIndexes > 0 && cfgIndex != null) || numCfgIndexes <= 0){
					
					// check if these tags from vortex can be skipped
					if(indexName != null && !indexName.startsWith("$_VTX")){
						//  check if index already exists in hashmap
						if(!statements.containsKey(indexName)){
							// create new index
							 _logger.debug("Adding source index: " + indexName);
							statements.put(indexName, 
									buildStart(indexName, !srcIndices.getBoolean("NON_UNIQUE"), iTableName));
						} 
					}
				}
			}
		}
		
		return statements;
	}
	
	/**
	 * Create the beginning of each create index SQL statement.
	 * 
	 * @param indexName Name of index to create.
	 * @param unique Is the index unique.
	 * @param tableName Name of table index will be created for.
	 * @return Starting portion of create index statement.
	 */
	protected static String buildStart(String indexName, boolean unique, String tableName){
		String statement = "create ";
		
		// add unique flag
		if(unique){
			statement += "UNIQUE ";
		}
		statement += "index " + indexName + " on " + tableName + " (";
		
		return statement;
	}
	
	/**
	 * Add a column to an index already started by the buildStart() method.
	 * 
	 * @param columnName Name of column to add to index.
	 * @param ascending "A" for ascending order, "D" for descending.  May be empty, null,
	 * 			or other for SQL server default.
	 * @return Column portion of create index statement.
	 */
	protected static String buildAddColumn(String columnName, String colAscDesc){
		// add this column to the end of the index
		String statement = columnName + " ";
		
		if(colAscDesc != null){
			if(colAscDesc.compareToIgnoreCase("A") == 0)
				statement += "ASC,";
			else if(colAscDesc.compareToIgnoreCase("D") == 0)
				statement += "DESC,";
		}
		else{
			statement += ",";
		}
		
		return statement;
	}
	
	/**
	 * Search through almost completed SQL statements for creating indices and add
	 * ending syntax to each statement.  Return as an iterator.
	 * 
	 * @param uncompleteStmnts Almost completed statements.
	 * @return Iterator containing all complete create index statements.
	 */
	protected static Iterator buildEndAllStatements(Iterator uncompleteStmnts){
		ArrayList completeStmnts = new ArrayList();
		String tmpStmnt;
		
		while(uncompleteStmnts.hasNext()){
			tmpStmnt = (String)uncompleteStmnts.next();

			// remove comma after last column
			tmpStmnt = tmpStmnt.substring(0, tmpStmnt.lastIndexOf(','));
			// finnish index statement
			tmpStmnt += ");";
			
			completeStmnts.add(tmpStmnt);
		}
		
		return completeStmnts.iterator();
	}

}
