package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostIscsiInitiatorNameEvent

doc {
	title "UpdateHostIscsiInitiatorName"

	category "host"

	desc """更新主机iscsiInitiatorName"""

	rest {
		request {
			url "PUT /v1/hosts/kvm/iscsiInitiatorName/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostIscsiInitiatorNameMsg.class

			desc """更新主机iscsiInitiatorName"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostIscsiInitiatorName"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.10.6"
				}
				column {
					name "iscsiInitiatorName"
					enclosedIn "updateHostIscsiInitiatorName"
					desc "iscsi启动器名称"
					location "body"
					type "String"
					optional false
					since "4.10.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.6"
				}
			}
		}

		response {
			clz APIUpdateHostIscsiInitiatorNameEvent.class
		}
	}
}