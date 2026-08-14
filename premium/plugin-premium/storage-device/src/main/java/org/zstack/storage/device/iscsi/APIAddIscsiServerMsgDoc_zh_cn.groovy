package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIAddIscsiServerEvent

doc {
	title "AddIscsiServer"

	category "storage.device"

	desc """添加iSCSI服务器"""

	rest {
		request {
			url "POST /v1/storage-devices/iscsi/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddIscsiServerMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "ip"
					enclosedIn "params"
					desc "IP地址"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "port"
					enclosedIn "params"
					desc "端口，默认为3260"
					location "body"
					type "Integer"
					optional true
					since "3.0.0"
				}
				column {
					name "chapUserName"
					enclosedIn "params"
					desc "CHAP用户名，默认为空"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "chapUserPassword"
					enclosedIn "params"
					desc "CHAP密码，默认为空"
					location "body"
					type "String"
					optional true
					since "3.0.0"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddIscsiServerEvent.class
		}
	}
}