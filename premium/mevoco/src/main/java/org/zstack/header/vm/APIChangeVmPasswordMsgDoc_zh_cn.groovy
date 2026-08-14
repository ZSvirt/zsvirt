package org.zstack.header.vm

import org.zstack.header.vm.APIChangeVmPasswordEvent

doc {
	title "ChangeVmPassword"

	category "mevoco"

	desc """变更云主机密码"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmPasswordMsg.class

			desc """变更云主机密码"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVmPassword"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "changeVmPassword"
					desc "密码"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "account"
					enclosedIn "changeVmPassword"
					desc "账户"
					location "body"
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
			clz APIChangeVmPasswordEvent.class
		}
	}
}