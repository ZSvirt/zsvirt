package org.zstack.header.vm

import org.zstack.header.vm.APIConvertVmInstanceToTemplatedVmInstanceEvent

doc {
	title "虚拟机转换为虚拟机模板(ConvertVmInstanceToTemplatedVmInstance)"

	category "vmInstance"

	desc """虚拟机转换为虚拟机模板"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/convert-to-templatedVmInstance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIConvertVmInstanceToTemplatedVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
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
			clz APIConvertVmInstanceToTemplatedVmInstanceEvent.class
		}
	}
}