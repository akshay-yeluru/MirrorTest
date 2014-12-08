call setPaths.bat
SET SRB_WAREHOUSE_JOB=Interfaces/Schoollogic/Job/startStagingToSchoollogic
SET SRB_WAREHOUSE_PROPERTIES=SL_Integration_to_SchoolLogic.properties
SET SRB_WAREHOUSE_LOG=StagingToSchoolLogic.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_CLIENT%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_CLIENT%\\bin\\%SRB_WAREHOUSE_PROPERTIES%