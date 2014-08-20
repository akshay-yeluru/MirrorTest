/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db.vortex;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.cfg.CfgTable;
import com.tscsoftware.warehouse.db.ColType;
import com.tscsoftware.warehouse.db.ColTypes;

/**
 * Vortex Server implementation for Table class.
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
		
		// this function doesn't really matter, since we never write to vortex
		
		ArrayList matchingColTypes = colTypes.get(srcSqlColType);
		// convert unsupported types between databases
		if(matchingColTypes == null || matchingColTypes.size() < 1){
			return null; // no type found
		}
		
		return (ColType)matchingColTypes.get(0);
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Table#parseCreateParams(java.sql.ResultSetMetaData, java.lang.String, int)
	 */
	protected String parseCreateParams(ResultSetMetaData srcMeta,
			ColType colType, int colNum, String colName) throws SQLException {
		
		// shouldn't ever need to call this
		
		return "";
	}
}
