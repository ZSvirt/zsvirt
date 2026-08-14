package org.zstack.header.vm

import org.zstack.header.vm.APISetVmHostnameEvent

doc {
	title "设置云主机Hostname(SetVmHostname)"

	category "vmInstance"

	desc """设置云主机Hostname。注意，ZStack通过DHCP服务器配置云主机hostname，如果云主机本身采用静态hostname方式，该API配置的hostname不生效。"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmHostnameMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmHostname"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "hostname"
					enclosedIn "setVmHostname"
					desc "hostname，必须符合RFC 1123标准"
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
			clz APISetVmHostnameEvent.class
		}
	}
}