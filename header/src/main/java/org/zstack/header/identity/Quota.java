package org.zstack.header.identity;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;

import java.util.*;

/**
 * Created by frank on 7/13/2015.
 */
public class Quota {
    public final static long DEFAULT_NO_LIMITATION = -1;

    @Deprecated
    public interface QuotaOperator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs);

        List<QuotaUsage> getQuotaUsageByAccount(String accountUuid);
    }

    @Deprecated
    public interface QuotaValidator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        Set<String> reportQuotaName();

        List<Class<? extends Message>> getMessagesNeedValidation();
    }

    public static class QuotaUsage {
        private String name;
        private Long total;
        private Long used;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getUsed() {
            return used;
        }

        public void setUsed(Long used) {
            this.used = used;
        }
    }

    /**
     * quota pair describes quota name with its value
     *
     * for every account, a list of quota pair will be stored
     * and used during account's quota check
     */
    public static class QuotaPair {
        private String name;
        private long value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    /**
     * Deprecated but keep it for compatible
     *
     * use QuotaDefinition instead
     */
    private List<QuotaPair> quotaPairs;

    /**
     * Deprecated but keep it for compatible
     *
     * use addQuotaMessageChecker instead
     */
    private List<Class<? extends Message>> messagesNeedValidation = new ArrayList<>();

    /**
     * Deprecated but keep it for compatible
     *
     * use QuotaDefinition instead
     */
    private QuotaOperator operator;

    /**
     * Deprecated but keep it for compatible
     *
     * use addQuotaMessageChecker instead
     */
    private Set<QuotaValidator> quotaValidators;

    private List<QuotaDefinition> quotaDefinitions;
    private List<QuotaMessageHandler<? extends Message>> quotaMessageHandlerList = new ArrayList<>();

    @Deprecated
    public void addPair(QuotaPair p) {
        if (quotaPairs == null) {
            quotaPairs = new ArrayList<>();
        }
        quotaPairs.add(p);
    }

    public void defineQuota(QuotaDefinition d) {
        if (quotaDefinitions == null) {
            quotaDefinitions = new ArrayList<>();
        }
        quotaDefinitions.add(d);
    }

    @Deprecated
    public List<QuotaPair> getQuotaPairs() {
        return quotaPairs;
    }

    public List<QuotaDefinition> getQuotaDefinitions() {
        return quotaDefinitions;
    }

    public void setQuotaDefinitions(List<QuotaDefinition> quotaDefinitions) {
        this.quotaDefinitions = quotaDefinitions;
    }

    @Deprecated
    public void setQuotaPairs(List<QuotaPair> quotaPairs) {
        this.quotaPairs = quotaPairs;
    }

    @Deprecated
    public void addMessageNeedValidation(Class<? extends Message> msgClass) {
        messagesNeedValidation.add(msgClass);
    }

    public List<Class<? extends Message>> getMessagesNeedValidation() {
        return messagesNeedValidation;
    }

    public List<QuotaMessageHandler<? extends Message>> getQuotaMessageCheckerList() {
        return quotaMessageHandlerList;
    }

    public void addQuotaMessageChecker(QuotaMessageHandler<? extends Message> messageChecker) {
        this.quotaMessageHandlerList.add(messageChecker);
    }

    public void setQuotaMessageCheckerList(List<QuotaMessageHandler<? extends Message>> quotaMessageHandlerList) {
        this.quotaMessageHandlerList = quotaMessageHandlerList;
    }

    @Deprecated
    public QuotaOperator getOperator() {
        return operator;
    }

    @Deprecated
    public void setOperator(QuotaOperator operator) {
        this.operator = operator;
    }

    public Set<QuotaValidator> getQuotaValidators() {
        return this.quotaValidators;
    }

    public void addQuotaValidators(Set<QuotaValidator> quotaValidators) {
        if (this.quotaValidators == null) {
            this.quotaValidators = new HashSet<>();
        }
        for (QuotaValidator q : quotaValidators) {
            this.quotaValidators.add(q);
        }
    }
}
