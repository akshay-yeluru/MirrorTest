function Write-SetPath-File{
param(  [string]$UtilitiesDirectory,
		[string]$ClientDirectory
		)
		
$PdiHomeDirectory = Join-Path $UtilitiesDirectory 'pdi-ce-5.1.0.0-752\data-integration' 
$WarehouseHomeDirectory = Join-Path $UtilitiesDirectory 'warehousePDI\SRBWarehouse'
$WarehouseClientDirectory = Join-Path $UtilitiesDirectory "warehousePDI\$ClientDirectory"
$JavaLibClasspath = 'SET JAVA_LIB_CLASSPATH=%SRB_WAREHOUSE_HOME%\lib\jtds-1.2.jar;%SRB_WAREHOUSE_HOME%\lib\log4j-1.2.12.jar;%SRB_WAREHOUSE_HOME%\lib\SRBWarehouseSchemaBuilder-0.0.1-SNAPSHOT.jar;%SRB_WAREHOUSE_HOME%\lib\vortex-2006.02.28.jar'

$SetPathFile = Join-Path $WarehouseClientDirectory 'bin\setPaths.bat'
		
If (Test-Path $SetPathFile){
	Remove-Item $SetPathFile
}
	
#The double slash allows these environment variables to work within java.	
$WarehouseHomeDirectory = $WarehouseHomeDirectory -replace '\\', '\\'
$WarehouseClientDirectory = $WarehouseClientDirectory -replace '\\', '\\'
	
#SET PDI_HOME=C:\utilities\pdi-ce-5.1.0.0-752\data-integration
#SET SRB_WAREHOUSE_HOME=C:\\utilities\\warehousePDI\\SRBWarehouse
#SET SRB_WAREHOUSE_CLIENT=C:\\utilities\\warehousePDI\\dev
#SET JAVA_LIB_CLASSPATH=%SRB_WAREHOUSE_HOME%\lib\jtds-1.2.jar;%SRB_WAREHOUSE_HOME%\lib\log4j-1.2.12.jar;%SRB_WAREHOUSE_HOME%\lib\SRBWarehouseSchemaBuilder-0.0.1-SNAPSHOT.jar;%SRB_WAREHOUSE_HOME%\lib\vortex_2006_02_28.jar

"SET PDI_HOME=$PdiHomeDirectory" | Out-File $SetPathFile -Encoding ASCII
"SET SRB_WAREHOUSE_HOME=$WarehouseHomeDirectory" | Out-File $SetPathFile -Append  -Encoding ASCII
"SET SRB_WAREHOUSE_CLIENT=$WarehouseClientDirectory" | Out-File $SetPathFile -Append  -Encoding ASCII
"$JavaLibClasspath" | Out-File $SetPathFile -Append  -Encoding ASCII

}

function Write-Run-SetPath-File{
param(  [string]$UtilitiesDirectory,
		[string]$ClientDirectory
		)
		
$WarehouseClientDirectory = Join-Path $UtilitiesDirectory "warehousePDI\$ClientDirectory"

$RebuildSchemaAllFile = Join-Path $WarehouseClientDirectory 'bin\rebuildSchemaAll.bat'
		
If (Test-Path $RebuildSchemaAllFile){
	Write-Host 'File to Rebuild All Schemas already exists.'
}else{
	'cd %~dp0' | Out-File $RebuildSchemaAllFile -Encoding ASCII
	'cmd /c rebuildSchemaFinanceWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
	'cmd /c rebuildSchemaHumanResourcesWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
	'cmd /c rebuildSchemaPayrollFinanceWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
	'cmd /c rebuildSchemaPayrollHumanResourcesWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
	'cmd /c rebuildSchemaPayrollWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
	'cmd /c rebuildSchemaReceivablesWarehouse.bat' | Out-File $RebuildSchemaAllFile -Append -Encoding ASCII
}

Write-Host 'RebuildSchemaAllFile: ' $RebuildSchemaAllFile
& $RebuildSchemaAllFile

}


###########################################################################################
################################### VARIABLE DECLARATION ##################################
###########################################################################################

Write-Host 'Start Warehouse Upgrade'

#$ServerUtilitiesDirectory = $OctopusParameters['Server.Utilities.Directory']
$ServerUtilitiesDirectory = 'C:\Utilities'

$WarehouseDirectoryName = Join-Path $ServerUtilitiesDirectory 'warehousePDI'

#Unit Testing
#$InstallationDirectoryPath = 'C:\Utilities\zip\7za920'

###########################################################################################
################################### PRINT VARIABLES #######################################
###########################################################################################

Write-Host 'Start Print Variables'

Write-Host 'ServerUtilitiesDirectory: ' $ServerUtilitiesDirectory
Write-Host 'WarehouseDirectoryName: ' $WarehouseDirectoryName

###########################################################################################
#################################### SCRIPT LOGIC #########################################
###########################################################################################

#Warehouse Post Deploy Script
#Get all directories in c:\utilities\warehousePDI
#Ignore SRBWarehouse
#foreach directory found
#	Write out a new Set Path.bat file from a template into \bin directory
#	Run each rebuildSchema*.bat file found in \bin directory
#Done


Write-Host 'Start Script Logic'

$ChildDirectories = Get-ChildItem -dir $WarehouseDirectoryName #lists only directories

foreach($Child in $ChildDirectories){

if (-Not ($Child -like 'SRBWarehouse')){
	Write-Host 'Child: ' $Child
	Write-SetPath-File $ServerUtilitiesDirectory $Child
	Write-Run-SetPath-File $ServerUtilitiesDirectory $Child
	}
}

Write-Host 'Finish Warehouse Upgrade'