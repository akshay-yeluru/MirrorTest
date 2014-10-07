package com.tscsoftware.warehouse;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class VortexVersion {

	static Logger _logger = Logger.getLogger(VortexVersion.class);

	public static void main(String[] args) {
		try {

			// get properties file or die.
			if (args.length != 1) {

				// inform of proper usage
				String err = "Incorrect usage.  Include the configuration properties file as the first argument.";
				_logger.error(err);
				System.exit(0);
			}

			BasicConfigurator.configure();

			Properties props = new Properties();
			try {

				FileInputStream in = new FileInputStream(args[0]);
				props.load(in);
				in.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			String sourceDriver = props.getProperty("SOURCE_DRIVER");
			String sourceConnectionURL = props
					.getProperty("SOURCE_CONNECTION_URL");

			Connection conn = null;
			DatabaseMetaData dmd;

			Class.forName(sourceDriver);
			conn = DriverManager.getConnection(sourceConnectionURL);
			dmd = conn.getMetaData();

			_logger.info("getDriverVersion: " + dmd.getDriverVersion());

			if (conn != null)
				conn.close();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

}
