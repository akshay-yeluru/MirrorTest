call setPaths.bat
java -mx32M -cp %JAVA_LIB_CLASSPATH% com.tscsoftware.warehouse.Main humanResources.properties > ../log/rebuildSchemaHumanResources.log
