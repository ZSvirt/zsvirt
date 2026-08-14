package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteNicQosEvent

doc {
	title "DeleteNicQos"

	category "mevoco"

	desc """取消云主机网卡限速"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/nic-qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteNicQosMsg.class

			desc """取消云主机网卡限速"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机网卡的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "direction"
					enclosedIn ""
					desc "入方向还是出方向(in or out)"
					location "query"
					type "String"
					optional false
					since "0.6"
					values ("in","out")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteNicQosEvent.class
		}
	}
}