package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.APIDeleteVpcHaGroupEvent

doc {
	title "DeleteVpcHaGroup"

	category "vpcHa"

	desc """删除高可用组"""

	rest {
		request {
			url "DELETE /v1/vpc/hagroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVpcHaGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5"
				}
			}
		}

		response {
			clz APIDeleteVpcHaGroupEvent.class
		}
	}
}