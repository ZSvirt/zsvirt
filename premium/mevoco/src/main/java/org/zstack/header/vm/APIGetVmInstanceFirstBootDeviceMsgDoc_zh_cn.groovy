package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmInstanceFirstBootDeviceReply

doc {
	title "GetVmInstanceFirstBootDevice"

	category "mevoco"

	desc """获取云主机第一启动项"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/first-boot-device"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmInstanceFirstBootDeviceMsg.class

			desc """获取云主机第一启动项"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIGetVmInstanceFirstBootDeviceReply.class
		}
	}
}