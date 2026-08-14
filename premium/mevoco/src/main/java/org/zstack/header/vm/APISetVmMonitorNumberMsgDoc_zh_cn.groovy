package org.zstack.header.vm

import org.zstack.header.vm.APISetVmMonitorNumberEvent

doc {
	title "SetVmMonitorNumber"

	category "mevoco"

	desc """设置云主机支持的屏幕个数"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmMonitorNumberMsg.class

			desc """设置云主机支持的屏幕个数"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmMonitorNumber"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "monitorNumber"
					enclosedIn "setVmMonitorNumber"
					desc "显示器个数"
					location "body"
					type "Integer"
					optional false
					since "2.1.2"
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
			clz APISetVmMonitorNumberEvent.class
		}
	}
}