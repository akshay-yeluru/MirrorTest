REM please note that it does not matter what payroll properties file is used so long as it has the JOB_TYPE=PAYROLL.  
call setPaths.bat
java -mx32M -cp %JAVA_LIB_CLASSPATH% com.tscsoftware.warehouse.Main payrollTEAC.properties > ../log/rebuildSchemaPayroll.log
