package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIChangeAffinityGroupStateEvent

doc {
	title "ChangeAffinityGroupState"

	category "affinityGroup"

	desc """改变亲和组的使用状态"""

	rest {
		request {
			url "PUT /v1/affinity-groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAffinityGroupStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAffinityGroupState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "stateEvent"
					enclosedIn "changeAffinityGroupState"
					desc ""
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIChangeAffinityGroupStateEvent.class
		}
	}
}