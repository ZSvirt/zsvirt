package org.zstack.header.sriov

import org.zstack.header.sriov.APIIsVfNicAvailableInL3NetworkReply

doc {
	title "IsVfNicAvailableInL3Network"

	category "sriov"

	desc """查询物理机中是否存在可用的VF网卡"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/hosts/{hostUuid}/vfnicavailable"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIIsVfNicAvailableInL3NetworkMsg.class

			desc """查询物理机中是否存在可用的VF网卡"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIIsVfNicAvailableInL3NetworkReply.class
		}
	}
}