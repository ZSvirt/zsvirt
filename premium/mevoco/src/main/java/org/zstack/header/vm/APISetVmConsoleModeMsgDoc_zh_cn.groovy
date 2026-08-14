package org.zstack.header.vm

import org.zstack.header.vm.APISetVmConsoleModeEvent

doc {
	title "SetVmConsoleMode"

	category "mevoco"

	desc """设置云主机控制台模式"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmConsoleModeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmConsoleMode"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "mode"
					enclosedIn "setVmConsoleMode"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APISetVmConsoleModeEvent.class
		}
	}
}