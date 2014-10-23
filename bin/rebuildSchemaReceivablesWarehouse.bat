call setPaths.bat
java -mx32M -cp %JAVA_LIB_CLASSPATH% com.tscsoftware.warehouse.Main receivables.properties > ../log/rebuildSchemaReceivables.log
