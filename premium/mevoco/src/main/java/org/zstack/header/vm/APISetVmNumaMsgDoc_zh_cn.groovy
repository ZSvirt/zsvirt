package org.zstack.header.vm

import org.zstack.header.vm.APISetVmNumaEvent

doc {
	title "SetVmNuma"

	category "mevoco"

	desc """设置云主机是否开启了Vnuma功能,如果开启，云主机在重启后会自动生成numa结构"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmNumaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmNuma"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "enable"
					enclosedIn "setVmNuma"
					desc "云主机是否被标识为Vnuma可访问"
					location "body"
					type "boolean"
					optional false
					since "3.13.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
			}
		}

		response {
			clz APISetVmNumaEvent.class
		}
	}
}