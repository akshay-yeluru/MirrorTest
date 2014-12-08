call setPaths.bat
SET SRB_WAREHOUSE_JOB=Interfaces/Staging/Job/startAtrieveToStaging
SET SRB_WAREHOUSE_PROPERTIES=sl_integration_to_staging.properties
SET SRB_WAREHOUSE_LOG=AtrieveToStaging.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_CLIENT%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_CLIENT%\\bin\\%SRB_WAREHOUSE_PROPERTIES%