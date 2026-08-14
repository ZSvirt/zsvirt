package org.zstack.header.vm

import org.zstack.header.vm.APIRecoverVmInstanceEvent

doc {
	title "恢复已删除云主机(RecoverVmInstance)"

	category "vmInstance"

	desc """恢复一个处于Destroyed状态的云主机。恢复后云主机处于Stopped状态并且没有IP地址"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverVmInstance"
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
			clz APIRecoverVmInstanceEvent.class
		}
	}
}