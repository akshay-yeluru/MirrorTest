call setPaths.bat
SET SRB_WAREHOUSE_JOB=Interfaces/Schoollogic_Stats/Job/startSL_Stats_to_Staging
SET SRB_WAREHOUSE_PROPERTIES=sl_stats_to_staging.properties
SET SRB_WAREHOUSE_LOG=schoollogic_stats.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_CLIENT%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_CLIENT%\\bin\\%SRB_WAREHOUSE_PROPERTIES%