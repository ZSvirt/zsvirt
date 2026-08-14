package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.APIUpdateVpcHaGroupEvent

doc {
	title "UpdateVpcHaGroup"

	category "vpcHa"

	desc """更新高可用组"""

	rest {
		request {
			url "PUT /v1/vpc/hagroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVpcHaGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVpcHaGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "name"
					enclosedIn "updateVpcHaGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "description"
					enclosedIn "updateVpcHaGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
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
			clz APIUpdateVpcHaGroupEvent.class
		}
	}
}