package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateVmPriorityEvent

doc {
	title "UpdateVmPriority"

	category "vmInstance"

	desc """更改云主机优先级"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmPriorityMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVmPriority"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.7"
				}
				column {
					name "priority"
					enclosedIn "updateVmPriority"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 3.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.7"
				}
			}
		}

		response {
			clz APIUpdateVmPriorityEvent.class
		}
	}
}