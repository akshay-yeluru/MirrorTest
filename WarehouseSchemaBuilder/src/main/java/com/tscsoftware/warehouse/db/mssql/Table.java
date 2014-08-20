/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db.mssql;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.log4j.Logger;
//import org.pf.text.StringPattern;

import com.tscsoftware.warehouse.cfg.CfgColumn;
import com.tscsoftware.warehouse.cfg.CfgTable;
import com.tscsoftware.warehouse.db.ColType;
import com.tscsoftware.warehouse.db.ColTypes;

/**
 * MS SQL Server implementation for Table class.
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
				
		// do exceptions where the default type is not the one we want to select
		switch(srcSqlColType){
			case Types.VARBINARY:
				// always pick varbinary instead of uniqueidentifier
				return colTypes.get("varbinary");
			case Types.BINARY:
				// always pick binary instead of timestamp
				return colTypes.get("binary");
			case Types.LONGVARCHAR:
				// always pick text (not ntext)
				return colTypes.get("text");
			case Types.CHAR:
				// always pick char instead of nchar
				return colTypes.get("char");
			case Types.DECIMAL:
				// pick decimal, not money or small money
				if(autoNum)
					return colTypes.get("decimal() identity");
				else
					return colTypes.get("decimal");
			case Types.DOUBLE:		
				// dne in mssql
				return colTypes.get("float");
			case Types.VARCHAR:
				// pick varchar, not nvarchar,sysname,or sql_variant
				return colTypes.get("varchar");
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP: // use datetime rather than smalldatetime
				return colTypes.get("datetime");
//			case Types.NUMERIC:
//				return colTypes.get xxx
		}
		
		// usually a matching type
		ArrayList matchingTypes = colTypes.getColType(srcSqlColType);
		if(matchingTypes == null || matchingTypes.size() < 1){
			return null;
		}
		// check for identity types
		// (for all types that may contain an identity, identity or not are the only two
		//  variations from the jdbc types)
		//Mcox StringPattern identityPtrn = new StringPattern("*identity*", true);
		//Mcox identityPtrn.multiCharWildcardMatchesEmptyString(true);
		
		String pattern = "identity";
		for(int match = 0; match < matchingTypes.size(); match++){
			ColType temp = (ColType)matchingTypes.get(match);
			//MCox if(autoNum && identityPtrn.matches(temp.getName())){
			 if(autoNum && matches(pattern, temp.getName())){
				return temp;
			}
			//MCox else if(!autoNum && !identityPtrn.matches(temp.getName())){
			 else if(!autoNum && !matches(pattern, temp.getName())){
				return temp;
			}
		}
		
		return null;
	}
	
	/*
	 * Added by MCox
	 */
	private boolean matches(String pattern, String value){
		
		int index = -1;
		
		pattern = pattern.toLowerCase();
		value = value.toLowerCase();
		
		pattern.indexOf(pattern);
		if (index >= 0 ) return true;
		return false;
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
			String createParams, int colNum, String colName) throws SQLException {
//_logger.debug("1 in parseCreateParams createParams:"+createParams+"; colNum:"+colNum+" ;colName:"+colName);			
		if(createParams == null) return "";  // just means there are no parameters
		//_logger.debug("create params: " + createParams);
//_logger.debug("2 in parseCreateParams");			
		String out = createParams;
		CfgColumn cfgCol = null;
		if(_cfg != null){
//			cfgCol = _cfg.getCfgColumn(colNum);     bug, cfg file lookup should be done by colName, not colNum
			cfgCol = _cfg.getCfgColumn(colName);
		}
		
		int maxSize;

		// get width
		if(cfgCol != null && cfgCol.getLength() != null){
			// load size from config file
			maxSize = cfgCol.getLength().intValue();
//_logger.debug("     -- msSQL size loaded from config file, " + maxSize);
		}
		else{
			// load size from metadata
			maxSize = getColLength(srcMeta, cfgCol, colNum);
//_logger.debug("     -- msSQL size loaded from metadata, " + maxSize);
		}

//		_logger.debug("3 in parseCreateParams out:"+out);			
		
		// replace all "length"s
		out = stringReplace(out, "max length", "(" + String.valueOf(maxSize) + ")");
//		_logger.info("4 in parseCreateParams out:"+out);			
		out = stringReplace(out, "length", "(" + String.valueOf(maxSize) + ")");
//		_logger.debug("5 in parseCreateParams out:"+out);			
		
		// replace all "precision"s
		// TODO test (need decimal or numeric type)
		if(srcMeta != null){
			int precision = srcMeta.getPrecision(colNum);
			// adding scale - rn
			int scale = srcMeta.getScale(colNum);
			//
			out = stringReplace(out, "precision", "("+String.valueOf(precision)+","+String.valueOf(scale)+")");
//			int scale = srcMeta.getScale(colNum);
//			out = stringReplace(out, "scale", String.valueOf(scale));
		}
//		_logger.debug("6 in parseCreateParams out:"+out);			

		// manually convert numeric types with a scale factor 
		if (out.equalsIgnoreCase("20,scale")) {
			out = "(20,2)";
		}
		// manually convert numeric types that did not convert
		if (out.equalsIgnoreCase("precision,scale")) {
			out = "(20,2)";
		}

//		_logger.debug("8 in parseCreateParams out:"+out);			
		//_logger.debug("\t" + "x> scale [" + out + "]");
		
		// set flags (unsigned)
		if(srcMeta != null){
			boolean isSigned = srcMeta.isSigned(colNum);
			if(isSigned == false){
				out.replaceAll("[UNSIGNED]", "UNSIGNED");
			}
		}
//		_logger.info("9 in parseCreateParams out:"+out);			
		
		// remove all optional components we ain't usin
		out = removeOptionalSections(out);
		
//		_logger.info("10 in parseCreateParams out:"+out);			
		//_logger.debug("out: " + out);
		
		return out;
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Table#parseCreateParams(java.sql.ResultSetMetaData, com.tscsoftware.warehouse.db.ColType, int)
	 */
	protected String parseCreateParams(ResultSetMetaData srcMeta,
			ColType colType, int colNum, String colName) throws SQLException {

		return parseCreateParams(srcMeta, colType.getCreateParams(), colNum, colName);
	}
}
