package org.zstack.billing;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.billing.generator.BillingType;
import org.zstack.billing.generator.SubBillingTypeConfig;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * @author: kefeng.wang
 * @date: 2019-01-04
 **/
public class ResourceSpendingHelper {
    private static final CLogger logger = Utils.getLogger(ResourceSpendingHelper.class);
    private static Map<String, List<BillingType>> billingTypeMap = Maps.newHashMap();

    static {
        Platform.getReflections().getTypesAnnotatedWith(SubBillingTypeConfig.class)
                .forEach(c -> {
                            SubBillingTypeConfig conf = c.getAnnotation(SubBillingTypeConfig.class);
                            billingTypeMap.put(conf.type(), Arrays.asList(conf.subTypes()));
                        }
                );
    }

    private static List<BillingType> getResourceTypeList(String resourceType) {
        List<BillingType> resourceTypes = new LinkedList<>();
        if (StringUtils.isEmpty(resourceType) || resourceType.equals(BillingConstants.SPENDING_TYPE_ALL)) {
            return resourceTypes;
        }

        if (billingTypeMap.containsKey(resourceType)) {
            return billingTypeMap.get(resourceType);
        }

        throw new OperationFailureException(operr("unsupported billing resource type [%s]", resourceType));
    }

    private static String getWhereClause(List<BillingType> resourceTypes, String resourceUuid) {
        StringBuilder where = new StringBuilder();

        where.append(" WHERE startTime <= :dateEnd AND endTime >= :dateStart");

        if (!CollectionUtils.isEmpty(resourceTypes)) {
            where.append(" AND billingType IN (:billingType)");
        }

        if (StringUtils.isNotEmpty(resourceUuid)) {
            where.append(" AND resourceUuid = :resourceUuid");
        }

        return where.toString();
    }

    public static Pair<List<ResourceSpending>, Pagination> getResourceSpendings(String resourceType, String resourceUuid,
                                                                                String strDateStart, String strDateEnd,
                                                                                Integer start, Integer limit) {
        SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        // build sql
        StringBuilder sqlCount = new StringBuilder();
        StringBuilder sqlFully = new StringBuilder();

        long dateStart = 0;
        long dateEnd = System.currentTimeMillis();

        List<BillingType> resourceTypeList = ResourceSpendingHelper.getResourceTypeList(resourceType);
        String whereClause = getWhereClause(resourceTypeList, resourceUuid);

        if (resourceUuid == null) {
            sqlCount.append("SELECT DISTINCT resourceUuid, resourceName FROM BillingVO").append(whereClause);
            sqlFully.append("SELECT resourceUuid, resourceName, SUM(spending), MIN(startTime), MAX(endTime)")
                    .append(" FROM BillingVO").append(whereClause)
                    .append(" GROUP BY resourceUuid, resourceName")
                    .append(" ORDER BY SUM(spending) DESC");
        } else {
            sqlCount.append("SELECT COUNT(*) FROM BillingVO").append(whereClause);
            sqlFully.append("SELECT billingType, resourceName, spending, startTime, endTime")
                    .append(" FROM BillingVO").append(whereClause)
                    .append(" ORDER BY endTime DESC, billingType");
        }

        // binding param
        SQL queryCount = SQL.New(sqlCount.toString(), Tuple.class);
        SQL queryFully = SQL.New(sqlFully.toString(), Tuple.class);
        if (!resourceTypeList.isEmpty()) {
            queryCount.param("billingType", resourceTypeList);
            queryFully.param("billingType", resourceTypeList);
        }
        if (StringUtils.isNotEmpty(resourceUuid)) {
            queryCount.param("resourceUuid", resourceUuid);
            queryFully.param("resourceUuid", resourceUuid);
        }

        try {
            if (StringUtils.isNotEmpty(strDateStart)) {
                dateStart = SDF.parse(strDateStart).getTime();
            }
            if (StringUtils.isNotEmpty(strDateEnd)) {
                dateEnd = SDF.parse(strDateEnd).getTime();
            }
        } catch (ParseException e) {
            logger.warn(String.format("INVALID date format: [%s] [%s]", strDateStart, strDateEnd));
        }
        queryCount.param("dateStart", dateStart);
        queryFully.param("dateStart", dateStart);
        queryCount.param("dateEnd", dateEnd);
        queryFully.param("dateEnd", dateEnd);

        if (start != null) {
            queryFully.offset(start);
        }
        if (limit != null) {
            queryFully.limit(limit);
        }

        // pagination
        int total = 0;
        List<Tuple> tupleListCount = queryCount.list();
        if (!CollectionUtils.isEmpty(tupleListCount)) {
            if (resourceUuid == null) {
                total = tupleListCount.size();
            } else {
                Long count = tupleListCount.get(0).get(0, Long.class);
                total = (count == null) ? 0 : count.intValue();
            }
        }
        Pagination pagination = new Pagination();
        pagination.setTotal(total);
        pagination.setStart(start);
        pagination.setLimit(limit);

        // List<ResourceSpending>
        List<Tuple> tupleListFully = queryFully.list();
        List<ResourceSpending> spendingList = new LinkedList<>();
        tupleListFully.forEach((tuple) -> {
            String resourceTypeNew = null;
            String resourceUuidNew = null;
            if (resourceUuid == null) {
                resourceUuidNew = tuple.get(0, String.class);
            } else {
                resourceTypeNew = tuple.get(0, BillingType.class).toString();
            }
            String resourceName = tuple.get(1, String.class);
            Double spending = tuple.get(2, Double.class);
            Long spendingStart = tuple.get(3, Long.class);
            Long spendingEnd = tuple.get(4, Long.class);
            spendingList.add(new ResourceSpending(resourceTypeNew, resourceUuidNew, resourceName,
                    spending, spendingStart, spendingEnd));
        });

        return new Pair(spendingList, pagination);
    }
}
