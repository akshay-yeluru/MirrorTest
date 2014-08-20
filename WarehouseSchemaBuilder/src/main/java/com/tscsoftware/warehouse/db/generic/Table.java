/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db.generic;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.cfg.CfgColumn;
import com.tscsoftware.warehouse.cfg.CfgTable;
import com.tscsoftware.warehouse.db.ColType;
import com.tscsoftware.warehouse.db.ColTypes;

/**
 * Generic implementation for Table class.  Not really sure this will ever be useful.
 * 
 * @author Jason S
 */
public final class Table extends com.tscsoftware.warehouse.db.Table {
	
	/*
	 * See superclass for documentation
	 */
	public Table(String name, CfgTable cfgTable, Connection conn,
			Statement stmntWriteable, Statement stmntReadOnly)
			throws SQLException {
		super(name, cfgTable, conn, stmntWriteable, stmntReadOnly);
		_logger = Logger.getLogger(Table.class);
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Table#findColType(int, com.tscsoftware.warehouse.db.ColTypes, boolean)
	 */
	protected ColType findColType(int srcSqlColType, ColTypes colTypes,
			boolean autoNum) {
		ArrayList matchingColTypes = colTypes.get(srcSqlColType);
		// convert unsupported types between databases
		if(matchingColTypes == null || matchingColTypes.size() < 1){
			
			// MS SQL stuff
			
			// set double to float
			if(srcSqlColType == Types.DOUBLE){
				return (ColType)colTypes.get(Types.FLOAT).get(0);
			}
			
			return null; // no type found
		}
		// check if found match
		else if(matchingColTypes != null && matchingColTypes.size() == 1){
			// only found one type, must be correct
			// MSSQL Apples to types: BIT, LONGVARCHAR, FLOAT, REAL
			return (ColType)matchingColTypes.get(0);
		}
		else if(matchingColTypes != null && matchingColTypes.size() > 1){
			
			for(int matchedCol = 0; matchedCol < matchingColTypes.size(); matchedCol++){
				ColType testType = (ColType)matchingColTypes.get(matchedCol);
			
				// special cases for my sql
				
				// type bit is always a bit
				if(srcSqlColType == Types.BIT && testType.getName().trim().compareToIgnoreCase("bit") == 0){
					return testType;
				}
				
				// type longvarbinary is always a "long varbinary" in mysql, or an "image" in mssql
				if(srcSqlColType == Types.LONGVARBINARY && (
						testType.getName().trim().compareToIgnoreCase("image") == 0 ||
						testType.getName().trim().compareToIgnoreCase("long varbinary") == 0)){
					return testType;
				}
				
				// double type can be lots of things, just use double
				if(srcSqlColType == Types.DOUBLE && testType.getName().trim().compareToIgnoreCase("double") == 0){
					return testType;
				}
				
				// long varchar in mssql is text, in mysql could be lots of things including text, just return text always
				if(srcSqlColType == Types.LONGVARCHAR && testType.getName().trim().compareToIgnoreCase("text") == 0){
					return testType;
				}
				
				// always consider a char a char (don't use mssql nchar)
				if(srcSqlColType == Types.CHAR && testType.getName().trim().compareToIgnoreCase("char") == 0){
					return testType;
				}
				
				// always consider a varchar a varchar (don't mssql use nvarchar,sysname,sql_variant or mysql enum,set)
				if(srcSqlColType == Types.VARCHAR && testType.getName().trim().compareToIgnoreCase("varchar") == 0){
					return testType;
				}
				
				// always consider datetime a datetime (don't use smalldatetime is mssql or timestamp in mysql)
				if(srcSqlColType == Types.TIMESTAMP && testType.getName().trim().compareToIgnoreCase("datetime") == 0){
					return testType;
				}
				
				// identity types
				// (TINYINT, BIGINT, NUMERIC, DECIMAIL(+more which all considered decimal), INTEGER, SMALLINT)
				// TODO mssql identity cases (not supported with current architecture) far-far away
				if(srcSqlColType == Types.TINYINT || srcSqlColType == Types.BIGINT || 
						srcSqlColType == Types.NUMERIC || srcSqlColType == Types.DECIMAL ||
						srcSqlColType == Types.INTEGER || srcSqlColType == Types.SMALLINT){
					
					// don't include identity types
					if(!testType.getName().trim().toLowerCase().endsWith("identity")){
						// type integer always return int (not integer or mediumint in mysql)
						if(srcSqlColType == Types.INTEGER &&
								testType.getName().trim().compareToIgnoreCase("int") == 0){
							return testType;
						}
					}
					
					if(testType.getName().trim().toLowerCase().endsWith("identity") && autoNum){
						return testType;
					}
					else if(!testType.getName().trim().toLowerCase().endsWith("identity") && !autoNum){
						// THIS ONLY WORKS SOMETIMES when using mssql destination database
						return testType;
					}
				}
			}
			
			return null;	// no special case found, dunno which to pick
		}
		else{
			// type not found
			return null;
		}
	}
	
	/**
	 * Parse our create parameters string for building a column and fill in the
	 * appropriate values where necessary.
	 * 
	 * @param srcMeta Metadata of template to use to build syntax.  May be null if building exclusively
	 * 	from configuration data.
	 * @param createParams String of parameters to be used to determine formatting of parameters.
	 * @param colNum Number of column in srcMeta to build create syntax for.
	 * @return
	 * @throws SQLException Accessing source table's meta data. 
	 */
	private String parseCreateParams(ResultSetMetaData srcMeta,
			String createParams, int colNum) throws SQLException {
			
		if(createParams == null) return "";  // just means there are no parameters
			
		String out = createParams;
		CfgColumn cfgCol = null;
		if(_cfg != null){
			cfgCol = _cfg.getCfgColumn(colNum);
		}
		
		// replace all "M"s (mysql) "length"s (mssql)
		int maxSize = getColLength(srcMeta, cfgCol, colNum);
		out = singleLetterReplace(out, 'M', String.valueOf(maxSize));
		out = stringReplace(out, "max length", "(" + String.valueOf(maxSize) + ")");
		out = stringReplace(out, "length", "(" + String.valueOf(maxSize) + ")");
		
		// replace all "D"s (mysql)	// TODO test with mssql
		if(srcMeta != null){	// TODO add this to column config
			int precision = srcMeta.getPrecision(colNum);
			out = singleLetterReplace(out, 'D', String.valueOf(precision));
		}
		
		// set flags (unsigned)
		if(srcMeta != null){
			boolean isSigned = srcMeta.isSigned(colNum);
			if(isSigned == false){
				out.replaceAll("[UNSIGNED]", "UNSIGNED");
			}
		}
		
		// remove all optional components we ain't usin
		out = removeOptionalSections(out);
		
		return out;
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Table#parseCreateParams(java.sql.ResultSetMetaData, com.tscsoftware.warehouse.db.ColType, int)
	 */
	protected String parseCreateParams(ResultSetMetaData srcMeta,
			ColType colType, int colNum, String colName) throws SQLException {

		return parseCreateParams(srcMeta, colType.getCreateParams(), colNum);
	}
}
