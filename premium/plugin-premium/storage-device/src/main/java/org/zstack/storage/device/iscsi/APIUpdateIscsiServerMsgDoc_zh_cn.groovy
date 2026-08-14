package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIUpdateIscsiServerEvent

doc {
	title "UpdateIscsiServer"

	category "storage.device"

	desc """更新iSCSI服务器配置"""

	rest {
		request {
			url "PUT /v1/storage-devices/iscsi/servers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateIscsiServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateIscsiServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "name"
					enclosedIn "updateIscsiServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "chapUserName"
					enclosedIn "updateIscsiServer"
					desc "CHAP用户名"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "chapUserPassword"
					enclosedIn "updateIscsiServer"
					desc "CHAP密码"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "state"
					enclosedIn "updateIscsiServer"
					desc "启用状态"
					location "body"
					type "String"
					optional true
					since "3.0.0"
					values ("Enabled","Disabled")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
			clz APIUpdateIscsiServerEvent.class
		}
	}
}