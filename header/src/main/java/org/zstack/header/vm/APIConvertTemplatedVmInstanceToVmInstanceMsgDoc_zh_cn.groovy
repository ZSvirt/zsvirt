package org.zstack.header.vm

import org.zstack.header.vm.APIConvertTemplatedVmInstanceToVmInstanceEvent

doc {
	title "虚拟机模板转换为虚拟机(ConvertTemplatedVmInstanceToVmInstance)"

	category "vmInstance"

	desc """虚拟机模板转换为虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/{templatedVmInstanceUuid}/convert-to-vmInstance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIConvertTemplatedVmInstanceToVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "TemplatedVmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机模板UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "虚拟机名称"
					location "body"
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
				column {
					name "resetTpm"
					enclosedIn "params"
					desc "转换成虚拟机后是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIConvertTemplatedVmInstanceToVmInstanceEvent.class
		}
	}
}