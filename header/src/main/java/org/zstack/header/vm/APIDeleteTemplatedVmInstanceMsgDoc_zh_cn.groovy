package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteTemplatedVmInstanceEvent

doc {
	title "DeleteTemplatedVmInstance"

	category "vmInstance"

	desc """删除虚拟机模板"""

	rest {
		request {
			url "DELETE /v1/vm-instances/templatedVmInstance/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteTemplatedVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
			}
		}

		response {
			clz APIDeleteTemplatedVmInstanceEvent.class
		}
	}
}