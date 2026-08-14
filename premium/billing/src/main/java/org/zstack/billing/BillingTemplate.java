package org.zstack.billing;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.billing.table.AccountPriceTableRefVO;
import org.zstack.billing.table.AccountPriceTableRefVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/6/8.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class BillingTemplate {
    private static final CLogger logger = Utils.getLogger(BillingTemplate.class);

    @Autowired
    private DatabaseFacade dbf;

    // input parameters
    private SpendingStruct param;
    private Class usageClass;
    private String usageResourceUuidFiledName;
    private List<String> resourceNamesForPrice;
    private String spendingType;

    public static class ChargeRange {
        Usage usageCO;
        long startTime;
        long endTime;

        @Override
        public String toString() {
            return String.format("from %s (%s) to %s (%s), duration %s seconds",
                    new Date(startTime), startTime, new Date(endTime), endTime, TimeUnit.MILLISECONDS.toSeconds(endTime - startTime));
        }

        public Usage getUsageCO() {
            return usageCO;
        }
    }

    class ChargeRangeProducer {
        List cache = new ArrayList<>();

        ChargeRange push(Usage co) {
            RangeOp op = returnRangeOp(cache, co);
            if (op == RangeOp.ADD_TO_CACHE) {
                cache.add(co);
                return null;
            } else if (op == RangeOp.SKIP) {
                return null;
            } else if (op == RangeOp.CLOSE) {
                // pass
                if (cache.isEmpty()) {
                    return null;
                }
            } else if (op == RangeOp.CLOSE_AND_ADD_TO_CACHE) {
                Usage first = (Usage) cache.get(0);

                ChargeRange range = new ChargeRange();
                range.startTime = first.getDateInLong();
                range.endTime = co.getDateInLong();
                range.usageCO = first;

                cache = new ArrayList();
                cache.add(co);

                return range;
            }

            Usage first = (Usage) cache.get(0);

            ChargeRange range = new ChargeRange();
            range.startTime = first.getDateInLong();
            range.endTime = co.getDateInLong();
            range.usageCO = first;
            cache = new ArrayList<>();

            return range;
        }

        ChargeRange buildLastOneIfNeed(long endTime) {
            if (cache.isEmpty()) {
                return null;
            }

            Usage first = (Usage) cache.get(0);
            if (first.getDateInLong() > endTime) {
                return null;
            }

            ChargeRange range = new ChargeRange();
            range.startTime = first.getDateInLong();
            range.endTime = endTime;
            range.usageCO = first;
            return range;
        }
    }

    public static class SampleBundle {
        public String resourceName;
        public UsageSample sample;
    }

    public enum RangeOp {
        SKIP,
        ADD_TO_CACHE,
        CLOSE,
        CLOSE_AND_ADD_TO_CACHE
    }

    protected abstract Class getUsageClass();
    protected abstract String getUsageResourceUuidFiledName();
    protected abstract List<String> getResourceNamesForPrice();
    protected abstract String getSpendingType();
    protected abstract RangeOp returnRangeOp(List cache, Usage co);
    protected abstract List<SampleBundle> generateSampleBundle(ChargeRange range);
    protected abstract Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices);
    protected abstract List generateSpendingDetails(Map<String, List<UsageSample>> samples);

    // intermediate context
    private long startTimeForCollectingUsageCO;
    private long endTimeForCollectingUsageCO;
    private Map<String, List> usageCOByUuid = new LinkedHashMap<>();
    private List<PriceVO> prices;


    public BillingTemplate(SpendingStruct param) {
        this.param = param;
    }

    private StringBuilder loggerBuilder;

    private void init() {
        if (logger.isTraceEnabled()) {
            loggerBuilder = new StringBuilder(String.format("\n\nBILLING DETAILS FOR THE ACCOUNT(%s)", param.getAccountUuid()));
        }

        usageClass = getUsageClass();
        if (usageClass == null) {
            throw new CloudRuntimeException("getUsageClass() cannot return null");
        }

        usageResourceUuidFiledName = getUsageResourceUuidFiledName();

        if (logger.isTraceEnabled()) {
            loggerBuilder.append(String.format("\nUSAGE CLASS: %s", usageClass));
        }

        resourceNamesForPrice = getResourceNamesForPrice();
        if (resourceNamesForPrice == null) {
            throw new CloudRuntimeException("getResourceNamesForPrice() cannot return null");
        }

        if (logger.isTraceEnabled()) {
            loggerBuilder.append(String.format("\nRESOURCE NAMES: %s", resourceNamesForPrice));
        }

        spendingType = getSpendingType();
        if (spendingType == null) {
            throw new CloudRuntimeException("getSpendingType() cannot return null");
        }

        if (logger.isTraceEnabled()) {
            loggerBuilder.append(String.format("\nSPENDING TYPE: %s", spendingType));
        }
    }

    class BillingBuilder {
        private List<ChargeRange> chargeRanges;
        private Map<String, List<UsageSample>> samples = new LinkedHashMap<>();

        private List usageCOs;

        public BillingBuilder(List usageCOs) {
            this.usageCOs = usageCOs;
        }

        private void buildChargeRanges() {
            chargeRanges = new ArrayList<>();
            ChargeRangeProducer producer = new ChargeRangeProducer();
            for (Object obj : usageCOs) {
                ChargeRange range = producer.push((Usage) obj);
                if (range != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[Billing] found a charge range from %s", range));
                    }

                    chargeRanges.add(range);
                }
            }

            ChargeRange range = producer.buildLastOneIfNeed(param.getDateEnd());
            if (range != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Billing] found a charge range from %s", range));
                }

                chargeRanges.add(range);
            }

            // adjusting the charge range to match what users want to calculate
            if (!chargeRanges.isEmpty()) {
                ChargeRange first = chargeRanges.get(0);
                first.startTime = Math.max(first.startTime, param.getDateStart());
                ChargeRange last = chargeRanges.get(chargeRanges.size()-1);
                last.endTime = Math.min(last.endTime, param.getDateEnd());

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[billing] the account[%s] is charged from %s (%s) to %s (%s), duration: %s seconds",
                            param.getAccountUuid(), new Date(first.startTime), first.startTime, new Date(last.endTime), last.endTime,
                            TimeUnit.MILLISECONDS.toSeconds(last.endTime - first.startTime)));
                }
            }

            if (logger.isTraceEnabled()) {
                loggerBuilder.append(String.format("\nCHARGE RANGE: %s", chargeRanges.size()));

                printRow(20, "START", "END", "DURATION(S)");
                printRow(20, "------------", "-------------", "-------------");
                for (ChargeRange cr : chargeRanges) {
                    printRow(20, String.format("%s (%s)", cr.startTime, new Date(cr.startTime)),
                            String.format("%s (%s)", cr.endTime, new Date(cr.endTime)), TimeUnit.MILLISECONDS.toSeconds(cr.endTime - cr.startTime));
                }
                loggerBuilder.append("\n");
            }
        }

        private void collectSamples() {
            int sampleNum = 0;
            for (ChargeRange r : chargeRanges) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Billing] collect samples for the charge range {%s}", r));
                }

                List<SampleBundle> bundles = generateSampleBundle(r);
                for (SampleBundle bundle : bundles) {
                    List<UsageSample> ss = samples.computeIfAbsent(bundle.resourceName, k -> new ArrayList<>());

                    bundle.sample.setStartTime(r.startTime);
                    bundle.sample.setEndTime(r.endTime);

                    ss.add(bundle.sample);
                    sampleNum ++;
                }
            }

            if (logger.isTraceEnabled()) {
                loggerBuilder.append(String.format("\nSAMPLE BEFORE PROCESSING: %s", sampleNum));
                printRow(20, "RESOURCE NAME", "START", "END", "DURATION(S)", "USAGE");
                printRow(20, "--------------------", "--------------------", "--------------------",
                        "--------------------", "--------------------");
                for (Map.Entry<String, List<UsageSample>> e : samples.entrySet()) {
                    for (UsageSample s : e.getValue()) {
                        printRow(20, e.getKey(),
                                String.format("%s(%s)", new Date(s.startTime), s.startTime),
                                String.format("%s(%s)", new Date(s.endTime), s.endTime),
                                String.format("%s", TimeUnit.MILLISECONDS.toSeconds(s.endTime - s.startTime)),
                                String.format("%f", s.usage)
                        );
                    }
                }
                loggerBuilder.append("\n");
            }
        }

        List<SpendingDetails> build() {
            if (logger.isTraceEnabled()) {
                loggerBuilder.append(String.format("\nUSAGE DATA NUMBER: %s", usageCOs.size()));
            }

            buildChargeRanges();
            collectSamples();
            if (samples.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    loggerBuilder.append("\nNO SAMPLES FOR SAMPLING");
                    logger.trace(String.format("\n%s", loggerBuilder.toString()));
                }

                return new ArrayList<>();
            }

            samples = evaluateSampleSpending(samples, prices);

            if (logger.isTraceEnabled()) {
                int sampleNum = 0;
                for (Map.Entry<String, List<UsageSample>> e : samples.entrySet()) {
                    sampleNum += e.getValue().size();
                }

                loggerBuilder.append(String.format("\nSAMPLE AFTER PROCESSING: %s", sampleNum));
                printRow(20, "RESOURCE NAME", "START", "END", "DURATION(S)", "USAGE");
                printRow(20, "--------------------", "--------------------", "--------------------",
                        "--------------------", "--------------------");
                for (Map.Entry<String, List<UsageSample>> e : samples.entrySet()) {
                    for (UsageSample s : e.getValue()) {
                        printRow(20, e.getKey(),
                                String.format("%s(%s)", new Date(s.startTime), s.startTime),
                                String.format("%s(%s)", new Date(s.endTime), s.endTime),
                                String.format("%s", TimeUnit.MILLISECONDS.toSeconds(s.endTime - s.startTime)),
                                String.format("%f", s.usage)
                        );
                    }
                }
                loggerBuilder.append("\n\n");

                logger.trace(String.format("\n%s", loggerBuilder.toString()));
            }

            return generateSpendingDetails(samples);
        }
    }

    public Spending generate() {
        init();

        locateTimeRangeCollectingUsageData();

        if (!collectUsageCO()) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Billing] no usage data for %s found in the time range %s (%s) to %s (%s), duration: %s seconds",
                        usageClass,
                        new Date(startTimeForCollectingUsageCO), startTimeForCollectingUsageCO,
                        new Date(endTimeForCollectingUsageCO), endTimeForCollectingUsageCO,
                        TimeUnit.MILLISECONDS.toSeconds(endTimeForCollectingUsageCO - startTimeForCollectingUsageCO)));
            }
            return null;
        }


        if (!collectPrice()) {
            return null;
        }

        if (logger.isTraceEnabled()) {
            loggerBuilder.append(String.format("\nPRICES: %s", prices.size()));
            for (PriceVO co : prices) {
                loggerBuilder.append(String.format("\n%s", JSONObjectUtil.toJsonString(PriceInventory.valueOf(co))));
            }
            printRow(20, "RESOURCE NAME", "UNIT", "UNIT PRICE");
            printRow(20, "-------------", "-------------", "-------------");
            for (PriceVO vo : prices) {
                printRow(20, vo.getResourceName(), vo.getResourceUnit() == null ? "null" : vo.getResourceUnit(),
                        vo.getPrice());
            }
            loggerBuilder.append("\n");
        }


        return generateSpending();
    }

    private Spending generateSpending() {
        Spending spending = new Spending();
        List<SpendingDetails> details = new ArrayList<>();

        for (Map.Entry<String, List> e : usageCOByUuid.entrySet()) {
            String usageId = e.getKey();
            if (logger.isTraceEnabled()) {
                loggerBuilder.append(String.format("\nBUILD BILLING FOR THE RESOURCE[%s]", usageId));
            }

            BillingBuilder builder = new BillingBuilder(e.getValue());
            details.addAll(builder.build());
        }

        spending.setDetails(details);
        for (SpendingDetails detail: details) {
            spending.addHypervisorTypeSpending(detail);
        }
        spending.setSpendingType(spendingType);
        spending.setDateStart(param.getDateStart());
        spending.setDateEnd(param.getDateEnd());
        return spending;
    }

    private void printRow(int rowWidth, Object...args) {
        List<String> fmts = new ArrayList<>();
        for (Object arg : args) {
            fmts.add(String.format("%%-%ds", rowWidth));
        }

        String fmt = StringUtils.join(fmts, " ");
        fmt = "\n" + fmt;
        loggerBuilder.append(String.format(fmt, args));
    }

    private boolean collectPrice() {
        String priceTableUuid = Q.New(AccountPriceTableRefVO.class)
                .eq(AccountPriceTableRefVO_.accountUuid, param.getAccountUuid())
                .select(AccountPriceTableRefVO_.tableUuid)
                .findValue();
        if (priceTableUuid == null) {
            priceTableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;
        }

        prices = Q.New(PriceVO.class)
                .in(PriceVO_.resourceName, resourceNamesForPrice)
                .eq(PriceVO_.tableUuid, priceTableUuid)
                .list();
        return !prices.isEmpty();
    }

    @Transactional(readOnly = true)
    private boolean collectUsageCO() {
        Query q;

        if (!param.getResourceUuids().isEmpty()) {
            String sql = String.format("select vo from %s vo" +
                    " where vo.accountUuid = :acntUuid" +
                    " and vo.dateInLong >= :start" +
                    " and vo.dateInLong <= :end" +
                    " and vo.%s in (:resourceUuids) order by dateInLong asc", usageClass.getSimpleName(), usageResourceUuidFiledName);
            q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("acntUuid", param.getAccountUuid());
            q.setParameter("start", startTimeForCollectingUsageCO);
            q.setParameter("end", endTimeForCollectingUsageCO);
            q.setParameter("resourceUuids", param.getResourceUuids());
            if (param.getUsageLimit() != null) {
                q.setMaxResults(param.getUsageLimit());
            }
        } else {
            assert param.getUsageLimit() == null;

            // TODO ZSTAC-26120
            String sql = String.format("select vo from %s vo" +
                    " where vo.accountUuid = :acntUuid" +
                    " and vo.dateInLong >= :start" +
                    " and vo.dateInLong <= :end", usageClass.getSimpleName());

            String countSql = sql.replace("select vo from", "select count(vo.id) from");
            TypedQuery<Long> countQuery = dbf.getEntityManager().createQuery(countSql, Long.class)
                    .setParameter("acntUuid", param.getAccountUuid())
                    .setParameter("start", startTimeForCollectingUsageCO)
                    .setParameter("end", endTimeForCollectingUsageCO);
            long count = countQuery.getSingleResult();
            long max = BillingGlobalConfig.MAX_QUERY_USAGE_NUMBER.value(Long.class);
            if (count > max) {
                throw new CloudRuntimeException(String.format("Query %s exceeds limit[%s]", usageClass.getSimpleName(), max));
            }

            sql = String.format("%s order by dateInLong asc", sql);
            q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("acntUuid", param.getAccountUuid());
            q.setParameter("start", startTimeForCollectingUsageCO);
            q.setParameter("end", endTimeForCollectingUsageCO);
        }

        List vos = q.getResultList();

        for (Object obj : vos) {
            Usage u = (Usage) obj;
            List lst = usageCOByUuid.get(u.getUsageId());
            if (lst == null) {
                lst = new ArrayList();
                usageCOByUuid.put(u.getUsageId(), lst);
            }
            lst.add(u);
        }

        return !usageCOByUuid.isEmpty();
    }

    @Transactional(readOnly = true)
    private void locateTimeRangeCollectingUsageData() {
        // find the point before us
        String sql = String.format("select vo.dateInLong from %s vo where vo.accountUuid = :acntUuid" +
                " and vo.dateInLong <= :start order by dateInLong desc", usageClass.getSimpleName());
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("acntUuid", param.getAccountUuid());
        q.setParameter("start", param.getDateStart());
        q.setMaxResults(1);
        List<Long> ret = q.getResultList();

        startTimeForCollectingUsageCO = ret.isEmpty() ? param.getDateStart() : ret.get(0);
        endTimeForCollectingUsageCO = param.getDateEnd();

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[Billing] collecting usage data from %s (%s) to %s (%s), duration: %s seconds",
                    new Date(startTimeForCollectingUsageCO), startTimeForCollectingUsageCO,
                    new Date(endTimeForCollectingUsageCO), endTimeForCollectingUsageCO,
                    TimeUnit.MILLISECONDS.toSeconds(endTimeForCollectingUsageCO - startTimeForCollectingUsageCO)));
        }

        if (logger.isTraceEnabled()) {
            loggerBuilder.append(String.format("\nTIME RANGE FOR COLLECTING USAGE DATA: %s (%s) ~ %s (%s), DURATION: %s seconds",
                    new Date(startTimeForCollectingUsageCO), startTimeForCollectingUsageCO,
                    new Date(endTimeForCollectingUsageCO), endTimeForCollectingUsageCO,
                    TimeUnit.MILLISECONDS.toSeconds(endTimeForCollectingUsageCO - startTimeForCollectingUsageCO)));
        }
    }
}
