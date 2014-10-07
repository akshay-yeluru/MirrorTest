call setPaths.bat
java -mx32M -cp %SRB_WAREHOUSE_HOME%\lib\* com.tscsoftware.warehouse.Main payrollFinance.properties > ../log/rebuildSchemaPayrollFinance.log
