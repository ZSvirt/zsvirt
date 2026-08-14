package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIAttachSecurityGroupToL3NetworkEvent

doc {
	title "AttachSecurityGroupToL3Network"

	category "securityGroup"

	desc """用户可以使用AttachSecurityGroupToL3Network来挂载一个安全组到L3网络"""

	rest {
		request {
			url "POST /v1/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachSecurityGroupToL3NetworkMsg.class

			desc """用户可以使用AttachSecurityGroupToL3Network来挂载一个安全组到L3网络"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn "params"
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
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
			clz APIAttachSecurityGroupToL3NetworkEvent.class
		}
	}
}