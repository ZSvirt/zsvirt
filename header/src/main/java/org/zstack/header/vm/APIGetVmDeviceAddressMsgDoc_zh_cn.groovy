package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmDeviceAddressReply

doc {
	title "GetVmDeviceAddress"

	category "vmInstance"

	desc """获取云主机内部与云平台资源对应的设备地址"""

	rest {
		request {
			url "GET /v1/vm-instances/devices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmDeviceAddressMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "query"
					type "String"
					optional false
					since "zsv 3.10.0"
				}
				column {
					name "resourceTypes"
					enclosedIn ""
					desc "资源类型"
					location "query"
					type "List"
					optional false
					since "zsv 3.10.0"
					values ("VolumeVO")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
			}
		}

		response {
			clz APIGetVmDeviceAddressReply.class
		}
	}
}