package org.zstack.zwatch.namespace.event

import org.zstack.header.keyprovider.KeyProviderCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.KeyProviderNamespace

class KeyProviderNamespaceEvent {
    KeyProviderNamespaceEvent() {
        KeyProviderNamespace.KeyProviderDefaultServiceUnavailable.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_UNAVAILABLE_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.statusCode, data.statusMessage, data.defaultProvider)
        }.onEventBarrier(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_UNAVAILABLE_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data ->
            return data.keyProviderUuid
        }

        KeyProviderNamespace.KeyProviderDefaultServiceRecovered.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.statusCode, data.statusMessage, data.defaultProvider)
        }.onRecoverReturnResourceId { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return data.keyProviderUuid
        }.onEventBarriersCleaner(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return data.keyProviderUuid
        }

        KeyProviderNamespace.KeyProviderResourceServiceUnavailable.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_UNAVAILABLE_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.statusCode, data.statusMessage, data.defaultProvider)
        }.onEventBarrier(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_UNAVAILABLE_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data ->
            return data.keyProviderUuid
        }

        KeyProviderNamespace.KeyProviderResourceServiceRecovered.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.statusCode, data.statusMessage, data.defaultProvider)
        }.onRecoverReturnResourceId { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return data.keyProviderUuid
        }.onEventBarriersCleaner(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data ->
            return data.keyProviderUuid
        }

        KeyProviderNamespace.KeyProviderCertificateExpiring.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_EXPIRING_PATH) { KeyProviderCanonicalEvents.KeyProviderCertificateExpiringData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.certificateType, data.expiredDate, data.daysLeft, data.reportDate, data.defaultProvider)
        }.onEventBarrier(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_EXPIRING_PATH) { KeyProviderCanonicalEvents.KeyProviderCertificateExpiringData data ->
            return data.keyProviderUuid + ":" + data.certificateType + ":" + data.expiredDate
        }

        KeyProviderNamespace.KeyProviderCertificateRecovered.onCanonicalEvent(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData data ->
            return new EventFamily.Event(data.keyProviderUuid, data.keyProviderUuid, data.keyProviderName,
                    data.keyProviderType, data.certificateType, data.expiredDate, data.daysLeft, data.reportDate, data.defaultProvider)
        }.onRecoverReturnResourceId { KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData data ->
            return data.keyProviderUuid + ":" + data.certificateType + ":" + data.expiredDate
        }.onEventBarriersCleaner(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_RECOVERED_PATH) { KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData data ->
            return data.keyProviderUuid + ":" + data.certificateType + ":" + data.expiredDate
        }
    }
}
