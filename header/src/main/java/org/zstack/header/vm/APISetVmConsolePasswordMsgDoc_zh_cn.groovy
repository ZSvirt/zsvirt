package org.zstack.header.vm

import org.zstack.header.vm.APISetVmConsolePasswordEvent

doc {
	title "设置云主机控制台密码(SetVmConsolePassword)"

	category "vmInstance"

	desc """设置一个云主机控制台密码"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmConsolePasswordMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmConsolePassword"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "consolePassword"
					enclosedIn "setVmConsolePassword"
					desc "控制台密码，明文字符串"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APISetVmConsolePasswordEvent.class
		}
	}
}