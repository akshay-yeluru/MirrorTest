call setPaths.bat
SET SRB_WAREHOUSE_JOB=ReceivablesWarehouse/Job/startReceivablesWarehouse
SET SRB_WAREHOUSE_PROPERTIES=receivables.properties
SET SRB_WAREHOUSE_LOG=receivablesWarehouse.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_HOME%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_HOME%\\bin\\%SRB_WAREHOUSE_PROPERTIES%