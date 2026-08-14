package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostnameEvent

doc {
	title "UpdateHostname"

	category "host"

	desc """更新主机hostname"""

	rest {
		request {
			url "PUT /v1/hosts/hostname/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostnameMsg.class

			desc """更新主机hostname"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostname"
					desc "主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.20"
				}
				column {
					name "hostname"
					enclosedIn "updateHostname"
					desc "主机hostname"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.20"
				}
			}
		}

		response {
			clz APIUpdateHostnameEvent.class
		}
	}
}