package org.zstack.header.vm

import org.zstack.header.vm.APIRebootVmInstanceEvent

doc {
	title "重启虚拟机(RebootVmInstance)"

	category "vmInstance"

	desc """重启一个虚拟机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRebootVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "rebootVmInstance"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
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
			clz APIRebootVmInstanceEvent.class
		}
	}
}