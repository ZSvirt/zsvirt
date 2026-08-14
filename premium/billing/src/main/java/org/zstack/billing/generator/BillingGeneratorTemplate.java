package org.zstack.billing.generator;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.table.AccountPriceTableRefVO;
import org.zstack.billing.table.AccountPriceTableRefVO_;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.identity.AccountManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2019/4/1.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class BillingGeneratorTemplate {
    private static final CLogger logger = Utils.getLogger(BillingGeneratorTemplate.class);

    private static final int MAX_USAGE_LIMIT_PER_RESOURCE = 5000;

    private Class usageClass;

    private SpendingCalculator spendingCalculator;

    private String usageResourceUuidFiledName;

    protected String accountUuid;

    private List<String> resourceNamesForPrice;

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected AccountManager acntMgr;

    @Autowired
    private PluginRegistry pluginRgty;

    protected abstract BillingType getBillingType();
    protected abstract <T extends BillingVO> List<T> make(SpendingDetails spendingDetails);
    protected abstract UsageHistory make(Usage usageVO);
    protected abstract long getId(Usage usageVO);
    protected abstract List<Usage> makeNewUsageVOS(List<String> resourceUuids);
    protected abstract SpendingCalculator getSpendingCalculator();
    protected abstract List<String> getResourceNamesForPrice();

    private void init() {
        this.spendingCalculator = this.getSpendingCalculator();
        BillingGeneratorConfig billingGeneratorConfig = this.getClass().getAnnotation(BillingGeneratorConfig.class);
        this.usageClass = billingGeneratorConfig.usageClass();
        this.usageResourceUuidFiledName = billingGeneratorConfig.usageResourceUuidFiledName();
        this.resourceNamesForPrice = getResourceNamesForPrice();
    }

    private List<Tuple> getAllResourceUuids(String accountUuid) {
        String sql = String.format("select distinct vo.%s, count(vo.%s) from %s vo where vo.accountUuid = :accountUuid group by vo.%s",
                this.usageResourceUuidFiledName,
                this.usageResourceUuidFiledName,
                usageClass.getSimpleName(),
                this.usageResourceUuidFiledName);
        List<Tuple> resourceUuids = SQL.New(sql, Tuple.class)
                .param("accountUuid", accountUuid)
                .list();
        return resourceUuids;
    }

    private List<BillingVO> getBillingList(Spending spending) {
        List<BillingVO> billingVOS = new ArrayList<>();

        if (spending == null) {
            return billingVOS;
        }

        List<SpendingDetails> spendingDetails = spending.getDetails();
        if (spendingDetails == null || spendingDetails.isEmpty()) {
            return billingVOS;
        }

        for (SpendingDetails s : spendingDetails) {
            List<BillingVO> vos = make(s);
            billingVOS.addAll(vos);
        }

        for (BillingVO billingVO : billingVOS) {
            if (System.currentTimeMillis() - billingVO.getEndTime() > TimeUnit.HOURS.toMillis(48)) {
                billingVO.setCreateDate(new Timestamp(billingVO.getEndTime()));
            }
        }

        return billingVOS;
    }

    public final void generate(String accountUuid) {
        logger.info(String.format("%s generate account[%s] billing", this.getClass(), accountUuid));

        this.init();
        this.accountUuid = accountUuid;

        List<Tuple> resourceUuidTuples = getAllResourceUuids(accountUuid);

        int slice = BillingGlobalConfig.BILLING_GENERATION_RESOURCE_SLICE_SIZE.value(Integer.class);
        List<List<Tuple>> resourceUuidTupleParts = Lists.partition(resourceUuidTuples, slice);
        resourceUuidTupleParts.stream().forEach(tuplePart -> {
            List<String> resourceUuids = new ArrayList<>();

            for (Tuple tuple : tuplePart) {
                String resourceUuid = tuple.get(0, String.class);
                long usageVONum = tuple.get(1, Long.class);

                if (usageVONum > MAX_USAGE_LIMIT_PER_RESOURCE) {
                    this.generateBillsInBatches(resourceUuid);
                } else {
                    resourceUuids.add(resourceUuid);
                }
            }

            if (!resourceUuids.isEmpty()) {
                this.generate(resourceUuids);
            }
        });
    }

    private boolean skipGenerateBilling() {
        String priceTableUuid = Q.New(AccountPriceTableRefVO.class)
                .eq(AccountPriceTableRefVO_.accountUuid, accountUuid)
                .select(AccountPriceTableRefVO_.tableUuid)
                .findValue();
        if (priceTableUuid == null) {
            priceTableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;
        }

        return !Q.New(PriceVO.class)
                .in(PriceVO_.resourceName, resourceNamesForPrice)
                .eq(PriceVO_.tableUuid, priceTableUuid)
                .isExists();
    }

    private void generate(List<String> resourceUuids) {
        SpendingStruct struct = new SpendingStruct();
        struct.setDateStart(0L);
        struct.setDateEnd(System.currentTimeMillis());
        struct.setAccountUuid(this.accountUuid);
        struct.setResourceUuids(resourceUuids);

        Spending spending = null;
        try {
            spending = this.spendingCalculator.calculate(struct);
        } catch (CloudRuntimeException exception) {
            logger.error(String.format("generate account[%s] resources%s billing failed, calculate spending error",
                    struct.getAccountUuid(), resourceUuids),
                    exception);
            return;
        }

        List<BillingVO> billingVOS = getBillingList(spending);

        String sql = String.format("select vo from %s vo where vo.accountUuid = :accountUuid and vo.%s in (:resourceUuids)",
                usageClass.getSimpleName(),
                this.usageResourceUuidFiledName);
        List<Usage> usageVOS = SQL.New(sql, usageClass)
                .param("accountUuid", accountUuid)
                .param("resourceUuids", resourceUuids)
                .list();

        List<Long> usageIds = new ArrayList<>();
        usageVOS.forEach(usageVO -> usageIds.add(getId(usageVO)));

        List<UsageHistory> usageHistoryVOS = new ArrayList<>();
        for (Usage usage : usageVOS) {
            UsageHistory history = make(usage);
            usageHistoryVOS.add(history);
        }

        List<Usage> newUsageVOS = makeNewUsageVOS(resourceUuids);

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (BillingVO billingVO : billingVOS) {
                    persist(billingVO);
                }

                for (UsageHistory usageHistory : usageHistoryVOS) {
                    persist(usageHistory);
                }

                List usageIdParts = Lists.partition(usageIds, 5000);
                usageIdParts.stream().forEach(ids -> {
                    String testSql = String.format("delete from %s where id in (:ids)", usageClass.getSimpleName());
                    Query q = dbf.getEntityManager().createNativeQuery(testSql);
                    q.setParameter("ids", ids);
                    q.executeUpdate();
                });

                for (Usage usage : newUsageVOS) {
                    persist(usage);
                }
            }
        }.execute();
    }

    private void generateBillsInBatches(String resourceUuid) {
        boolean incomplete = true;
        while (incomplete) {
            incomplete = !generate(resourceUuid, MAX_USAGE_LIMIT_PER_RESOURCE);
        }
    }

    /**
     * @param resourceUuid
     * @param usageLimit
     * @return Whether to complete the resource bill
     */
    private boolean generate(String resourceUuid, int usageLimit) {
        assert usageLimit > 1000;

        String sql = String.format("select vo from %s vo where vo.accountUuid = :accountUuid and vo.%s = :resourceUuid",
                usageClass.getSimpleName(),
                this.usageResourceUuidFiledName);
        List<Usage> usageVOS = SQL.New(sql, usageClass)
                .param("accountUuid", accountUuid)
                .param("resourceUuid", resourceUuid)
                .limit(usageLimit + 1)
                .list();

        if (usageVOS.isEmpty()) {
            return true;
        }

        boolean completed = usageVOS.size() < usageLimit;
        if (!completed) {
            usageVOS.remove(usageVOS.size() - 1);
        }

        SpendingStruct struct = new SpendingStruct();
        struct.setDateStart(0L);
        if (completed) {
            struct.setDateEnd(System.currentTimeMillis());
        } else {
            struct.setDateEnd(usageVOS.get(usageVOS.size() - 1).getDateInLong());
        }
        struct.setAccountUuid(this.accountUuid);
        struct.setResourceUuids(Collections.singletonList(resourceUuid));
        struct.setUsageLimit(usageLimit);

        Spending spending = null;
        try {
            spending = this.spendingCalculator.calculate(struct);
        } catch (CloudRuntimeException exception) {
            logger.error(String.format("generate account[%s] resource[%s] billing failed, calculate spending error",
                    struct.getAccountUuid(), resourceUuid),
                    exception);
            return true;
        }

        if (!completed) {
            usageVOS.remove(usageVOS.size() - 1);
        }

        List<BillingVO> billingVOS = getBillingList(spending);

        List<Long> usageIds = new ArrayList<>();
        usageVOS.forEach(usageVO -> usageIds.add(getId(usageVO)));

        List<UsageHistory> usageHistoryVOS = new ArrayList<>();
        for (Usage usage : usageVOS) {
            UsageHistory history = make(usage);
            usageHistoryVOS.add(history);
        }

        Usage newUsageVO = null;
        if (completed) {
            List<Usage> newUsageVOS = makeNewUsageVOS(Collections.singletonList(resourceUuid));
            DebugUtils.Assert(newUsageVOS.size() <= 1, "newUsageVOS size error");
            newUsageVO = newUsageVOS.size() > 0 ? newUsageVOS.get(0) : null;
        }

        Usage finalNewUsageVO = newUsageVO;
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (BillingVO billingVO : billingVOS) {
                    persist(billingVO);
                }

                for (UsageHistory usageHistory : usageHistoryVOS) {
                    persist(usageHistory);
                }

                String testSql = String.format("delete from %s where id in (:ids)", usageClass.getSimpleName());
                Query q = dbf.getEntityManager().createNativeQuery(testSql);
                q.setParameter("ids", usageIds);
                q.executeUpdate();

                if (finalNewUsageVO != null) {
                    persist(finalNewUsageVO);
                }
            }
        }.execute();

        return completed;
    }
}
