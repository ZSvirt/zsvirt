package org.zstack.header.query

doc {
	rest {
		request {
			params {
				desc """
#### <a name="query-conditions">查询条件</a>

ZStack支持类似于MySQL数据库类似的查询条件，例如：

```
uuid=bfa67f956afb430890aa49db14b85153
totalCapacity>2000
vmInstanceUuid!=null
```

> **字段名、查询操作符、匹配值三者之间不能有任何空格**。例如`uuid = 25506342d1384c07b7342373a57475b9`就是一个错误的查询条件，必须写为
`uuid=25506342d1384c07b7342373a57475b9`。

多个查询条件之间是**与**关系。总共支持10个查询操作符：

- `=`: 等于。可以用`=null`来测试一个字段是否为`null`，例如：

	```
	vmInstanceUuid=null
	```
	
- `!=`: 不等于。可以用`!=null`来测试一个字段是否部位`null`，例如：

	```
	vmInstanceUuid!=null
	```

- `>`: 大于
- `<`: 小于
- `>=`: 大于等于
- `<=`: 小于等于
- `?=`: `in`操作符，测试字段值是否在一个集合。集合中的值以`,`分隔。例如测试*uuid*是否属于某个集合：
	
	```
	uuid?=25506342d1384c07b7342373a57475b9,bc58d68090ac42358c0cb0fe72e3287f
	```
	
- `!?=`: `not int`操作符，测试字段值是否**不属于**一个集合。集合中的值以`,`分隔，例如测试*name*是否不等于VM1和VM2:

	```
	name!?=VM1,VM2
	```
	
- `~=`: 字符串模糊匹配，相当于MySQL中的`like`操作。使用`%`匹配一个或多个字符，使用`_`匹配一个字符。
	例如查询一个名字是以*IntelCore*开头的：
	
	```
	name~=IntelCore%
	```
	
	或者查询一个名字是以*IntelCore*开头，以*7*结尾，中间模糊匹配一个字符：
	
	```
	name~=IntelCore_7
	```  
	  
	这样名字是*IntelCoreI7*，*IntelCoreM7*的记录都会匹配上。
	
- `!~=`: 模糊匹配非操作。查询一个字段不能模糊匹配到某个字符串，匹配条件与`~=`相同

#### <a name="query-pagination">分页查询</a>

`start`、`limit`、`replyWithCount`三个字段可以配合使用实现分页查询。其中`start`指定其实查询位置，`limit`指定查询返回的最大记录数，而
`replyWithCount`被设置成true后，查询返回中会包含满足查询条件的记录总数，跟`start`值比较就可以得知还需几次分页。

例如总共有1000记录满足查询条件，使用如下组合:

```
start=0 limit=100 replyWithCount=true
```

则API返回将包含头100条记录，以及`total`字段等于1000，表示总共满足条件的记录为1000。
"""

				column {
					name "conditions"
					desc "见[查询条件](#query-conditions)。传入一个空List将返回所有记录，返回记录数的上限受限于`limit`字段"
					type "List"
					location "query"
					optional false
					since "zsv 0.6"
				}

				column {
					name "limit"
					desc "最多返回的记录数，类似MySQL的limit，默认值1000"
					type "Integer"
					location "query"
					optional true
					since "zsv 0.6"
				}

				column {
					name "start"
					desc "起始查询记录位置，类似MySQL的offset。跟`limit`配合使用可以实现分页"
					type "Integer"
					location "query"
					optional true
					since "zsv 0.6"
				}

				column {
					name "count"
					desc "计数查询，相当于MySQL中的count()函数。当设置成`true`时，API只返回的是满足查询条件的记录数"
					type "Boolean"
					location "query"
					optional true
					since "zsv 0.6"
				}

				column {
					name "groupBy"
					desc "以字段分组，相当于MySQL中的group by关键字。例如groupBy=type"
					type "String"
					location "query"
					optional true
					since "zsv 1.9"
				}

				column {
					name "replyWithCount"
					desc "见上面[分页查询](#query-pagination)"
					type "Boolean"
					location "query"
					optional true
					since "zsv 0.6"
				}

				column {
					name "sortBy"
					desc "以字段排序，等同于MySQL中的sort by关键字，例如sortBy=ip。必须跟`sortDirection`配合使用"
					type "String"
					location "query"
					optional true
					since "zsv 0.6"
				}

				column {
					name "sortDirection"
					desc "字段排序方向，必须跟`sortBy`配合使用"
					type "String"
					location "query"
					optional true
					since "zsv 0.6"
					values ("asc", "desc")
				}

				column {
					name "fields"
					desc "指定返回的字段，等同于MySQL中的select字段功能。例如fields=name,uuid，则只返回满足条件记录的`name`和`uuid`字段"
					type "List"
					location "query"
					optional true
					since "zsv 0.6"
				}
			}
		}
	}
}
