package org.zstack.billing;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.table.*;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.billing.BillingSystemTags.PRICE_GPU_OFFERING_UUID;
import static org.zstack.billing.BillingSystemTags.PRICE_GPU_OFFERING_UUID_TOKEN;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.inerr;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/3/8.
 */
@InterceptorForService("billing")
public class BillingApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(BillingApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateResourcePriceMsg || msg instanceof APICreatePriceTableMsg
                || msg instanceof APIUpdatePriceTableMsg || msg instanceof APIAttachPriceTableToAccountMsg
                || msg instanceof APIUpdateResourcePriceMsg) {
            checkBillingEnabled();
        }

        if (msg instanceof APICreateResourcePriceMsg) {
            validate((APICreateResourcePriceMsg) msg);
        } else if (msg instanceof APICalculateAccountSpendingMsg) {
            validate((APICalculateAccountSpendingMsg) msg);
        } else if (msg instanceof APICalculateResourceSpendingMsg) {
            validate((APICalculateResourceSpendingMsg) msg);
        } else if (msg instanceof APICalculateAccountBillingSpendingMsg) {
            validate((APICalculateAccountBillingSpendingMsg ) msg);
        } else if (msg instanceof APICreatePriceTableMsg) {
            validate((APICreatePriceTableMsg ) msg);
        } else if (msg instanceof APIUpdatePriceTableMsg) {
            validate((APIUpdatePriceTableMsg ) msg);
        } else if (msg instanceof APIAttachPriceTableToAccountMsg) {
            validate((APIAttachPriceTableToAccountMsg ) msg);
        } else if (msg instanceof APIDeletePriceTableMsg) {
            validate((APIDeletePriceTableMsg ) msg);
        } else if (msg instanceof APIDeleteResourcePriceMsg) {
            validate((APIDeleteResourcePriceMsg) msg);
        } else if (msg instanceof APIGetAccountPriceTableRefMsg) {
            validate((APIGetAccountPriceTableRefMsg) msg);
        } else if (msg instanceof APIUpdateResourcePriceMsg) {
             validate((APIUpdateResourcePriceMsg) msg);
        } else if (msg instanceof APICleanupBillingUsageMsg) {
            validate((APICleanupBillingUsageMsg) msg);
        }

        return msg;
    }

    private void checkBillingEnabled() {
        if (!BillingGlobalConfig.BILLING_ENABLE.value(Boolean.class)) {
            throw new ApiMessageInterceptionException(argerr(
                    "billing is disabled"
            ));
        }
    }

    private void validate(APICalculateAccountSpendingMsg msg) {
        if (msg.getDateEnd() != null && msg.getDateStart() != null && msg.getDateStart() > msg.getDateEnd()) {
            throw new ApiMessageInterceptionException(argerr(
                    "the start date must be greater than the end date"
            ));
        }

        if (msg.getDateStart() == null) {
            msg.setDateStart(0L);
        }

        if (msg.getDateEnd() == null) {
            msg.setDateEnd(System.currentTimeMillis());
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[Billing] calculate billing for the account[%s] from %s (%s) to %s (%s), duration %s seconds",
                    msg.getAccountUuid(), new Date(msg.getDateStart()), msg.getDateStart(), new Date(msg.getDateEnd()),
                    msg.getDateEnd(), TimeUnit.MILLISECONDS.toSeconds(msg.getDateEnd() - msg.getDateStart())));
        }
    }

    private void validate(APICalculateAccountBillingSpendingMsg msg) {
        if (msg.getDateEnd() != null && msg.getDateStart() != null && msg.getDateStart() > msg.getDateEnd()) {
            throw new ApiMessageInterceptionException(argerr(
                    "the start date must be greater than the end date"
            ));
        }

        if (msg.getDateStart() == null) {
            msg.setDateStart(0L);
        }

        if (msg.getDateEnd() == null) {
            msg.setDateEnd(System.currentTimeMillis());
        }
    }

    private void validate(APICalculateResourceSpendingMsg msg) {
        if (msg.getDateEnd() != null && msg.getDateStart() != null
                && msg.getDateStart().compareTo(msg.getDateEnd()) > 0) {
            throw new ApiMessageInterceptionException(argerr("the start date must be greater than the end date"));
        }

        if (msg.getResourceType() == null && msg.getResourceUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("resourceType and resourceUuid cannot be empty at the same time"));
        }

        if (msg.getLimit() == null) {
            msg.setLimit(100);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[Billing] calculate billing for the resource[%s] from %s to %s",
                    msg.getResourceType(), msg.getDateStart(), msg.getDateEnd()));
        }
    }

    private String toTimeUnit(String s) {
        if ("ms".equalsIgnoreCase(s)) {
            return TimeUnit.MILLISECONDS.toString();
        } else if ("s".equalsIgnoreCase(s)) {
            return TimeUnit.SECONDS.toString();
        } else if ("m".equalsIgnoreCase(s)) {
            return TimeUnit.MINUTES.toString();
        } else if ("h".equalsIgnoreCase(s)) {
            return TimeUnit.HOURS.toString();
        } else if ("d".equalsIgnoreCase(s)) {
            return TimeUnit.DAYS.toString();
        } else if ("w".equalsIgnoreCase(s)) {
            return TimeUtils.TIME_UNIT_WEEKS;
        }else if ("mon".equalsIgnoreCase(s)) {
            return TimeUtils.TIME_UNIT_MONTHS;
        }else {
            throw new ApiMessageInterceptionException(inerr("unknown time unit[%s]", s));
        }
    }

    private void validate(APICreateResourcePriceMsg msg) {
        msg.setResourceUnit(makeResourceUnit(msg.getResourceName(), msg.getResourceUnit()));

        if (SizeUnit.BYTE.toString().equalsIgnoreCase(msg.getResourceUnit())) {
            throw new ApiMessageInterceptionException(argerr(
                    "the minimal resource unit is megabyte, cannot be byte"
            ));
        }

        msg.setTimeUnit(toTimeUnit(msg.getTimeUnit()));

        if (msg.getPrice() < 0 || msg.getPrice() > 999999999.99999d) {
            throw new ApiMessageInterceptionException(argerr(
                    "price must be 0 and 999999999.99"
            ));
        }

        if (BillingConstants.SPENDING_PCI_DEVICE.equalsIgnoreCase(msg.getResourceName())) {
            if(msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(
                        "gpu price must be bound to gpu uuid empty"
                ));
            }

            String offeringUuid = SystemTagUtils.findTagValue(msg.getSystemTags(), PRICE_GPU_OFFERING_UUID, PRICE_GPU_OFFERING_UUID_TOKEN);
            if(StringUtils.isEmpty(offeringUuid)){
                throw new ApiMessageInterceptionException(argerr(
                        "gpu price must be bound to gpu uuid %s", msg.getSystemTags()
                ));
            }
        }

        /*
        if (msg.getDateInLong() != null && msg.getDateInLong() < System.currentTimeMillis()) {
            throw new ApiMessageInterceptionException(argerr(
                    "dateInLong must be greater than current time"));
        }
        */

        if (msg.getTableUuid() == null) {
            msg.setTableUuid(BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID);
        }
    }

    private String makeResourceUnit(String resourceName, String resourceUnit) {
        if (BillingConstants.SPENDING_CPU.equalsIgnoreCase(resourceName)) {
            return null;
        } else if (BillingConstants.SPENDING_PCI_DEVICE.equalsIgnoreCase(resourceName)) {
            return null;
        } else if (BillingConstants.SPENDING_MEMORY.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_ROOT_VOLUME.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_TYPE_DATA_VOLUME.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_TYPE_SNAPSHOT.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_VIP_BANDWIDTH_IN.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_VIP_BANDWIDTH_OUT.equalsIgnoreCase(resourceName)) {
           return SizeUnit.fromString(resourceUnit).toString();
        } else if (BillingConstants.SPENDING_BAREMETAL2_INSTANCE.equalsIgnoreCase(resourceName)) {
            return null;
        }

        throw new ApiMessageInterceptionException(argerr(
                "resourceName[%s] is invalid", resourceName));
    }

    private void validate(APICreatePriceTableMsg msg) {
        for (APICreatePriceTableMsg.Price price : msg.getPrices()) {
            validatePrice(price);
        }
    }

    private void validate(APIUpdatePriceTableMsg msg) {

    }

    private void validatePrice(APICreatePriceTableMsg.Price price) {
        price.setResourceUnit(makeResourceUnit(price.getResourceName(), price.getResourceUnit()));

        if (SizeUnit.BYTE.toString().equalsIgnoreCase(price.getResourceUnit())) {
            throw new ApiMessageInterceptionException(argerr(
                    "the minimal resource unit is megabyte, cannot be byte"
            ));
        }

        price.setTimeUnit(toTimeUnit(price.getTimeUnit()));

        if (price.getPrice() < 0 || price.getPrice() > 999999999.99999d) {
            throw new ApiMessageInterceptionException(argerr(
                    "price must be 0 and 999999999.99"
            ));
        }

        if (BillingConstants.SPENDING_PCI_DEVICE.equalsIgnoreCase(price.getResourceName())) {
            if(price.getSystemTags() == null || price.getSystemTags().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(
                        "gpu price must be bound to gpu uuid empty"
                ));
            }

            String offeringUuid = SystemTagUtils.findTagValue(price.getSystemTags(), PRICE_GPU_OFFERING_UUID, PRICE_GPU_OFFERING_UUID_TOKEN);
            if(StringUtils.isEmpty(offeringUuid)){
                throw new ApiMessageInterceptionException(argerr(
                        "gpu price must be bound to gpu uuid %s", price.getSystemTags()
                ));
            }
        }
    }

    private void validate(APIAttachPriceTableToAccountMsg msg) {
        boolean exists = Q.New(AccountPriceTableRefVO.class)
                .eq(AccountPriceTableRefVO_.accountUuid, msg.getAccountUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(argerr("The account[uuid=%s] has attach price table", msg.getAccountUuid()));
        }
    }

    private void validate(APIDeletePriceTableMsg msg) {
        if (BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID.equals(msg.getUuid())) {
            throw new ApiMessageInterceptionException(argerr("This priceTable[uuid=%s] is not allowed to delete", msg.getUuid()));
        }
    }

    private void validate(APIDeleteResourcePriceMsg msg) {
        String tableUuid = Q.New(PriceVO.class)
                .select(PriceVO_.tableUuid)
                .eq(PriceVO_.uuid, msg.getUuid())
                .findValue();
        msg.setTableUuid(tableUuid);
    }

    private void validate(APIGetAccountPriceTableRefMsg msg) {
        if (msg.getAccountUuid() != null && msg.getTableUuid() != null) {
            throw new ApiMessageInterceptionException(argerr("accountUuid/tableUuid only one of them is allowed to be set"));
        }
    }

    private void validate(APIUpdateResourcePriceMsg msg) {
        if (msg.getEndDateInLong() != null && msg.getEndDateInLong() < 0) {
            throw new ApiMessageInterceptionException(argerr("endDateInLong is not allowed to be negative"));
        }

        if (msg.getEndDateInLong() != null && msg.isSetEndDateInLongBaseOnCurrentTime()) {
            throw new ApiMessageInterceptionException(argerr("endDateInLong and setEndDateInLongBaseOnCurrentTime are not allowed to set at the same time"));
        }

        if (msg.isSetEndDateInLongBaseOnCurrentTime()) {
            msg.setEndDateInLong(new Date().getTime());
        }

        if (msg.getEndDateInLong() != null) {
            Long endDateInLong = Q.New(PriceVO.class)
                    .eq(PriceVO_.uuid, msg.getUuid())
                    .select(PriceVO_.endDateInLong)
                    .findValue();
            if (endDateInLong != null) {
                throw new ApiMessageInterceptionException(argerr("endDateInLong is set, no modification allowed"));
            }

            long dateInLong = Q.New(PriceVO.class)
                    .eq(PriceVO_.uuid, msg.getUuid())
                    .select(PriceVO_.dateInLong)
                    .findValue();
            if (msg.getEndDateInLong() <= dateInLong) {
                throw new ApiMessageInterceptionException(argerr("endDateInLong cannot be earlier than dateInLong"));
            }
        }
    }

    private void validate(APICleanupBillingUsageMsg msg) {
        if (BillingGlobalConfig.BILLING_ENABLE.value(Boolean.class)) {
            throw new ApiMessageInterceptionException(argerr(
                    "billing is enable, This operation is only allowed in the disabled state"));
        }
    }
}
