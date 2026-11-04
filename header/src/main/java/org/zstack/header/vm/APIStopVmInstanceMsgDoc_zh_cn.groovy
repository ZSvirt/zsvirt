package org.zstack.header.vm

import org.zstack.header.vm.APIStopVmInstanceEvent

doc {
	title "停止虚拟机(StopVmInstance)"

	category "vmInstance"

	desc """停止一个虚拟机"""

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
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "type"
					enclosedIn "stopVmInstance"
					desc "停止虚拟机的方式。`grace`：优雅关机，需要虚拟机里安装了相关ACPI驱动；`cold`：冷关机，相当于直接断电"
					location "body"
					type "String"
					optional true
					since "zsv_0.6"
					values ("grace","cold")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "stopHA"
					enclosedIn "stopVmInstance"
					desc "彻底关闭HA虚拟机"
					location "body"
					type "String"
					optional true
					since "zsv_2.2"
				}
			}
		}

		response {
			clz APIStopVmInstanceEvent.class
		}
	}
}