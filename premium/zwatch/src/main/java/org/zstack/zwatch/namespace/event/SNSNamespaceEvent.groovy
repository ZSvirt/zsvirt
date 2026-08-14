package org.zstack.zwatch.namespace.event

import org.zstack.sns.SNSCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.SNSNamespace

/**
 * Created by Qi Le on 2019-07-15
 */
class SNSNamespaceEvent {
    SNSNamespaceEvent() {
        SNSNamespace.SendSmsFailed.onCanonicalEvent(SNSCanonicalEvents.SEND_SMS_FAILED_PATH) { SNSCanonicalEvents.SNSSendSmsFailedData data ->
            return new EventFamily.Event(data.endpointUuid, data.phoneNumber, data.errCode, data.errMessage)
        }
    }
}
