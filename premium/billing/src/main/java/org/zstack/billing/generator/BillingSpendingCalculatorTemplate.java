package org.zstack.billing.generator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.billing.Spending;
import org.zstack.billing.SpendingDetails;
import org.zstack.billing.SpendingStruct;
import org.zstack.core.db.SQL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lining on 2019/4/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class BillingSpendingCalculatorTemplate implements BillingSpendingCalculator {
    private static final CLogger logger = Utils.getLogger(BillingSpendingCalculatorTemplate.class);

    protected SpendingStruct struct;
    protected int limit = 50000;
    private Map<String, SpendingDetails> detailsMap = new HashMap<>();

    protected abstract int getBillingCount();
    protected abstract SpendingDetails make(BillingVO billingVO);
    protected abstract void merge(SpendingDetails source, SpendingDetails target);
    protected abstract String getSpendingType();
    protected abstract List<BillingVO> getBillingVOS(int offset);

    protected int getBillingVOCount(Class billingClass) {
        String sql;
        if (struct.getResourceUuid() == null) {
            sql = String.format("select count(*) from %s vo where vo.accountUuid = :accountUuid and vo.startTime <= %s and vo.endTime >= %s",
                    billingClass.getSimpleName(),
                    this.struct.getDateEnd(),
                    this.struct.getDateStart());
        } else {
            sql = String.format("select count(*) from %s vo where vo.accountUuid = :accountUuid and resourceUuid = '%s' and vo.startTime <= %s and vo.endTime >= %s",
                    billingClass.getSimpleName(), this.struct.getResourceUuid(),
                    this.struct.getDateEnd(),
                    this.struct.getDateStart());
        }


        Long count = SQL.New(sql, Long.class)
                .param("accountUuid", this.struct.getAccountUuid())
                .find();
        return count.intValue();
    }

    protected List<BillingVO> getBillingVOS(Class billingClass, int offset) {
        String sql;
        if (struct.getResourceUuid() == null) {
            sql = String.format("select vo from %s vo where vo.accountUuid = :accountUuid and vo.startTime <= %s and vo.endTime >= %s",
                    billingClass.getSimpleName(),
                    this.struct.getDateEnd(),
                    this.struct.getDateStart());
        } else {
            sql = String.format("select vo from %s vo where vo.accountUuid = :accountUuid and resourceUuid = '%s' and vo.startTime <= %s and vo.endTime >= %s",
                    billingClass.getSimpleName(), this.struct.getResourceUuid(),
                    this.struct.getDateEnd(),
                    this.struct.getDateStart());
        }

        return SQL.New(sql, billingClass)
                .param("accountUuid", this.struct.getAccountUuid()).offset(offset).limit(limit)
                .list();
    }

    private int getAndMergeBillint(int offset) {
        List<BillingVO> billingVOS = getBillingVOS(offset);
        if (billingVOS.isEmpty()) {
            return 0;
        }

        for (BillingVO billingVO : billingVOS) {
            SpendingDetails details = make(billingVO);
            String key = String.format("%s-%s", details.getType(), details.getResourceUuid());

            SpendingDetails spendingDetails = detailsMap.get(key);
            if (spendingDetails == null) {
                detailsMap.put(key, details);
                continue;
            }

            if (!struct.isSimple()) {
                merge(spendingDetails, details);
            }
            spendingDetails.spending += details.spending;
        }
        return billingVOS.size();
    }

    @Override
    public Spending calculate(SpendingStruct param) {
        long start = System.currentTimeMillis();
        logger.debug(String.format("start calculate %s of account %s", this.getSpendingType(), param.getAccountUuid()));
        this.struct = param;

        Spending spending = new Spending();
        spending.setDateEnd(param.getDateEnd());
        spending.setDateStart(param.getDateStart());
        spending.setSpendingType(this.getSpendingType());

        int billingCount = getBillingCount();
        if (billingCount == 0) {
            return null;
        }
        int current = 0;
        while (current < billingCount) {
            logger.debug(String.format("calculate %s of account %s from offset %d / %d", this.getSpendingType(), param.getAccountUuid(),
                    current, billingCount));
            int size = getAndMergeBillint(current);
            if (size == 0) {
                break;
            } else {
                current += size;
            }
        }

        List<SpendingDetails> detailsList = new ArrayList<>(detailsMap.values());
        for (SpendingDetails details : detailsList) {
            spending.addHypervisorTypeSpending(details);
        }

        if (!param.isSimple()) {
            spending.setDetails(detailsList);
        }

        long end = System.currentTimeMillis();
        logger.debug(String.format("end calculate %s", this.getSpendingType()));
        logger.debug(String.format("calculate %s from account %s spend %d ms...", this.getSpendingType(), param.getAccountUuid(), end - start));

        return spending;
    }

    protected double reCalculationSpending(double spending, long startTime, long endTime) {
        long redundantTime = 0;
        if (startTime < struct.getDateStart()) {
            redundantTime = struct.getDateStart() - startTime;
        }

        if (endTime > struct.getDateEnd()) {
            redundantTime += endTime - struct.getDateEnd();
        }

        if (redundantTime == 0) {
            return spending;
        }

        long total = endTime - startTime;
        assert total > redundantTime;
        double ratio = (total - redundantTime) * 1d / total * 1d;
        spending = ratio * spending;
        return spending;
    }

    protected long getSpendingStartTime(long billingStartTime) {
        return billingStartTime < this.struct.getDateStart() ? this.struct.getDateStart() : billingStartTime;
    }

    protected long getSpendingEndTime(long billingEndTime) {
        return billingEndTime > this.struct.getDateEnd() ? this.struct.getDateEnd() : billingEndTime;
    }
}
