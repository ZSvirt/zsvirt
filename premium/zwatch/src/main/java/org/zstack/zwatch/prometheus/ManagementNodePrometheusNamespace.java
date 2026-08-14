package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.core.thread.ChainTaskStatistic;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeMXBean;
import org.zstack.core.thread.ThreadPoolStatistic;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.StaticInit;
import org.zstack.utils.Bash;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.ManagementNodeNamespace;

import javax.persistence.Query;
import java.util.*;

public class ManagementNodePrometheusNamespace extends AbstractPrometheusNamespace {
    private static DatabaseFacade dbf;
    private static ThreadFacade thdf;
    private static CloudBus bus;
    private static double queries;
    private static double transactions;
    private static double msgNum;
    private static double lastMsgNum;

    private static String mgmtIp;
    private static String mysqlPid;

    private static long loops;
    /**
     * Following metrics do not need or not well support frequently
     * collect, we use loops as a tick to extend their collect
     * interval.
     * But prometheus require metric follow its scrape interval to
     * return metric value, so use a static local variable as metric
     * value cache to satisfy prometheus scrape rule.
     */
    private static CounterMetricFamily ManagementProgressGCStatus = null;
    private static CounterMetricFamily MysqlTableSize = null;

    @StaticInit
    static void staticInit () {
        PrometheusNamespace.namespacesClasses.put(ManagementNodeNamespace.class, ManagementNodePrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new ManagementCollector());
    }

    public ManagementNodePrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    private static Map<String, String> metricMap = new HashMap<>();
    private static Map<String, String> metricExpressionMap = new HashMap<>();

