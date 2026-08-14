package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIDeleteNvmeServerEvent

doc {
	title "DeleteNvmeServer"

	category "storageDevice"

	desc """删除NVMe服务器"""

	rest {
		request {
			url "DELETE /v1/storage-devices/nvme/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteNvmeServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIDeleteNvmeServerEvent.class
		}
	}
}