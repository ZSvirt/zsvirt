package org.zstack.header.vm

import org.zstack.header.vm.APIPauseVmInstanceEvent

doc {
	title "暂停虚拟机(PauseVmInstance)"

	category "vmInstance"

	desc """暂停一个虚拟机，虚拟机状态仍然存在内存里面，稍后可以恢复"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPauseVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "pauseVmInstance"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
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
			}
		}

		response {
			clz APIPauseVmInstanceEvent.class
		}
	}
}