package org.zstack.iam1.api.ensemble

import org.zstack.iam1.entity.ensemble.AccountSharingView
import org.zstack.iam1.entity.ensemble.AccountGroupSharingView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源被分享对象的结果"

	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "4.10.0"
	}
	field {
		name "masterResourceUuid"
		desc "如果资源在某个资源组里面，这个值是资源组中主要资源的UUID。如果没有，这个值是null"
		type "String"
		since "4.10.0"
	}
	field {
		name "masterResourceType"
		desc "如果资源在某个资源组里面，这个值是资源组中主要资源的类型。如果没有，这个值是null"
		type "String"
		since "4.10.0"
	}
	field {
		name "toPublic"
		desc "资源是否被全局分享"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "accounts"
		path "org.zstack.iam1.api.ensemble.APIGetResourceSharingReply.accounts"
		desc "资源被指定分享给哪些账户，不包含分享账户组中涉及的账户"
		type "List"
		since "4.10.0"
		clz AccountSharingView.class
	}
	ref {
		name "accountGroups"
		path "org.zstack.iam1.api.ensemble.APIGetResourceSharingReply.accountGroups"
		desc "资源被指定分享给哪些账户组"
		type "List"
		since "4.10.0"
		clz AccountGroupSharingView.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.iam1.api.ensemble.APIGetResourceSharingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
