package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIDeleteAffinityGroupEvent

doc {
	title "DeleteAffinityGroup"

	category "affinityGroup"

	desc """删除亲和组"""

	rest {
		request {
			url "DELETE /v1/affinity-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIDeleteAffinityGroupEvent.class
		}
	}
}