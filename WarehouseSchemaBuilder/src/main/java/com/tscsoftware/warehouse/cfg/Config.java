/*
* Copyright 2005 TSC Software Services Inc. All Rights Reserved.
*
* This software is the proprietary information of TSC Software Services Inc.
* Use is subject to license terms.
 */
package com.tscsoftware.warehouse.cfg;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.*;


/**
 * Contains all configuration information and handles the parsing of the xml file.
 * 
 * @author Jason S
 */
public class Config {
	
	private Logger		_logger;
	
	// sourcedatabase
	private String _srcDriver;
	private String _srcScript;
	private String _srcType;
	private String _srcName;
	
	// destinationdatabase
	private String 	_dstDriver;
	private String 	_dstScript;
	private String	_dstType;
	private String 	_dstName;
	private String	_dstWarehouseTable;
	private boolean	_clearDatabase;
	private boolean _clearNewTables;
	
	// tables
	private boolean 	_copyAllTables;
	private boolean 	_copyIndices;
	private boolean 	_createTables;
	private boolean 	_copyData;
	private ArrayList	_tables;
	private ArrayList	_tablesNotToDrop;
	private HashMap		_tablesByNewName;
	
	public Config(){
		_logger = Logger.getLogger(Config.class);
		initializeMembers();
	}
	
	/**
	 * Set all member variables to their defaults.
	 */
	private void initializeMembers(){
		_srcDriver 		= null;
		_srcScript 		= null;
		_srcType		= null;
		_srcName 		= null;
		
		_dstDriver 		= null;
		_dstScript 		= null;
		_dstType		= null;
		_dstName 		= null;
		_dstWarehouseTable	= null;
		_clearDatabase 	= false;
		_clearNewTables = false;
		
		_copyAllTables 	= true;
		_copyIndices	= true;
		_createTables	= false;
		_copyData		= true;
		_tables			= new ArrayList();
		_tablesByNewName = new HashMap();
		_tablesNotToDrop = new ArrayList();
	}
	
	
	/* ***********************************************************
	 * GET AND SET METHODS
	 * ***********************************************************/
	
	
	public String getSrcDriver(){
		return _srcDriver;
	}
	public String getSrcScript(){
		return _srcScript;
	}
	public String getSrcType(){
		return _srcType;
	}
	public String getSrcName(){
		return _srcName;
	}
	
	public String getDstDriver(){
		return _dstDriver;
	}
	public String getDstScript(){
		return _dstScript;
	}
	public String getDstType(){
		return _dstType;
	}
	public String getDstName(){
		return _dstName;
	}
	public String getDstWarehouseTable(){
		return _dstWarehouseTable;
	}
	
	/**
	 * @return Clear the destination database before beginning create table and copy data processes.
	 */
	public boolean getClearDatabase(){
		return _clearDatabase;
	}
	
	/**
	 * @return Clear the destination database of the tables that are going to be created before beginning create table and copy data processes.
	 */
	public boolean getClearNewTables(){
		return _clearNewTables;
	}
	
	/**
	 * @return Copy all tables from source to destination databases.
	 */
	public boolean getCopyAllTables(){
		return _copyAllTables;
	}
	
	/**
	 * @return Copy all indices from the old tables to the new.
	 */
	public boolean getCopyIndices(){
		return _copyIndices;
	}
	
	/**
	 * @return Create clean tables in the destination database.
	 */
	public boolean getCreateTables(){
		return _createTables;
	}
	
	/**
	 * @return Copy the data in the tables.
	 */
	public boolean getCopyData(){
		return _copyData;
	}
	
	/**
	 * @return Number of tables explicitly configured in xml file.
	 */
	public int getNumTables(){
		return _tables.size();
	}
	
	public HashMap getTablesByNewName(){
		return _tablesByNewName;
	}
	
	public ArrayList getTables(){
		return _tables;
	}
	
	public ArrayList getTablesNotToDrop(){
		return _tablesNotToDrop;
	}
	/**
	 * Retrieve the configuration data for a table based on the index provided.
	 * 
	 * @param tableNum Index of table configuration to be retrieved.
	 * @return CfgTable containing the tables configuration data, or null on error.
	 */
	public CfgTable getCfgTable(int tableNum){
		if(tableNum >= 0 && tableNum < _tables.size()){
			return (CfgTable)_tables.get(tableNum);
		}
		
		return null;
	}
	
