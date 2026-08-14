package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIAddNvmeServerEvent

doc {
	title "AddNvmeServer"

	category "storageDevice"

	desc """添加NVMe服务器"""

	rest {
		request {
			url "POST /v1/storage-devices/nvme/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddNvmeServerMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "名称"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "ip"
					enclosedIn "params"
					desc "IP地址"
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "port"
					enclosedIn "params"
					desc "端口"
					location "body"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "transport"
					enclosedIn "params"
					desc "目标传输类型"
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIAddNvmeServerEvent.class
		}
	}
}