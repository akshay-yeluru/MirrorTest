call setPaths.bat
SET SRB_WAREHOUSE_JOB=PayrollWarehouse/Job/startPayrollHumanResourcesWarehouse
SET SRB_WAREHOUSE_PROPERTIES=payrollHumanResources.properties
SET SRB_WAREHOUSE_LOG=payrollHumanResourcesWarehouse.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_HOME%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_HOME%\\bin\\%SRB_WAREHOUSE_PROPERTIES%