	/**
	 * Attempt to retrieve table configuration based on the table's name.  Returns
	 * null if the name does not exist.  Will search for tables using * and ? wild cards as well,
	 * so if the name of the table is PAY_*, PAY_CODE will return a table.
	 * 
	 * @param tableName Case in-sensitive name of table to retrieve configuration data for.
	 * @return CfgTable for table with tableName, null if not found or error.
	 */
	public CfgTable getCfgTable(String tableName){
		if(tableName == null) return null;
		
		// search for table
		CfgTable tmpTable;
		for(int curTable = 0; curTable < _tables.size(); curTable++){
			tmpTable = (CfgTable)_tables.get(curTable); 
			//if(tmpTable.getName().compareToIgnoreCase(tableName) == 0){
			if(tableName.equalsIgnoreCase(tmpTable.getName())){
				return tmpTable;
			}
		}
		
		// table not found
		return null;
	}
	
	
	/* ***********************************************************
	 * PARSE METHODS
	 * ***********************************************************/
	
	/**
	 * Parse the xml file and populate this object.
	 * 
	 * @param xmlFile Path and filename of xml file containing the configuration data.
	 * @throws ConfigException On any error encountered.
	 * @throws IOExcpetion File not found or not valid.
	 */
	public void parse(InputStream inputStream) throws Exception,ConfigException, IOException{
		_logger.debug("Loading InputStream. ");
		
		// get the document for parsing
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			
			// parse the file
			NodeList nodes = document.getElementsByTagName("databasecopy");
			if(nodes.getLength() != 1){
				// invalid number of databasecopy tags specified
				throw new ConfigException("One and only one databasecopy tag must be defined in the InputStream.");
			}
			Element elRoot = (Element)nodes.item(0);
			parseRoot(elRoot);
		}
		catch(SAXException sxe){
			throw new ConfigException("Unable to parse InputStream. ", sxe);
		}
		catch(ParserConfigurationException pce){
			throw new ConfigException("Unable to parse InputSream. " , pce);
		}
		
