package org.zstack.zwatch.namespace.event

import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.ThirdpartyAlertNamespace

class ThirdpartyAlertNamespaceEvent {
    ThirdpartyAlertNamespaceEvent() {
        ThirdpartyAlertNamespace.ThirdpartyAlert.onCanonicalEvent(ThirdpartyAlertCanonicalEvents.THIRDPARTY_ALERT_PATH) { ThirdpartyAlertCanonicalEvents.ThirdpartyAlertData data ->
            return new EventFamily.Event(data.platformUuid, data.uuid, data.platformUuid, data.product, data.service, data.message, data.dataSource, data.metric, data.alertLevel, data.alertTime, data.dimensions)
        }
    }
}
