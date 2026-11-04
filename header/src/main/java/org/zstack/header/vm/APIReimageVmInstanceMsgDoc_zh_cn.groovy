package org.zstack.header.vm

import org.zstack.header.vm.APIReimageVmInstanceEvent

doc {
	title "重置虚拟机(ReimageVmInstance)"

	category "vmInstance"

	desc """将一个虚拟机的根云盘重置为最初状态。该API只对从非ISO创建出的虚拟机有效。

>警告：执行该API后，虚拟机根云盘重置成最初创建的状态，意味着所有后续写入的数据都会被丢失，该操作不可逆！
"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReimageVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "reimageVmInstance"
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
			clz APIReimageVmInstanceEvent.class
		}
	}
}