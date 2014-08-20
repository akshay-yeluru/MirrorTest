/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db;

/**
 * Contains data for a column type.
 * 
 * @author Jason S
 */
public class ColType{
	int		_type;
	String	_name;
	String	_createParams;
	
	/**
	 * Create a new column type.
	 * 
	 * @param type DATA_TYPE field in a connection getMetaData().getTypeInfo() result set.
	 * @param name TYPE_NAME field in a connection getMetaData().getTypeInfo() result set.
	 * @param createParams CREATE_PARAMS field in a connection getMetaData().getTypeInfo() result set.
	 */
	ColType(int type, String name, String createParams){
		_type 			= type;
		_name			= name;
		_createParams	= createParams;
	}
	
	/**
	 * Get type, representig a DATA_TYPE field in a connection getMetaData().getTypeInfo() result set.
	 * 
	 * @return Type.
	 */
	public int getType(){
		return _type;
	}
	
	/**
	 * Get name, representig a TYPE_NAME field in a connection getMetaData().getTypeInfo() result set.
	 * 
	 * @return Name.
	 */
	public String getName(){
		return _name;
	}
	
	/**
	 * Get create parameters, representig a CREATE_PARAMS field in a connection getMetaData().getTypeInfo()
	 * result set.
	 * 
	 * @return Create parameters.
	 */
	public String getCreateParams(){
		return _createParams;
	}
}
