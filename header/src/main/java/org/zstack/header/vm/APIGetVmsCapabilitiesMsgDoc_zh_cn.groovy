package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmsCapabilitiesEvent

doc {
	title "GetVmsCapabilities"

	category "vmInstance"

	desc """批量获取云主机能力"""

	rest {
		request {
			url "POST /v1/vm-instances/capabilities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmsCapabilitiesMsg.class

			desc """"""

			params {

				column {
					name "vmUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "zsv 3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
			}
		}

		response {
			clz APIGetVmsCapabilitiesEvent.class
		}
	}
}