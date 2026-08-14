package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.APIChangeVpcHaGroupMonitorIpsEvent

doc {
	title "ChangeVpcHaGroupMonitorIps"

	category "vpcHa"

	desc """更新高可用组仲裁ip"""

	rest {
		request {
			url "PUT /v1/vpc/hagroups/{uuid}/monitorIps"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVpcHaGroupMonitorIpsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVpcHaGroupMonitorIps"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "monitorIps"
					enclosedIn "changeVpcHaGroupMonitorIps"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
			}
		}

		response {
			clz APIChangeVpcHaGroupMonitorIpsEvent.class
		}
	}
}