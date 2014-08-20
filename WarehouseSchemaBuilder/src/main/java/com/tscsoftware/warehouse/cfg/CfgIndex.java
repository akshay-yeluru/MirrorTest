/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.cfg;

import org.w3c.dom.Element;

/**
 * Contains all configuration information for a column tag represented in a confiruation xml file.  Also
 * handles the parsing of the tag.
 * 
 * @author Jason
 */
public class CfgIndex {
	protected String	_appendColumns;
	protected String	_columns;
	protected String	_name;
	protected String	_prependColumns;
	protected boolean	_isUnique;
	
	CfgIndex(){
		initializeMembers();
	}
	
	/**
	 * Parse the index element and setup the object.
	 * 
	 * @param elIndex Index element.
	 * @throws ConfigException Any error encountered.
	 */
	CfgIndex(Element elIndex) throws ConfigException{
		parse(elIndex);
	}
	
	/**
	 * Set all member variables to their defaults.
	 */
	protected void initializeMembers(){
		_appendColumns	= null;
		_columns		= null;
		_name			= null;
		_prependColumns	= null;
		_isUnique		= false;
	}
	
	
	/* ***********************************************************
	 * GET AND SET METHODS
	 * ***********************************************************/
	
	
	public String getName(){
		return _name;
	}
	
	public String getAppendColumns(){
		return _appendColumns;
	}
	
	public String getColumns(){
		return _columns;
	}
	
	public String getPrependColumns(){
		return _prependColumns;
	}
	
	public boolean isUnique(){
		return _isUnique;
	}
	
	
	/* ***********************************************************
	 * PARSE METHODS
	 * ***********************************************************/
	
	
	/**
	 * Parse the index element and populate this object.
	 * 
	 * @param elIndex Index element.
	 * @throws ConfigException On any error encountered.
	 */
	public void parse(Element elIndex) throws ConfigException{
		initializeMembers();
		
		// get name
		_name = elIndex.getAttribute("name");
		if(_name == null || _name.length() <= 0){
			throw new ConfigException("Missing required attribute \"name\" in an index tag.");
		}
		
		// get columns
		_appendColumns = elIndex.getAttribute("appendColumns");
		// ignore columns attribute if there is a wildcard character in the name
		if(_name.indexOf('*') < 0 && _name.indexOf('?') < 0){
			_columns = elIndex.getAttribute("columns");
		}
		_prependColumns = elIndex.getAttribute("prependColumns");
		
		// get unique
		String tmpStr = elIndex.getAttribute("unique");
		if(tmpStr == null || tmpStr.length() < 1){
			_isUnique = false;
		}
		else{
			Boolean tmpBool = new Boolean(tmpStr);
			if(tmpBool != null){
				_isUnique = tmpBool.booleanValue();
			}
			else{
				_isUnique = false;
			}
		}
		
	} //parse
	
}
