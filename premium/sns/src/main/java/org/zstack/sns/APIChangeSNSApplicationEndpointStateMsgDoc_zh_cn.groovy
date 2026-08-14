package org.zstack.sns

import org.zstack.sns.APIChangeSNSApplicationEndpointStateEvent

doc {
	title "ChangeSNSApplicationEndpointState"

	category "sns"

	desc """更改SNS应用终端状态"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeSNSApplicationEndpointStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeSNSApplicationEndpointState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "stateEvent"
					enclosedIn "changeSNSApplicationEndpointState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
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
			clz APIChangeSNSApplicationEndpointStateEvent.class
		}
	}
}