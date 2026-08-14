package org.zstack.header.vm

import org.zstack.header.vm.APIGetNicQosReply

doc {
	title "GetNicQos"

	category "mevoco"

	desc """获取云主机网卡限速"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/nic-qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetNicQosMsg.class

			desc """获取云主机网卡限速"""

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
					name "forceSync"
					enclosedIn ""
					desc "是否到物理机上去同步数据"
					location "query"
					type "Boolean"
					optional true
					since "3.3.0"
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
			clz APIGetNicQosReply.class
		}
	}
}