package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIAddVmToAffinityGroupEvent

doc {
	title "AddVmToAffinityGroup"

	category "affinityGroup"

	desc """添加云主机到亲和组"""

	rest {
		request {
			url "POST /v1/affinity-groups/{affinityGroupUuid}/vm-instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVmToAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "affinityGroupUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			clz APIAddVmToAffinityGroupEvent.class
		}
	}
}