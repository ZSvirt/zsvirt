package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APICreateRoleEvent

doc {
	title "CreateRole"

	category "rbac"

	desc """创建角色"""

	rest {
		request {
			url "POST /v1/identities/roles"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateRoleMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "角色名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "角色的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "policies"
					enclosedIn "params"
					desc "角色初始的权限条目, 列表形式。可以是 '.header.image.APIAddImageMsg' 表示有该角色的账户能上传镜像"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "baseOnRole"
					enclosedIn "params"
					desc "新创建的角色基于哪个已存在角色的, 传入这个已存在角色的 UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "新创建的角色使用哪个UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APICreateRoleEvent.class
		}
	}
}