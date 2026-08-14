package org.zstack.header.allocator;

import org.zstack.header.core.I18nMessage;
import org.zstack.header.host.HostVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HostCandidate implements Serializable {
    public final HostVO host;

    public List<String> recommendBy;
    public List<String> notRecommendBy;
    public String reject;
    public String rejectBy;
    public String rejectI18n;

    public HostCandidate(HostVO host) {
        this.host = Objects.requireNonNull(host);
    }

    public String getUuid() {
        return host.getUuid();
    }

    public void markAsRecommended(String flowName) {
        recommendBy = recommendBy == null ? new ArrayList<>() : recommendBy;
        recommendBy.add(flowName);
    }

    public void markAsNotRecommended(String flowName) {
        notRecommendBy = notRecommendBy == null ? new ArrayList<>() : notRecommendBy;
        notRecommendBy.add(flowName);
    }

    public void markAsRejected(Class<?> flowClazz, I18nMessage reason) {
        markAsRejected(flowClazz.getSimpleName(), reason);
    }

    public void markAsRejected(String flowName, String reason) {
        rejectBy = flowName;
        reject = reason;
    }

    public void markAsRejected(String flowName, I18nMessage reason) {
        rejectBy = flowName;
        reject = reason.getDetails();
        rejectI18n = reason.getI18nDetails();
    }

    @Override
    public String toString() {
        return host.getUuid();
    }

    public RejectedCandidate toRejectedCandidate() {
        return new RejectedCandidate(host.getUuid(), host.getName(), reject, rejectI18n, rejectBy);
    }

    public static class RejectedCandidate implements I18nMessage {
        public final String hostUuid;
        public final String hostName;
        public final String reject;
        public String rejectI18n;
        public final String rejectBy;

        private RejectedCandidate(String hostUuid, String hostName, String reject, String rejectI18n, String rejectBy) {
            this.hostUuid = hostUuid;
            this.hostName = hostName;
            this.reject = reject;
            this.rejectI18n = rejectI18n;
            this.rejectBy = rejectBy;
        }

        @Override
        public String getDetails() {
            return reject;
        }

        @Override
        public String getI18nDetails() {
            return rejectI18n;
        }
    }
}
