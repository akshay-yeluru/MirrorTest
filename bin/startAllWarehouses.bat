REM This script is used to start all of the different warehouse scripts for a client
REM
REM Finance
cmd /c startFinanceWarehouse.bat

REM Human Resources
cmd /c startHumanResourcesWarehouse.bat

REM Payroll Truncate - This must be run before the rest of the payroll scripts
cmd /c startPayrollTruncateWarehouse.bat

REM Payroll Finance Integration
cmd /c startPayrollFinanceWarehouse.bat

REM Payroll Human Resources Integration
cmd /c startPayrollHumanResourcesWarehouse.bat

REM Payroll 
cmd /c startPayrollTEACWarehouse.bat
