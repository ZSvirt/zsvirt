package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateTemplatedVmInstanceEvent

doc {
	title "UpdateTemplatedVmInstance"

	category "vmInstance"

	desc """更新虚拟机模板"""

	rest {
		request {
			url "PUT /v1/vm-instances/templatedVmInstance/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateTemplatedVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateTemplatedVmInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "name"
					enclosedIn "updateTemplatedVmInstance"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "description"
					enclosedIn "updateTemplatedVmInstance"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "cpuNum"
					enclosedIn "updateTemplatedVmInstance"
					desc "CPU数目"
					location "body"
					type "Integer"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "memorySize"
					enclosedIn "updateTemplatedVmInstance"
					desc "内存大小"
					location "body"
					type "Long"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
			}
		}

		response {
			clz APIUpdateTemplatedVmInstanceEvent.class
		}
	}
}