		//System.out.println("Loaded xml file " + xmlFile);
		
	} // pars
	
	
	/**
	 * Parse the xml file and populate this object.
	 * 
	 * @param xmlFile Path and filename of xml file containing the configuration data.
	 * @throws ConfigException On any error encountered.
	 * @throws IOExcpetion File not found or not valid.
	 */
	public void parse(String xmlFile) throws Exception,ConfigException, IOException{
		_logger.debug("Loading xml file " + xmlFile);
		
		// load the file
		File file = new File(xmlFile);
		if(file == null || file.exists() == false || file.isFile() == false){
			String err = "Could not load file " + xmlFile + ", " +
					"does not exist or is not a normal file.";
			throw new IOException(err);
		}
		
		// get the document for parsing
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(file);
			
			// parse the file
			NodeList nodes = document.getElementsByTagName("databasecopy");
			if(nodes.getLength() != 1){
				// invalid number of databasecopy tags specified
				throw new ConfigException("One and only one databasecopy tag must be defined in " + xmlFile
						+ ".");
			}
			Element elRoot = (Element)nodes.item(0);
			parseRoot(elRoot);
		}
		catch(SAXException sxe){
			throw new ConfigException("Unable to parse file " + xmlFile + ".", sxe);
		}
		catch(ParserConfigurationException pce){
			throw new ConfigException("Unable to parse file " + xmlFile + ".", pce);
		}
		
		//System.out.println("Loaded xml file " + xmlFile);
		
	} // parse
	
	/**
	 * Parse the databasecopy element and populate the appropriate member variables.
	 * 
	 * @param elRoot databasecopy element
	 * @throws ConfigException Any error encountered.
	 */
	private void parseRoot(Element elRoot) throws Exception, ConfigException{
		if(elRoot == null){
			throw new ConfigException("Invalid configuration file, does not contain a proper databasecopy tag.");
		}
		
		NodeList nodes;
		
		// get all children tags and parse them
		
		// parse source database
		nodes = elRoot.getElementsByTagName("sourceDatabase");
		if(nodes.getLength() < 1){
			throw new ConfigException("Configuration XML file must contain at least one sourceDatabase tag.");
		}
		else if(nodes.getLength() > 1){
			throw new ConfigException("Multiple sourceDatabase tags specified in the configuration XML file.  " +
					"Only one may be specified.");
			// TO-DO add support for this one day
		}
		else{
			parseSrcDb((Element)nodes.item(0));
		}
		
		// parse destination database
		nodes = elRoot.getElementsByTagName("destinationDatabase");
		if(nodes.getLength() < 1){
			throw new ConfigException("Configuration XML file must contain at least one destinationDatabase tag.");
		}
		else if(nodes.getLength() > 1){
			throw new ConfigException("Multiple destinationDatabase tags specified in the configuration XML file.  " +
					"Only one may be specified.");
			// TO-DO add support for this one day
		}
		else{
			parseDstDb((Element)nodes.item(0));
		}
		
		// parse tables
		nodes = elRoot.getElementsByTagName("tables");
		if(nodes.getLength() != 1){
			throw new ConfigException("Configuration XML file must contain one and only one tables tag.");
		}
		else{
			parseTables((Element)nodes.item(0));
		}
		// parse tables not to be dropped
		nodes = elRoot.getElementsByTagName("tablesNotToDrop");
		if(nodes.getLength()== 1){
			parseTablesNotToDrop((Element)nodes.item(0));
		}
	} // parseRoot
	
	/**
	 * Parse the sourceDatabase element and populate the appropriate member variables.
	 * 
	 * @param elSrcDb sourceDatabase element
	 * @throws ConfigException On any error encountered.
	 */
	private void parseSrcDb(Element elSrcDb) throws ConfigException{
		
		// load attributes
		_srcDriver 	= elSrcDb.getAttribute("driver");
		_srcScript 	= elSrcDb.getAttribute("driverScript");
		_srcType 	= parseType(elSrcDb);
		_srcName	= elSrcDb.getAttribute("databaseName");
	}
	
	/**
	 * Parse the destinationDatabase element and populate the appropriate member variables.
	 * 
	 * @param elDstDb destinationDatabase element
	 * @throws ConfigException On any error encountered.
	 */
	private void parseDstDb(Element elDstDb) throws ConfigException{

		// load attributes
		_dstDriver 			= elDstDb.getAttribute("driver");
		_dstScript		 	= elDstDb.getAttribute("driverScript");
		_dstType		 	= parseType(elDstDb);
		_dstName			= elDstDb.getAttribute("databaseName");
		_dstWarehouseTable 	= elDstDb.getAttribute("warehouseTable");
		if(_dstWarehouseTable == null || _dstWarehouseTable.length() < 1){
			// default warehouse table if no other exists
			_dstWarehouseTable = new String("WAREHOUSE_STATUS");
		}

 		String tmpStr = elDstDb.getAttribute("clearDatabase");
		if(tmpStr == null || tmpStr.length() < 1 || tmpStr.compareToIgnoreCase("false") == 0){
			_clearDatabase = false;
		}
		else if(tmpStr.compareToIgnoreCase("true") == 0){
			_clearDatabase = true;
		}
		else{
			_clearDatabase = false;
			//System.err.println("\"clearDatabase\" attribute incorrect in tag <tables>.  Must" +
			//	" be set to \"true\" or \"false\".  Using default value of false.");
			String err = "\"clearDatabase\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of false.";
			_logger.warn(err);
		}
		
 		tmpStr = elDstDb.getAttribute("clearNewTables");
		if(tmpStr == null || tmpStr.length() < 1 || tmpStr.compareToIgnoreCase("false") == 0){
			_clearNewTables = false;
		}
		else if(tmpStr.compareToIgnoreCase("true") == 0){
			_clearNewTables = true;
		}
		else{
			_clearNewTables = false;
			String err = "\"clearNewTables\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of false.";
			_logger.warn(err);
		}
		
	}
	
	/**
	 * Sets the given type string according to the attribute in the provided element.  Performs the
	 * required validity checking.
	 * 
	 * @param elDatabase Element of the database tag
	 * @return Correct setting for the attribute 
	 * @throws ConfigException Invalid attribute setting.
	 */
	private String parseType(Element elDatabase) throws ConfigException{
		String type = elDatabase.getAttribute("type");
		if(type == null){
			throw new ConfigException("Required attribute \"type\" has not been set for a source or " +
					"destination database tag in the configuration XML file.");
		}
		
		if(type.compareToIgnoreCase("mssql") != 0 &&
				type.compareToIgnoreCase("mysql") != 0 &&
				type.compareToIgnoreCase("vortex") != 0 &&
				type.compareToIgnoreCase("generic") != 0){
			throw new ConfigException("The \"type\" attribute has been set incorrectly in a " +
					"sourceDatabase or destinationDatabase tag.  Acceptable values include: mssql, " +
					"mysql, vortex, and generic");
		}
		
		return type;
	}
	
	private void parseTablesNotToDrop(Element elTables) throws ConfigException{
		String tmpStr;
		Element tempelem = null;
		NodeList nlTables = elTables.getElementsByTagName("table");
		for(int curTable = 0; curTable < nlTables.getLength(); curTable++){
			if(nlTables.item(curTable).getNodeType() == Node.ELEMENT_NODE){
				tempelem = (Element)nlTables.item(curTable);
				if(tempelem.getAttribute("name")!= null){
					_tablesNotToDrop.add(tempelem.getAttribute("name"));
				}
			}
		}
	}
	/**
	 * Parse the tables element and populate the appropriate member variables.
	 * 
	 * @param elTables tables element
	 * @throws ConfigException Any error encountered.
	 */
	private void parseTables(Element elTables) throws Exception,ConfigException{
		String tmpStr;
		
		// get copy all tables
		tmpStr = elTables.getAttribute("all");
		if(tmpStr == null || tmpStr.length() < 1){
			_copyAllTables = true;
		}
		else if(tmpStr.compareToIgnoreCase("true") == 0){
			_copyAllTables = true;
		}
		else if(tmpStr.compareToIgnoreCase("false") == 0){
			_copyAllTables = false;
		}
		else{
			_copyAllTables = true;	// default, was err in xml though
			//System.err.println("\"all\" attribute incorrect in tag <tables>.  Must" +
			//		" be set to \"true\" or \"false\".  Using default value of true.");
			String err = "\"all\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of true.";
			_logger.warn(err);
		}
		
		// get copy indices
		tmpStr = elTables.getAttribute("indices");
		if(tmpStr == null || tmpStr.length() < 1 || tmpStr.compareToIgnoreCase("true") == 0){
			_copyIndices = true;
		}
		else if(tmpStr.compareToIgnoreCase("false") == 0){
			_copyIndices = false;
		}
		else{
			_copyIndices = true;
			//System.err.println("\"indices\" attribute incorrect in tag <tables>.  Must" +
			//	" be set to \"true\" or \"false\".  Using default value of true.");
			String err = "\"indices\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of true.";
			_logger.warn(err);
		}
		
		// get create tables
		tmpStr = elTables.getAttribute("createTables");
		if(tmpStr == null || tmpStr.length() < 1 || tmpStr.compareToIgnoreCase("false") == 0){
			_createTables = false;
		}
		else if(tmpStr.compareToIgnoreCase("true") == 0){
			_createTables = true;
		}
		else{
			_createTables = false;
			//System.err.println("\"createTables\" attribute incorrect in tag <tables>.  Must" +
			//	" be set to \"true\" or \"false\".  Using default value of false.");
			String err = "\"createTables\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of false.";
			_logger.warn(err);
		}
		
		// get copy data
		tmpStr = elTables.getAttribute("copyData");
		if(tmpStr == null || tmpStr.length() < 1 || tmpStr.compareToIgnoreCase("true") == 0){
			_copyData = true;
		}
		else if(tmpStr.compareToIgnoreCase("false") == 0){
			_copyData = false;
		}
		else{
			_copyData = Boolean.getBoolean(tmpStr);
			//System.err.println("\"copyData\" attribute incorrect in tag <tables>.  Must" +
			//	" be set to \"true\" or \"false\".  Using default value of true.");
			String err = "\"copyData\" attribute incorrect in tag <tables>.  Must" +
				" be set to \"true\" or \"false\".  Using default value of true.";
			_logger.info(err);
		}
		
		// get table tags
		Element tempelem = null;
		NodeList nlTables = elTables.getElementsByTagName("table");
		for(int curTable = 0; curTable < nlTables.getLength(); curTable++){
			if(nlTables.item(curTable).getNodeType() == Node.ELEMENT_NODE){
				_tables.add(new CfgTable((Element)nlTables.item(curTable), _createTables, _copyData));
				tempelem = (Element)nlTables.item(curTable);
				if(tempelem.getAttribute("newName")!= null){
					if(!tempelem.getAttribute("newName").trim().equalsIgnoreCase("")){
						_tablesByNewName.put(tempelem.getAttribute("newName"),new CfgTable((Element)nlTables.item(curTable), _createTables, _copyData));
					}
				}
			}
		}
	}
}
