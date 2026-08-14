package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.APICreateVpcHaGroupEvent

doc {
	title "CreateVpcHaGroup"

	category "vpcHa"

	desc """创建高可用组"""

	rest {
		request {
			url "POST /v1/vpc/hagroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVpcHaGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "monitorIps"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
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
			clz APICreateVpcHaGroupEvent.class
		}
	}
}