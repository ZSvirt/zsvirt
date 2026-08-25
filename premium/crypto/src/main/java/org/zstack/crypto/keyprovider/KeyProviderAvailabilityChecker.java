package org.zstack.crypto.keyprovider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderConstant;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.message.MessageReply;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class KeyProviderAvailabilityChecker {
    private static final String KEY_PROVIDER_UNAVAILABLE = "keyProviderUnavailable";

    @Autowired
    private CloudBus bus;

    public void ensureProviderAvailable(String providerUuid) {
        if (StringUtils.isBlank(providerUuid)) {
            throw new OperationFailureException(operr("default key provider is not configured")
                    .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
        }

        Set<String> providerUuids = new LinkedHashSet<>();
        providerUuids.add(providerUuid);
        ensureProvidersAvailable(providerUuids);
    }

    public void ensureProvidersAvailable(Collection<String> providerUuids) {
        if (CollectionUtils.isEmpty(providerUuids)) {
            return;
        }

        Set<String> normalized = providerUuids.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return;
        }

        Map<String, KeyProviderType> providerTypes = queryProviderTypes(normalized);
        for (String providerUuid : normalized) {
            KeyProviderType type = providerTypes.get(providerUuid);
            if (type == null) {
                throw new OperationFailureException(operr("cannot find key provider[uuid:%s]",
                        providerUuid).withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }

            TriggerKeyProviderHealthCheckMsg msg = new TriggerKeyProviderHealthCheckMsg();
            msg.setProviderUuid(providerUuid);
            msg.setProviderType(type.toString());
            msg.setRealtime(true);
            bus.makeTargetServiceIdByResourceUuid(msg, KeyProviderConstant.SERVICE_ID, providerUuid);
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                throw new OperationFailureException(operr("key provider[uuid:%s] realtime health check failed: %s",
                        providerUuid, reply.getError() == null ? null : reply.getError().getReadableDetails())
                        .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }

            TriggerKeyProviderHealthCheckReply healthCheckReply = reply.castReply();
            if (!healthCheckReply.isAvailable()) {
                throw new OperationFailureException(operr("key provider[uuid:%s] is unavailable: %s",
                        providerUuid, healthCheckReply.getStatusMessage())
                        .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }
        }
    }

    private Map<String, KeyProviderType> queryProviderTypes(Set<String> providerUuids) {
        List<Tuple> tuples = Q.New(KeyProviderVO.class)
                .select(KeyProviderVO_.uuid, KeyProviderVO_.type)
                .in(KeyProviderVO_.uuid, providerUuids)
                .listTuple();
        return tuples.stream().collect(Collectors.toMap(
                tuple -> tuple.get(0, String.class),
                tuple -> tuple.get(1, KeyProviderType.class)));
    }
}
