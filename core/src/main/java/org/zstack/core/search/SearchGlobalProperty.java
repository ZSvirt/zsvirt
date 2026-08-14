package org.zstack.core.search;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:23 2020/11/18
 */
@GlobalPropertyDefinition
public class SearchGlobalProperty {
    @GlobalProperty(name="Search.autoRegister", defaultValue = "true")
    public static boolean SearchAutoRegister = true;
    @GlobalProperty(name="Search.indexBaseDir")
    public static String SearchIndexBaseDir;
    @GlobalProperty(name="IndexWorker.execution")
    public static String IndexWorkerExecution;
    @GlobalProperty(name="IndexWorker.flushInterval")
    public static String IndexWorkerFlushInterval;
    @GlobalProperty(name="MassIndexer.ThreadsToLoadObjects", defaultValue = "8")
    public static int massIndexerThreadsToLoadObjects;
    @GlobalProperty(name="MassIndexer.BatchSizeToLoadObjects", defaultValue = "100")
    public static int massIndexerBatchSizeToLoadObjects;
    @GlobalProperty(name="JGroup.InfinispanInitialHosts")
    public static String JGroupInfinispanInitialHosts;
    @GlobalProperty(name="JGroup.BackendInitialHosts")
    public static String JGroupBackendInitialHosts;
    //for infinispan
    @GlobalProperty(name="JGroup.InfinispanPort", defaultValue = "7800")
    public static String JGroupInfinispanPort;
    //for jgroup backend
    @GlobalProperty(name="JGroup.BackendPort", defaultValue = "7805")
    public static String JGroupBackendPort;
    @GlobalProperty(name="Exclusive.indexUse", defaultValue = "false")
    public static String ExclusiveIndexUse;
    @GlobalProperty(name="JGroup.JoinTimeout", defaultValue = "1000")
    public static String JGroupJoinTimeout;
    @GlobalProperty(name="JGroup.FlushBypass", defaultValue = "true")
    public static String JGroupFlushBypass;
}
