/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.cfg;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;


/**
 * Contains all configuration information for a column tag (or srcColumns) represented in a confiruation
 * xml file.  Also handles the parsing of the tag.
 * 
 * @author Jason S
 */
public class CfgColumn {
	
	protected static Logger		_logger;

	protected String	_name;
	protected String	_foreignKey;
	protected String	_value;
	protected Boolean	_isPrimaryKey;
	protected Integer	_length;
	protected Integer	_sqlType;
	protected String	_copyFromColumnName;
	protected String	_startEndPos;
	protected String	_bitYN;
	protected String 	_bitYNSpecial;
	
	public CfgColumn(){
		_logger = Logger.getLogger(CfgColumn.class);
		initializeMembers();
	}
	
	/**
	 * Parse the column element and setup the object.
	 * 
	 * @param elCol Column element.
	 * @throws ConfigException Any error encountered.
	 */
	CfgColumn(Element elCol) throws ConfigException{
		_logger = Logger.getLogger(CfgColumn.class);
		parse(elCol);
	}
	
	/**
	 * Set all member variables to their defaults.
	 */
	protected void initializeMembers(){
		_name			= null;
		_foreignKey		= null;
		_isPrimaryKey	= false;
		_length			= null;
		_sqlType		= null;
		_copyFromColumnName = null;
		_startEndPos	= null;
		_bitYN			= null;
	}
	
	
	/* ***********************************************************
	 * GET AND SET METHODS
	 * ***********************************************************/
	
	
	public String getName(){
		return _name;
	}
	
	public String getForeignKey(){
		return _foreignKey;
	}
	
	public Integer getLength(){
		return _length;
	}
	
	public String getValue(){
		return _value;
	}
	
	public Integer getSqlType(){
		return _sqlType;
	}
	
	public String getCopyFrom(){
		return _copyFromColumnName;
	}

	public String getStartEndPos(){
		return _startEndPos;
	}
	
	public String getBitYN(){
		return _bitYN;
	}
	
	public String getBitYNSpecial(){
		return _bitYNSpecial;
	}
	public boolean isPrimaryKey(){
		return _isPrimaryKey;
	}
	
	public void setLength(int length){
		_length = new Integer(length);
	}
	
	public void setName(String name){
		_name = name;
	}
	
	public void setValue(String value){
		_value = value;
	}
	
	public void setIsPrimaryKey(boolean primary){
		_isPrimaryKey = primary;
	}
	
	public void setSqlType(int type){
		_sqlType = new Integer(type);
	}
	
	
	/* ***********************************************************
	 * PARSE METHODS
	 * ***********************************************************/
	
	
	/**
	 * Parse the column element and populate this object.
	 * 
	 * @param elCol Column element.
	 * @throws ConfigException On any error encountered.
	 */
	public void parse(Element elCol) throws ConfigException{
		initializeMembers();
		
		// get name
		_name = elCol.getAttribute("name");
		//_logger.debug("Found column " + _name);
		if(_name == null || _name.length() <= 0){
			throw new ConfigException("Missing required attribute \"name\" in a column tag.");
		}
		
		// get value
		_value = elCol.getAttribute("value");
		if(_value != null && _value.length() <= 0){
			_value = null;
		}
		
		// get type
		try{
			_sqlType = new Integer(elCol.getAttribute("jdbcType"));
		}
		catch(Exception e){
			_sqlType = null;
		}
		
		// get size
		try{
			_length = new Integer(elCol.getAttribute("length"));
		}
		catch(Exception e){
			_length = null;
		}
		
		// get foreign key
		_foreignKey = elCol.getAttribute("foreignKey");
		
		// get primary key
		String tmpStr = elCol.getAttribute("primaryKey");
		if(tmpStr == null || tmpStr.length() < 1){
			_isPrimaryKey = false;
		}
		else{
//			_isPrimaryKey = Boolean.getBoolean(tmpStr);
			_isPrimaryKey = new Boolean(tmpStr);
		}

		//B2K: - New column transformation logic

		// get alternate source column name
		String copyFromColumnName = elCol.getAttribute("copyFrom");
		if (copyFromColumnName != null && copyFromColumnName.length() >= 1){
			// column name transformation
			_copyFromColumnName = copyFromColumnName;
			_logger.debug(_name + " has CopyFrom " + copyFromColumnName);
		}

		// get alternate source column start/end positions
		String startEndPos = elCol.getAttribute("copySubString");
		if (startEndPos != null && startEndPos.length() >= 1){
			// column name transformation
			_startEndPos = startEndPos;
			_logger.debug(_name + " has copySubString " + copyFromColumnName);
		}

		
		// get Status Bit Transformation settings
		String tmpBit = elCol.getAttribute("bitYN");
		if (tmpBit != null && tmpBit.length() >= 1){
			// Status Bit transformation
			_bitYN = tmpBit;
			_logger.debug(_name + " has bitYN " + tmpBit);
		}

		// get Status Bit Transformation settings custom for SRB East
		String tmpBitSpecial = elCol.getAttribute("bitYNSpecial");
		if (tmpBitSpecial != null && tmpBitSpecial.length() >= 1){
			// Status Bit transformation
			_bitYNSpecial = tmpBitSpecial;
			_logger.debug(_name + " has bitYN " + tmpBitSpecial);
		}

	}
}
