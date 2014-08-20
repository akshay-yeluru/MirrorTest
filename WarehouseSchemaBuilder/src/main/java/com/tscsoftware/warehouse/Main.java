/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.tscsoftware.warehouse.cfg.ConfigException;



/**
 * @author Jason
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Main {

	static Logger _logger = Logger.getLogger(Main.class);
	
	public static void main(String[] args) {
		System.out.println("Starting warehouse");
		
		long startTime = System.currentTimeMillis();
		
		// get config xml file
		if(args.length < 1 || args.length > 2){
			BasicConfigurator.configure();
			// inform of proper usage
			String err = "Incorrect usage.  Include the configuration xml file as the first argument, and" +
					"optionally a logging xml file as the second.";
			_logger.error(err);
		}
		else{
			// configure logger
			if(args.length == 2){
				// log according to configuration file
				try{
					DOMConfigurator.configure(args[1]);
				}
				catch(Exception e){
					// no logging info
					BasicConfigurator.configure();
					_logger.warn("Unable to load and parse logging configuration file " + args[1]);
				}
			}
			else{
				// no logging file, log to STDOUT
				BasicConfigurator.configure();
			}
			
			// warehouse
			_logger.debug("Starting warehouse");

			try{
				Warehouse warehouse = new Warehouse(args[0]);
				warehouse.copyDb();
			}
			catch(ConfigException e){
				_logger.error("Unkown configuration error occurred", e);
			}
			catch(IOException e){
				_logger.error("Unknown IO error occurred", e);
			}
			catch(Exception e){
				_logger.error("Unknown error occurred", e);
			}
		}
		
		float ttlTime = (System.currentTimeMillis() - startTime) / 1000;
		_logger.info("Done warehouse in " + ttlTime + " seconds.");
	}
	
}
