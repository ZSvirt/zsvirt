package org.zstack.header.vm

import org.zstack.header.vm.APIAttachVmNicToVmEvent

doc {
	title "AttachVmNicToVm"

	category "vmInstance"

	desc """加载网卡到虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/nices/{vmNicUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachVmNicToVmMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "虚拟机网卡UUID"
					location "url"
					type "String"
					optional false
					since "zsv_3.10"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv_3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv_3.10"
				}
			}
		}

		response {
			clz APIAttachVmNicToVmEvent.class
		}
	}
}