package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APICreateAffinityGroupEvent

doc {
	title "CreateAffinityGroup"

	category "affinityGroup"

	desc """创建亲和组"""

	rest {
		request {
			url "POST /v1/affinity-groups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "policy"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.2"
					values ("antiSoft")
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.2"
					values ("host")
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "subType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APICreateAffinityGroupEvent.class
		}
	}
}