package org.zstack.header.vm

import org.zstack.header.vm.APIDetachIsoFromVmInstanceEvent

doc {
	title "卸载虚拟机上的ISO(DetachIsoFromVmInstance)"

	category "vmInstance"

	desc """如果虚拟机上挂载有ISO，卸载它"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/iso"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachIsoFromVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "isoUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv_2.3.1"
				}
			}
		}

		response {
			clz APIDetachIsoFromVmInstanceEvent.class
		}
	}
}