package org.zstack.header.vm.cdrom

import org.zstack.header.vm.cdrom.APISetVmInstanceDefaultCdRomEvent

doc {
	title "SetVmInstanceDefaultCdRom"

	category "vmInstance"

	desc """设置云主机默认CDROM"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/cdroms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmInstanceDefaultCdRomMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmInstanceDefaultCdRom"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "setVmInstanceDefaultCdRom"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
			}
		}

		response {
			clz APISetVmInstanceDefaultCdRomEvent.class
		}
	}
}