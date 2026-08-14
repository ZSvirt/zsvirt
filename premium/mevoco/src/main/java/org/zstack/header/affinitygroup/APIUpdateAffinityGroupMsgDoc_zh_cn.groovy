package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIUpdateAffinityGroupEvent

doc {
	title "UpdateAffinityGroup"

	category "affinityGroup"

	desc """更新亲和组信息"""

	rest {
		request {
			url "PUT /v1/affinity-groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAffinityGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "name"
					enclosedIn "updateAffinityGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "description"
					enclosedIn "updateAffinityGroup"
					desc "资源的详细描述"
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
			}
		}

		response {
			clz APIUpdateAffinityGroupEvent.class
		}
	}
}