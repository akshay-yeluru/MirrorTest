/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.tscsoftware.warehouse.cfg.Config;
import com.tscsoftware.warehouse.cfg.ConfigException;
import com.tscsoftware.warehouse.db.Db;


/**
 * This is the main warehouse class which completes all tasks in turn.
 * 
 * @author Jason S
 */
public class Warehouse {
	
	private final short MAX_RETRY = 3;
	
	static private Logger _logger;
	
	private Config _cfg;
	
	private String sourceDriver;
	private String sourceConnectionURL;
	private String targetDriver;
	private String targetConnectionURL;
	
	/**
	 * Build this object and read the configuration XML file.
	 * 
	 * @param configFile Full or relative path and filename of the properties file.
	 * @throws ConfigException Any config error.
	 * @throws IOException Error reading the file.
	 */
	Warehouse(String propertiesFile) throws Exception,ConfigException, IOException{
		_logger = Logger.getLogger(Warehouse.class);
		
		Properties props = new Properties();
	    try 
	    {
	    	
	    	FileInputStream in = new FileInputStream(propertiesFile);
	    	props.load(in);
	    	in.close();
	    	
	    } catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    sourceDriver = props.getProperty("SOURCE_DRIVER");
	    sourceConnectionURL = props.getProperty("SOURCE_CONNECTION_URL");
		
	    targetDriver = props.getProperty("TARGET_DRIVER");
	    targetConnectionURL = props.getProperty("TARGET_CONNECTION_URL");
	    
	    String jobType = props.getProperty("JOB_TYPE");

		_cfg = new Config();
		InputStream configFile = null;
		
		// load config xml resource file from java resource
	    if ("FINANCE".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("finance_config.xml");
	    }else if ("RECEIVABLES".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("receivables_config.xml");
	    }else if ("HUMAN_RESOURCES".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("human_resources_config.xml");
	    }else if ("PAYROLL".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("payroll_config.xml");
	    }else if ("PAYROLL_HUMAN_RESOURCES".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("payroll_human_resources_config.xml");
	    }else if ("PAYROLL_FINANCE".equals(jobType)){
	    	configFile = (InputStream) ClassLoader.getSystemResourceAsStream("payroll_finance_config.xml");
	    }else{
	    	throw new WarehouseException("No configruation file for job type.");
	    }
		
		_cfg.parse(configFile);
	}
	
	/**
	 * Copy the databases based on the parsed XML file.
	 * 
	 * @throws Exception Any error encountered.
	 */
	public void copyDb() throws Exception{
		
		Db dbDst = null;
		Db dbSrc = null;
		try{
			// get database class
			if(_cfg.getDstType() == null || _cfg.getSrcType() == null){
				throw new Exception("Source or destination database type not defined");
			}
			String dstClass = "com.tscsoftware.warehouse.db." + _cfg.getDstType() + ".Db";
			String srcClass = "com.tscsoftware.warehouse.db." + _cfg.getSrcType() + ".Db";
			
			// setup databases
			dbDst = (Db)Class.forName(dstClass).newInstance();
			dbSrc = (Db)Class.forName(srcClass).newInstance();
			dbDst.init(_cfg, _cfg.getDstName(), targetDriver, targetConnectionURL);
			dbSrc.init(_cfg, _cfg.getSrcName(), sourceDriver, sourceConnectionURL);
			
			// clear destination database
			if(_cfg.getClearDatabase()){
				try{
					dbDst.clearDb();
				}
				catch(SQLException e){
					String err = "Unable to remove tables from database " + _cfg.getDstName() + "." +
						" Attempting to continue copy.";
					_logger.error(err, e);
				}
			}
			
			// clear new tables in destination database
			if(_cfg.getClearNewTables()){
				try{
					dbDst.clearNewTables();;
				}
				catch(SQLException e){
					String err = "Unable to remove tables from database " + _cfg.getDstName() + "." +
						" Attempting to continue copy.";
					_logger.error(err, e);
				}
			}
			
			// copy from source to destination
			dbDst.copy(dbSrc);
		}
		catch(WarehouseException we){
			throw we;
		}
		catch(SQLException se){
			_logger.error("Unknown Database Exception", se);
			throw new WarehouseException("Unkown SQL error occurred.");
		}
		finally{
			// cleanup
			try{
				dbDst.close();
			}catch(Exception e){}
			try{
				dbSrc.close();
			}catch(Exception e){}
		}
	} //copyDb
}
