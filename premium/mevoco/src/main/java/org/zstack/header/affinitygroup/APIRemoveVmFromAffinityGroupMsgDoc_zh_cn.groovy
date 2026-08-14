package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIRemoveVmFromAffinityGroupEvent

doc {
	title "RemoveVmFromAffinityGroup"

	category "affinityGroup"

	desc """从亲和组中移除云主机"""

	rest {
		request {
			url "DELETE /v1/affinity-groups/{affinityGroupUuid}/vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveVmFromAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "affinityGroupUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional false
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
			clz APIRemoveVmFromAffinityGroupEvent.class
		}
	}
}