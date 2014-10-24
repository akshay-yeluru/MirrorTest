call setPaths.bat
SET SRB_WAREHOUSE_JOB=HumanResourcesWarehouse/Job/startHumanResourcesWarehouse
SET SRB_WAREHOUSE_PROPERTIES=humanResources.properties
SET SRB_WAREHOUSE_LOG=humanResourcesWarehouse.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_CLIENT%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_CLIENT%\\bin\\%SRB_WAREHOUSE_PROPERTIES%