package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIUpdateNvmeServerEvent

doc {
	title "UpdateNvmeServer"

	category "storage.device"

	desc """更新NVMe服务器配置"""

	rest {
		request {
			url "PUT /v1/storage-devices/nvme/servers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateNvmeServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateNvmeServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "name"
					enclosedIn "updateNvmeServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
			}
		}

		response {
			clz APIUpdateNvmeServerEvent.class
		}
	}
}