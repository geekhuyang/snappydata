INCLUDE $JTESTS/io/snappydata/hydra/connectionPool/connPool.inc;

INCLUDE $JTESTS/io/snappydata/hydra/consistency/columnTableExtnSingleBucket_NonJDBC.inc;

INCLUDE $JTESTS/io/snappydata/hydra/consistency/startSnappyCluster.inc;

//INCLUDE $JTESTS/io/snappydata/hydra/testDMLOps/startDerby.inc;

INITTASK     taskClass   = io.snappydata.hydra.consistency.SnappyConsistencyTest taskMethod  = HydraTask_initialize
	threadGroups = snappyThreads,dmlThreads,selectThreads;

INITTASK     taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod  = HydraTask_initialize
	threadGroups = snappyThreads,dmlThreads,selectThreads;

INITTASK     taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod  = HydraTask_createSnappySchemas
	threadGroups = snappyThreads;

INITTASK     taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod  = HydraTask_createSnappyTables
	threadGroups = snappyThreads;

INITTASK     taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod  = HydraTask_saveTableMetaDataToBB
  threadGroups = snappyThreads;

INITTASK     taskClass   = io.snappydata.hydra.consistency.SnappyConsistencyTest taskMethod  = HydraTask_registerDMLThreads
  threadGroups = dmlThreads;

//Load some data in the data, before starting any operations.
INITTASK     taskClass   = io.snappydata.hydra.consistency.SnappyConsistencyTest taskMethod  = HydraTask_populateTables
  io.snappydata.hydra.testDMLOps.SnappySchemaPrms-batchSize =5000000
  threadGroups = snappyThreads;

//task for verifying the populated data.
//INITTASK   taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod  = HydraTask_verifyResultsInitTask
//  threadGroups = snappyThreads;

//task for running dmlOps
TASK        taskClass   = io.snappydata.hydra.consistency.SnappyConsistencyTest taskMethod  = HydraTask_performDMLOPsAndVerifyConsistencyInJob
  io.snappydata.hydra.cluster.SnappyPrms-jobClassNames = io.snappydata.hydra.consistency.ValidateConsistencyWithDMLOpsJob
  io.snappydata.hydra.cluster.SnappyPrms-userAppJar = snappydata-store-scala-tests*tests.jar
  io.snappydata.hydra.cluster.SnappyPrms-hasDynamicAppProps = true
io.snappydata.hydra.testDMLOps.SnappySchemaPrms-batchSize =1000
  threadGroups = dmlThreads;


//task for running select queries at the end of test
// CLOSETASK   taskClass   = io.snappydata.hydra.testDMLOps.SnappyDMLOpsUtil taskMethod = HydraTask_verifyResultsInitTask
//  threadGroups = snappyThreads;

INCLUDE $JTESTS/io/snappydata/hydra/consistency/stopSnappyCluster.inc;

INCLUDE $JTESTS/io/snappydata/hydra/testDMLOps/stopDerby.inc;

hydra.Prms-totalTaskTimeSec           = 450;
hydra.Prms-maxResultWaitSec           = 900;
hydra.Prms-maxCloseTaskResultWaitSec  = 900;

io.snappydata.hydra.testDMLOps.SnappySchemaPrms-testUniqueKeys = true;

io.snappydata.hydra.testDMLOps.SnappySchemaPrms-useCsv = false;