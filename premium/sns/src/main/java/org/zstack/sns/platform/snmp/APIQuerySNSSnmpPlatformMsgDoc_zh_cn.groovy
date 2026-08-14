package org.zstack.sns.platform.snmp

import org.zstack.sns.platform.email.APIQuerySNSEmailPlatformReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSSnmpPlatform"

	category "sns"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/sns/application-platforms/snmp"
			url "GET /v1/sns/application-platforms/snmp/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSSnmpPlatformMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSEmailPlatformReply.class
		}
	}
}