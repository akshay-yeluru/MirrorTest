call setPaths.bat
SET SRB_WAREHOUSE_JOB="Interfaces/Electronic Educator Profile/Job/EEP"
SET SRB_WAREHOUSE_PROPERTIES=eep.properties
SET SRB_WAREHOUSE_LOG=eep.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_CLIENT%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_CLIENT%\\bin\\%SRB_WAREHOUSE_PROPERTIES%