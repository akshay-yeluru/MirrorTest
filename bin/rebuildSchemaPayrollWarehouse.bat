REM please note that it does not matter what payroll properties file is used so long as it has the JOB_TYPE=PAYROLL.  
java -mx32M -cp ../lib/SRBWarehouseSchemaBuilder-0.0.1-SNAPSHOT.jar;../lib/jtds-1_2.jar;../lib/log4j-1_2_12.jar;../lib/vortex_2005_08_30.jar com.tscsoftware.warehouse.Main payrollTEAC.properties > ../log/rebuildSchemaPayroll.log
