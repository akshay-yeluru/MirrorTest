/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db.mysql;

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
 * MySQL implementation for Table class.
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
	protected ColType findColType(int srcSqlColType, ColTypes colTypes, boolean autoNum) {
		// handle special cases
		switch(srcSqlColType){
			case Types.TINYINT: 
				// sei nov 23, 2006 if tiny int then use int cuz of below error:
				// Data truncation: Data truncated; out of range for column 'SERIAL_NO' 
				return colTypes.get("int");
			case Types.BIT:
				// always pick bit, not bool
				return colTypes.get("bit");
			case Types.LONGVARBINARY:
				// always pick longvarbinary, not mediumblob,longblob,blob,or tinyblob
				return colTypes.get("long varbinary");
			case Types.LONGVARCHAR:
				// always pick text, not long varchar, mediumtext, longtext, or tinytext
				return colTypes.get("text");
			case Types.INTEGER:
				// always pick int, not integer or mediumint
				return colTypes.get("int");
			case Types.DOUBLE:
				// always pick double, not double precision or real
				return colTypes.get("double");
			case Types.VARCHAR:
				// always pick varchar, not enum or set
				return colTypes.get("varchar");
			case Types.TIMESTAMP:
				// always pick timestamp, not datestamp
				return colTypes.get("timestamp");
		}
		// by default, pick the first matching type
		ArrayList matchingColTypes = colTypes.get(srcSqlColType);
		if(matchingColTypes != null && matchingColTypes.size() > 0){
//			System.out.println("found type: "+((ColType)matchingColTypes.get(0)).getType()+",name: "+((ColType)matchingColTypes.get(0)).getName());
			return (ColType)matchingColTypes.get(0);
		}
		
		return null;
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
		//_logger.debug("createParams: " + createParams);
			
		String out = createParams;
		CfgColumn cfgCol = null;
		if(_cfg != null){
			cfgCol = _cfg.getCfgColumn(colNum);
		}
		
		// replace all "M"s
		int maxSize = getColLength(srcMeta, cfgCol, colNum);
		out = singleLetterReplace(out, 'M', String.valueOf(maxSize));
		
		// replace all "D"s
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
		//_logger.debug("out: " + out);
		
		return out;
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Table#parseCreateParams(java.sql.ResultSetMetaData, com.tscsoftware.warehouse.db.ColType, int)
	 */
	protected String parseCreateParams(ResultSetMetaData srcMeta,
			ColType colType, int colNum, String colName) throws SQLException {
		
		if(colType.getType() == Types.DOUBLE)
			return "";
		
		return parseCreateParams(srcMeta, colType.getCreateParams(), colNum);
	}
}
