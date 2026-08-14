package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIAttachEipEvent

doc {
	title "绑定弹性IP(AttachEip)"

	category "弹性IP"

	desc """绑定弹性IP"""

	rest {
		request {
			url "POST /v1/eips/{eipUuid}/vm-instances/nics/{vmNicUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachEipMsg.class

			desc """"""

			params {

				column {
					name "eipUuid"
					enclosedIn "params"
					desc "弹性IP UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "usedIpUuid"
					enclosedIn "params"
					desc "IP地址Uuid"
					location "body"
					type "String"
					optional true
					since "3.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIAttachEipEvent.class
		}
	}
}