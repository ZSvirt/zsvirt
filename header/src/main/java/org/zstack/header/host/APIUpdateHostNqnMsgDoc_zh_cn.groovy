package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostNqnEvent

doc {
	title "UpdateHostNqn"

	category "host"

	desc """更新主机nqn"""

	rest {
		request {
			url "PUT /v1/hosts/nqn/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostNqnMsg.class

			desc """更新主机nqn"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostNqn"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "nqn"
					enclosedIn "updateHostNqn"
					desc "NVMe 限定名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
			}
		}

		response {
			clz APIUpdateHostNqnEvent.class
		}
	}
}