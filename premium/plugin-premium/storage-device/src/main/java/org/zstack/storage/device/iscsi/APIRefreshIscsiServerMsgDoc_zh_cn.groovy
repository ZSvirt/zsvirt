package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIRefreshIscsiServerEvent

doc {
	title "RefreshIscsiServer"

	category "storage.device"

	desc """刷新iSCSI服务器"""

	rest {
		request {
			url "POST /v1/storage-devices/iscsi/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshIscsiServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIRefreshIscsiServerEvent.class
		}
	}
}