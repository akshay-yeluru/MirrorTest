/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db.vortex;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.WarehouseException;
import com.tscsoftware.warehouse.cfg.CfgTable;

/**
 * Vortex Server implementation for Db class.
 * 
 * @author Jason S
 */
public final class Db extends com.tscsoftware.warehouse.db.Db {

	public Db(){
		super();
		_logger = Logger.getLogger(Db.class);
	}
	
	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Db#getTable(java.lang.String)
	 */
	public com.tscsoftware.warehouse.db.Table getTable(String tableName) throws SQLException,
			WarehouseException {

		return new Table(tableName, _cfg.getCfgTable(tableName), _conn, _stmntWriteable,
				_stmntReadOnly);
	}

	/* (non-Javadoc)
	 * @see com.tscsoftware.warehouse.db.Db#getTable(java.lang.String, com.tscsoftware.warehouse.cfg.CfgTable)
	 */
	public com.tscsoftware.warehouse.db.Table getTable(String tableName, CfgTable cfg) throws
			SQLException, WarehouseException {
		
		return new Table(tableName, cfg, _conn, _stmntWriteable,
				_stmntReadOnly);
	}
}