    static {
        metricMap.put(ManagementNodeNamespace.TimeNeededToSyncDB.getName(), "zstack_mn_db_time_to_sync");
        metricMap.put(ManagementNodeNamespace.DbFencerIpReachable.getName(), "zstack_db_fencer_ip_reachable");

        metricMap.put(ManagementNodeNamespace.MNProgressLasts.getName(), "zstack_mn_lasts");
        metricMap.put(ManagementNodeNamespace.MNProgressSocketNum.getName(), "zstack_mn_socket_num");
        metricMap.put(ManagementNodeNamespace.MNProgressGCStatus.getName(), "zstack_mn_gc_status");
        metricMap.put(ManagementNodeNamespace.MNProgressExpends.getName(), "zstack_mn_resource_expends");

        metricMap.put(ManagementNodeNamespace.MNThreadPoolStatus.getName(), "zstack_mn_thread_pool");
        metricMap.put(ManagementNodeNamespace.MNQueueStatus.getName(), "zstack_mn_queue");
        metricMap.put(ManagementNodeNamespace.MNProgressMsgNum.getName(), "zstack_mn_msg_num");

        metricMap.put(ManagementNodeNamespace.MysqlLasts.getName(), "mysql_lasts");
        metricMap.put(ManagementNodeNamespace.MysqlProgressExpends.getName(), "mysql_expends");
        metricMap.put(ManagementNodeNamespace.MysqlSlowQuery.getName(), "mysql_slow_query");
        metricMap.put(ManagementNodeNamespace.MysqlProcessLists.getName(), "mysql_processlist");

        metricMap.put(ManagementNodeNamespace.MysqlTableSize.getName(), "mysql_table_size");
        metricMap.put(ManagementNodeNamespace.MysqlOpenttables.getName(), "mysql_opentables");
        metricMap.put(ManagementNodeNamespace.MysqlQuerys.getName(), "mysql_queries");
        metricMap.put(ManagementNodeNamespace.MysqlTransactions.getName(), "mysql_transactions");
        metricMap.put(ManagementNodeNamespace.MysqlQueryCache.getName(), "mysq_query_cache");
        metricMap.put(ManagementNodeNamespace.MysqlDbError.getName(), "mysql_db_error");


        metricMap.put(ManagementNodeNamespace.ErrorCodes.getName(), "zstack_errorcodes");

    }


    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metricExpressionMap.get(metric.getName()) != null) {
            rule.setExpression(metricExpressionMap.get(metric.getName()));
        } else if (metricMap.get(metric.getName()) != null) {
            rule.setExpression(metricMap.get(metric.getName()));
        } else {
            rule.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l->rule.labelMapping(l, l));
        }

        return rule;
    }

    public static class ManagementCollector implements MetricCollector {
        @Override
        public boolean skipManagementNodeCheck() {
            return true;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            mgmtIp = Platform.getManagementServerIp();
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            loops += 1;

            long start = System.currentTimeMillis();

            samples.addAll(progressStatusMetrics());
            samples.addAll(mysqlStatusMetrics());
            samples.addAll(errorCodesMetrics());

            if (loops >= 10000) {
                loops = 0;
            }
            long end = System.currentTimeMillis();
            logger.debug(String.format("collect metrics from ManagementNodePrometheusNamespace spend %s ms", end - start));
            return samples;
        }

        @Override
        public String getCollectorName() {
            return ManagementCollector.class.getName();
        }

        private List<String> getMysqlStatus(String key) {
            String sql = String.format("show global status like '%s'", key);
            Query q = dbf.getEntityManager().createNativeQuery(sql);
            Object obj = q.getSingleResult();
            return JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(obj), ArrayList.class, String.class);
        }

        @ExceptionSafe
        private void addMysqlQps(CounterMetricFamily metrics) {
            List<String> rets = getMysqlStatus("queries");
            List<String> l = Arrays.asList(mgmtIp, "total");
            double ret = Double.parseDouble(rets.get(1).trim());
            metrics.addMetric(l, ret);

            if (queries > 0 && ret >= queries) {
                l = Arrays.asList(mgmtIp, "delta");
                metrics.addMetric(l, ret - queries);
            }
            queries = ret;
        }

        @ExceptionSafe
        private void addMysqlTps(CounterMetricFamily metrics) {
            List<String> commits = getMysqlStatus("com_commit");
            List<String> rollbacks = getMysqlStatus("com_rollback");
            List<String> l = Arrays.asList(mgmtIp, "total");
            double ret = Double.parseDouble(commits.get(1).trim()) + Double.parseDouble(rollbacks.get(1).trim());
            metrics.addMetric(l, ret);

            if (transactions > 0 && ret >= transactions){
                l = Arrays.asList(mgmtIp, "delta");
                metrics.addMetric(l, ret - transactions);
            }
            transactions = ret;
        }

        private static String getMysqlPid() {
            final String[] pid = new String[1];
            new Bash() {
                @Override
                @ExceptionSafe
                protected void scripts() {
                    unsetE();
                    run("pgrep -f 'mysqld.*datadir'");
                    if (lastReturnCode == 0) {
                        pid[0] = stdout().trim();
                    }
                }
            }.execute();
            return pid[0];
        }

        @ExceptionSafe
        private void addMysqlProgress(CounterMetricFamily metrics1, CounterMetricFamily metrics2) {
            if (mysqlPid == null) {
                mysqlPid = getMysqlPid();
            }
            
            if (mysqlPid != null) {
                ProgressStatus status = ProgressMonitorHelper.getProgressStatus(mysqlPid);
                if (status.existed) {
                    metrics2.addMetric(Arrays.asList(mgmtIp, "cpu"), status.cpuExpends);
                    metrics2.addMetric(Arrays.asList(mgmtIp, "memrss"), status.memoryExpends);
                    metrics1.addMetric(Arrays.asList(mgmtIp), status.lasts);
                } else {
                    logger.warn("mysql process is not existed, skip getProgressStatus from it");
                    mysqlPid = null;
                }
            }
        }

        @ExceptionSafe
        private void addMysqlStatus(CounterMetricFamily metrics, String key, String...labels) {
            String sql = String.format("show global status like '%s'", key);
            final List<String> l = new ArrayList<>();
            l.add(mgmtIp);
            l.addAll(Arrays.asList(labels));

            processMysqlResults(sql, new ForEachFunction<Map<String, String>>() {
                @Override
                public void run(Map<String, String> ret) {
                    if (ret.get("name").trim().equalsIgnoreCase(key)) {
                        metrics.addMetric(l, Double.parseDouble(ret.get("value").trim()));
                    }

                }
            }, "name", "value");
        }

        @ExceptionSafe
        private void addMysqlProcesslist(CounterMetricFamily metrics) {
            String sql = "select db,command,count(1) from information_schema.processlist group by db,command";
            Map<String, Double> totals = new HashMap<>();
            processMysqlResults(sql, new ForEachFunction<Map<String, String>>() {
                @Override
                public void run(Map<String, String> ret) {
                    double num = Double.parseDouble(ret.get("count").trim());
                    String db = ret.get("db");
                    metrics.addMetric(Arrays.asList(mgmtIp, db, ret.get("command").trim().toLowerCase()), num);
                    totals.compute(db, (k, v) -> v==null ? num : v + num);
                }
            }, "db", "command", "count");

            totals.forEach((k, v) -> {
                metrics.addMetric(Arrays.asList(mgmtIp, k, "total"), v);
            });

        }

        private void addMysqlQueryCache(CounterMetricFamily metrics) {
            String sql = "show status like 'qcache_%'";
            double[] caches = new double[2];
            processMysqlResults(sql, new ForEachFunction<Map<String, String>>() {
                @Override
                public void run(Map<String, String> ret) {
                    if (ret.get("name").equalsIgnoreCase("Qcache_hits")) {
                        caches[0] = Double.parseDouble(ret.get("value").trim());
                    }
                    if (ret.get("name").equalsIgnoreCase("Qcache_inserts")) {
                        caches[1] = Double.parseDouble(ret.get("value").trim());
                    }
                }
            }, "name", "value");
            metrics.addMetric(Arrays.asList(mgmtIp, "hits"), caches[0]);
            metrics.addMetric(Arrays.asList(mgmtIp, "inserts"), caches[1]);
            metrics.addMetric(Arrays.asList(mgmtIp, "ratio"), (caches[0]) / (caches[0] + caches[1] + 1));
        }

        @ExceptionSafe
        private void addMysqlOpentables(CounterMetricFamily metrics) {
            String sql = "show open tables where in_use > 0";
            double[] total = new double[1];
            processMysqlResults(sql, new ForEachFunction<Map<String, String>>() {
                @Override
                public void run(Map<String, String> ret) {
                    if (ret.get("in_use") != null) {
                        double inUse = Double.parseDouble(ret.get("in_use").trim());
                        metrics.addMetric(Arrays.asList(mgmtIp, ret.get("db") + "." + ret.get("table")), inUse);
                        total[0] += inUse;
                    }
                }
            }, "db", "table", "in_use", "locked");
            metrics.addMetric(Arrays.asList(mgmtIp, "total"), total[0]);
        }

        @ExceptionSafe
        private void addMysqlTableSize(CounterMetricFamily metrics, String type) {
            String sql = String.format("SELECT TABLE_SCHEMA,sum(round((%s_LENGTH)/1024/1024,2)) FROM information_schema.TABLES WHERE TABLE_SCHEMA like 'zstack%%' group by TABLE_SCHEMA", type);
            processMysqlResults(sql, (ForEachFunction<Map<String, String>>) ret -> metrics.addMetric(Arrays.asList(mgmtIp, ret.get("db"), type), Double.parseDouble(ret.get("sizeMB").trim())), "db", "sizeMB");
        }

        private <V> void processMysqlResults(String sql, ForEachFunction<V> addMetrics, String...args) {
            if (args.length == 0) {
                return;
            }
            Query q = dbf.getEntityManager().createNativeQuery(sql);
            List<Object> objs = q.getResultList();

            if (objs.isEmpty()) {
                return;
            }


            for (Object obj: objs) {
                List<String> rets = JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(obj), ArrayList.class, String.class);
                int delta = rets.size() % args.length;
                if (delta != 0) {
                    if (CoreGlobalProperty.UNIT_TEST_ON) {
                        throw new RuntimeException(String.format("we issue mysql results have %d cols, but got %d actually.", args.length, delta));
                    } else {
                        logger.warn(String.format("we issue mysql results have %d cols, but got %d actually.", args.length, delta));
                        continue;
                    }
                }
                Map<String, String> line = new HashMap<>();
                for (int i = 0; i < rets.size(); i ++) {
                    line.put(args[i], rets.get(i));
                }
                addMetrics.run((V)line);
            }
        }

        private List<Collector.MetricFamilySamples> mysqlStatusMetrics() {
            if (dbf == null) {
                dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
            }

            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            CounterMetricFamily MysqlLasts = createCounterMetric(ManagementNodeNamespace.MysqlLasts);
            CounterMetricFamily MysqlProgressExpends = createCounterMetric(ManagementNodeNamespace.MysqlProgressExpends);
            CounterMetricFamily MysqlSlowQuery = createCounterMetric(ManagementNodeNamespace.MysqlSlowQuery);
            CounterMetricFamily MysqlProcessLists = createCounterMetric(ManagementNodeNamespace.MysqlProcessLists);

            boolean needUpdate = (MysqlTableSize == null || loops % 60 == 0);

            // refresh mysql table size
            if (needUpdate) {
                MysqlTableSize = createCounterMetric(ManagementNodeNamespace.MysqlTableSize);
                addMysqlTableSize(MysqlTableSize, "DATA");
                addMysqlTableSize(MysqlTableSize, "INDEX");
            }

            CounterMetricFamily MysqlQuerys = createCounterMetric(ManagementNodeNamespace.MysqlQuerys);
            CounterMetricFamily MysqlTransactions = createCounterMetric(ManagementNodeNamespace.MysqlTransactions);
            CounterMetricFamily MysqlOpenttables = createCounterMetric(ManagementNodeNamespace.MysqlOpenttables);
            CounterMetricFamily MysqlQueryCache = createCounterMetric(ManagementNodeNamespace.MysqlQueryCache);
            CounterMetricFamily MysqlDbError = createCounterMetric(ManagementNodeNamespace.MysqlDbError);


            samples.add(MysqlLasts);
            samples.add(MysqlProgressExpends);
            samples.add(MysqlSlowQuery);
            samples.add(MysqlProcessLists);
            samples.add(MysqlTableSize);
            samples.add(MysqlQuerys);
            samples.add(MysqlTransactions);
            samples.add(MysqlOpenttables);
            samples.add(MysqlQueryCache);
            samples.add(MysqlDbError);

            addMysqlProgress(MysqlLasts, MysqlProgressExpends);
            addMysqlStatus(MysqlSlowQuery, "slow_queries");
            addMysqlQps(MysqlQuerys);
            addMysqlTps(MysqlTransactions);
            addMysqlProcesslist(MysqlProcessLists);
            addMysqlOpentables(MysqlOpenttables);
            addMysqlQueryCache(MysqlQueryCache);
            MysqlDbError.addMetric(Arrays.asList(mgmtIp, "total"), DatabaseFacadeImpl.getDberror().doubleValue());
            MysqlDbError.addMetric(Arrays.asList(mgmtIp, "deadlock"), DatabaseFacadeImpl.getDbdeadlock().doubleValue());
            MysqlDbError.addMetric(Arrays.asList(mgmtIp, "locktimeout"), DatabaseFacadeImpl.getDblocktimeout().doubleValue());


            return samples;
        }

        private List<Collector.MetricFamilySamples> errorCodesMetrics() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            CounterMetricFamily ErrorCodes = createCounterMetric(ManagementNodeNamespace.ErrorCodes);
            double[] total = new double[1];
            Platform.getErrorCounter().forEach((k, v) -> {
                total[0] += v;
                ErrorCodes.addMetric(Arrays.asList(mgmtIp, k), v);
            });
            ErrorCodes.addMetric(Arrays.asList(mgmtIp, "total"), total[0]);

            samples.add(ErrorCodes);

            return samples;
        }

        private List<Collector.MetricFamilySamples> progressStatusMetrics() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            String pid = Platform.getManagementPid();
            CounterMetricFamily ManagementProgressLasts = createCounterMetric(ManagementNodeNamespace.MNProgressLasts);
            CounterMetricFamily ManagementProgressSocketNum = createCounterMetric(ManagementNodeNamespace.MNProgressSocketNum);

            boolean needUpdate = (ManagementProgressGCStatus == null || loops % 6 == 0);

            // refresh gc stats
            if (needUpdate) {
                ManagementProgressGCStatus = createCounterMetric(ManagementNodeNamespace.MNProgressGCStatus);
                new Bash() {
                    @Override
                    protected void scripts() {
                        setE();
                        sudoRun("jstat -gcutil %s", pid);
                        String[] lines = stdout().split("\n");
                        String lastline = lines[lines.length - 1];
                        String[] gc = lastline.trim().split("\\s+");
                        if (gc.length == 11){
                            String ygc = gc[6];
                            String ygct = gc[7];
                            String fgc = gc[8];
                            String fgct = gc[9];
                            String gct = gc[10];
                            ManagementProgressGCStatus.addMetric(Arrays.asList(mgmtIp, "ygc"), Double.parseDouble(ygc));
                            ManagementProgressGCStatus.addMetric(Arrays.asList(mgmtIp, "ygct"), Double.parseDouble(ygct));
                            ManagementProgressGCStatus.addMetric(Arrays.asList(mgmtIp, "fgc"), Double.parseDouble(fgc));
                            ManagementProgressGCStatus.addMetric(Arrays.asList(mgmtIp, "fgct"), Double.parseDouble(fgct));
                            ManagementProgressGCStatus.addMetric(Arrays.asList(mgmtIp, "gct"), Double.parseDouble(gct));
                        }
                    }
                }.execute();
            }

            CounterMetricFamily ManagementProgressExpends = createCounterMetric(ManagementNodeNamespace.MNProgressExpends);

            CounterMetricFamily ManagementThreadPool = createCounterMetric(ManagementNodeNamespace.MNThreadPoolStatus);
            CounterMetricFamily ManagementQueue = createCounterMetric(ManagementNodeNamespace.MNQueueStatus);
            CounterMetricFamily MNProgressMsgNum = createCounterMetric(ManagementNodeNamespace.MNProgressMsgNum);

            samples.add(ManagementProgressLasts);
            samples.add(ManagementProgressSocketNum);
            samples.add(ManagementProgressGCStatus);
            samples.add(ManagementProgressExpends);
            samples.add(ManagementThreadPool);
            samples.add(ManagementQueue);
            samples.add(MNProgressMsgNum);

            ProgressStatus status = ProgressMonitorHelper.getProgressStatus(pid);
            ManagementProgressLasts.addMetric(Collections.singletonList(mgmtIp), status.lasts);
            ManagementProgressSocketNum.addMetric(Collections.singletonList(mgmtIp), status.sockets);
            ManagementProgressExpends.addMetric(Arrays.asList(mgmtIp, "cpu"), status.cpuExpends);
            ManagementProgressExpends.addMetric(Arrays.asList(mgmtIp, "memrss"), status.memoryExpends);

            if (thdf == null) {
                thdf = Platform.getComponentLoader().getComponent(ThreadFacade.class);
            }
            ThreadFacadeMXBean thdfBean = (ThreadFacadeMXBean)thdf;
            ThreadPoolStatistic poolStatistic = thdfBean.getThreadPoolStatistic();
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "total"), poolStatistic.getTotalThreadNum());
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "active"), poolStatistic.getActiveThreadNum());
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "core"), poolStatistic.getCorePoolSize());
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "max"), poolStatistic.getMaxPoolSize());
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "pending"), poolStatistic.getPendingTaskNum());
            ManagementThreadPool.addMetric(Arrays.asList(mgmtIp, "queued"), poolStatistic.getQueuedTaskNum());

            if (bus == null) {
                bus = Platform.getComponentLoader().getComponent(CloudBus.class);
            }
            MNProgressMsgNum.addMetric(Arrays.asList(mgmtIp, "current"), bus.getEnvelopeSize());
            MNProgressMsgNum.addMetric(Arrays.asList(mgmtIp, "total"), msgNum);
            MNProgressMsgNum.addMetric(Arrays.asList(mgmtIp, "delta"), msgNum - lastMsgNum);
            lastMsgNum = msgNum;


            Map<String, ChainTaskStatistic> chainTaskStatistics = thdfBean.getChainTaskStatistics();
            int[] tasks = new int[3];
            chainTaskStatistics.forEach((k, v) -> {
                tasks[0] += 1;
                tasks[1] += v.getCurrentRunningThreadNum();
                tasks[2] += v.getPendingTaskNum();
            });
            ManagementQueue.addMetric(Arrays.asList(mgmtIp, "tasks"), tasks[0]);
            ManagementQueue.addMetric(Arrays.asList(mgmtIp, "running"), tasks[1]);
            ManagementQueue.addMetric(Arrays.asList(mgmtIp, "pending"), tasks[2]);

            return samples;
        }

        private CounterMetricFamily createCounterMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new CounterMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private GaugeMetricFamily createGaugeMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new GaugeMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

    }

    public static void addMsgNum() {
        msgNum ++;
    }
}
