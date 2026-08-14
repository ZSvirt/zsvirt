package org.zstack.vpc

import org.zstack.vpc.APIQueryVpcHaGroupNetworkServiceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVpcHaGroupNetworkServiceRef"

	category "vpc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vpc/hagroups/networkserviceref/"
			url "GET /v1/vpc/hagroups/networkserviceref/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVpcHaGroupNetworkServiceRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVpcHaGroupNetworkServiceRefReply.class
		}
	}
}