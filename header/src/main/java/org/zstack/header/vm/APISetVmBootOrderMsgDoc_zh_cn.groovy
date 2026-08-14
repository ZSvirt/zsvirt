package org.zstack.header.vm

import org.zstack.header.vm.APISetVmBootOrderEvent

doc {
	title "指定云主机启动设备(SetVmBootOrder)"

	category "vmInstance"

	desc """指定一个云主机的启动设备"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmBootOrderMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmBootOrder"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "bootOrder"
					enclosedIn "setVmBootOrder"
					desc "启动设备。`CdRom`：光驱，`HardDisk`：云盘，`Network`：网络。若该字段不指定，则表示使用系统默认启动设备顺序(HardDisk, CdRom, Network)"
					location "body"
					type "List"
					optional true
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
			clz APISetVmBootOrderEvent.class
		}
	}
}