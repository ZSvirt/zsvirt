package org.zstack.header.vm

import org.zstack.header.vm.APIPauseVmInstanceEvent

doc {
	title "暂停云主机(PauseVmInstance)"

	category "vmInstance"

	desc """暂停一个云主机，云主机状态仍然存在内存里面，稍后可以恢复"""

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
					desc "云主机UUID"
					location "url"
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
			clz APIPauseVmInstanceEvent.class
		}
	}
}