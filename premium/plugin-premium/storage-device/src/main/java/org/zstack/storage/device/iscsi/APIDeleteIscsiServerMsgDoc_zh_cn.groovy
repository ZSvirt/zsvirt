package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIDeleteIscsiServerEvent

doc {
	title "DeleteIscsiServer"

	category "storage.device"

	desc """删除iSCSI服务器"""

	rest {
		request {
			url "DELETE /v1/storage-devices/iscsi/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteIscsiServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "iSCSI服务器的UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIDeleteIscsiServerEvent.class
		}
	}
}