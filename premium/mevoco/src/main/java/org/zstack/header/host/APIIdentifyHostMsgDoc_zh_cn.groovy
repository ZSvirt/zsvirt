package org.zstack.header.host

import org.zstack.header.host.APIIdentifyHostEvent

doc {
	title "IdentifyHost"

	category "host"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/hosts/kvm/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIIdentifyHostMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "identifyHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "interval"
					enclosedIn "identifyHost"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIIdentifyHostEvent.class
		}
	}
}