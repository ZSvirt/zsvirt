package org.zstack.header.vm

import org.zstack.header.vm.APITakeVmConsoleScreenshotEvent

doc {
	title "TakeVmConsoleScreenshot"

	category "vmInstance"

	desc """获取虚拟机控制台截图"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APITakeVmConsoleScreenshotMsg.class

			desc """获取虚拟机控制台截图"""

			params {

				column {
					name "uuid"
					enclosedIn "takeVmConsoleScreenshot"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APITakeVmConsoleScreenshotEvent.class
		}
	}
}