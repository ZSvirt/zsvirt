package org.zstack.header.vm

import org.zstack.header.vm.APIStopVmInstanceEvent

doc {
	title "停止云主机(StopVmInstance)"

	category "vmInstance"

	desc """停止一个云主机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStopVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "stopVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "stopVmInstance"
					desc "停止云主机的方式。`grace`：优雅关机，需要云主机里安装了相关ACPI驱动；`cold`：冷关机，相当于直接断电"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("grace","cold")
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
				column {
					name "stopHA"
					enclosedIn "stopVmInstance"
					desc "彻底关闭HA云主机"
					location "body"
					type "String"
					optional true
					since "zsv 2.2"
				}
			}
		}

		response {
			clz APIStopVmInstanceEvent.class
		}
	}
}