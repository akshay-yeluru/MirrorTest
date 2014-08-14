SET PDI_HOME=C:\Users\MichaelC\Utilities\pdi-ce-5.1.0.0-752\data-integration
SET SRB_WAREHOUSE_HOME=C:\\Users\\MichaelC\\Development\\github\\SRBWarehouse
SET SRB_WAREHOUSE_JOB=FinanceWarehouse/Job/startFinanceWarehouse
SET SRB_WAREHOUSE_PROPERTIES=finance.properties
SET SRB_WAREHOUSE_LOG=finance.log

%PDI_HOME%\kitchen.bat /rep:KettleRepo /job:%SRB_WAREHOUSE_JOB% /log:%SRB_WAREHOUSE_HOME%\log\%SRB_WAREHOUSE_LOG% /param:SRB_PROPERTIES_FILE_PARAM=%SRB_WAREHOUSE_HOME%\\bin\\%SRB_WAREHOUSE_PROPERTIES%