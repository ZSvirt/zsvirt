package org.zstack.header.vm

import org.zstack.header.vm.APISetVmCleanTrafficEvent

doc {
	title "SetVmCleanTraffic"

	category "mevoco"

	desc """设置云主机防IP欺骗启用状态"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmCleanTrafficMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmCleanTraffic"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "enable"
					enclosedIn "setVmCleanTraffic"
					desc "是否启用云主机 clean-traffic"
					location "body"
					type "boolean"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APISetVmCleanTrafficEvent.class
		}
	}
}