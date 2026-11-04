package org.zstack.header.vm

import org.zstack.header.vm.APIExpungeVmInstanceEvent

doc {
	title "彻底删除虚拟机(ExpungeVmInstance)"

	category "vmInstance"

	desc """彻底删除一个处于Destroyed状态的虚拟机。该操作会从数据库里面删除虚拟机并在主存储上删除该虚拟机的根云盘。该操作一旦执行就不可恢复"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExpungeVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "expungeVmInstance"
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
			clz APIExpungeVmInstanceEvent.class
		}
	}
}