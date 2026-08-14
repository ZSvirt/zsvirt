package org.zstack.header.host

import org.zstack.header.host.APIChangeHostPasswordEvent

doc {
	title "ChangeHostPassword"

	category "host"

	desc """修改物理机 root 密码"""

	rest {
		request {
			url "PUT /v1/hosts/kvm/{hostUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeHostPasswordMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn "changeHostPassword"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.8"
				}
				column {
					name "password"
					enclosedIn "changeHostPassword"
					desc "密码"
					location "body"
					type "String"
					optional false
					since "3.8"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
			}
		}

		response {
			clz APIChangeHostPasswordEvent.class
		}
	}
}