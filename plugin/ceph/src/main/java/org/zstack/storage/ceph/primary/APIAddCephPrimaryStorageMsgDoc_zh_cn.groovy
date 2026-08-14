package org.zstack.storage.ceph.primary

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "添加 Ceph 主存储(AddCephPrimaryStorage)"

	category "storage.ceph.primary"

	desc """添加 Ceph 主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/ceph"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddCephPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "monUrls"
					enclosedIn "params"
					desc "Ceph mon 的地址列表"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "rootVolumePoolName"
					enclosedIn "params"
					desc "指定 Root Volume 可使用的 Ceph pool 名字"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "dataVolumePoolName"
					enclosedIn "params"
					desc "指定 Data Volume 可使用的 Ceph pool 名字"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "imageCachePoolName"
					enclosedIn "params"
					desc "指定镜像缓存可使用的 Ceph pool 名字"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "未使用"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "Ceph 主存储的名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "Ceph 主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "Ceph 主存储的类型，此处为 Ceph"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，Ceph 主存储会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
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
			clz APIAddPrimaryStorageEvent.class
		}
	}
}