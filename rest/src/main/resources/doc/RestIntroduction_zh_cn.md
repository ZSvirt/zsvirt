# ZStack RESTful API使用手册

从1.9版本，ZStack开始提供原生RESTful支持，以取代早期的HTTP RPC API。本手册详细描述 Restful
API的使用规范，并提供所有API的详细定义。

## 版本

当前API版本为`v1`，所有API的URL以`/v1`开头，例如:
```$xslt
/v1/vm-instances
```

## HTTP方法 (HTTP Verbs)

当前API支持如下方法：

|方法名|描述|
|---|---|
|GET|获取资源信息。所有的Query API以及读API均使用该方法|
|POST|创建一个资源|
|PUT|修改一个资源。所有对资源的修改操作，以及类RPC调用的操作，例如启动虚拟机，均使用该方法|
|DELETE|删除一个资源|

## 参数

URL、Query String、HTTP body三种方式均可用于传参。每种方式可以单独使用，也可以混合使用，
具体使用哪种传参方式由具体API决定。

**URL传参**

当对某具体资源进行操作时，资源的UUID通过编码到URL的方式进行传参，例如启动一个UUID为
*f97143d60f1042c9badd9a1336d3c105*的虚拟机，URL格式为：
```$xslt
/v1/vm-instances/f97143d60f1042c9badd9a1336d3c105/actions
```

这里UUID编码到URL路径当中。

**Query String传参**

所有使用HTTP `GET`方法的API均使用Query String传参，例如查询所有状态为`Running`的虚拟机，
URL格式为：
```$xslt
/v1/vm-instances?condition=state=Running
```

**HTTP Body传参**

当使用`POST`方法创建一个资源，或`PUT`方法修改一个资源时，除通过URL传参的部分外，剩余参数
均通过HTTP Body传参。例如在指定物理机上启动一个虚拟机：

```$xslt
PUT /v1/vm-instances/f97143d60f1042c9badd9a1336d3c105/actions

{
  "startVmInstance": {
        "hostUuid": "8aef7e3a53b34eedaa05027a919156d9"
   }
}
```

这里虚拟机的UUID通过URL传参，参数*hostUuid*则通过HTTP Body传递。

## HTTP Headers

当前API使用如下自定义HTTP Headers：

**Authorization**

除了少数API外（例如登录API），使用ZStack API前都需要一个会话(session)，在调用API时
通过`Authorization` HTTP Header传递会话UUID。该Header的格式为：

```$xslt
Authorization: OAuth 会话UUID
```

举例：
```$xslt
Authorization: OAuth 34cbfddd470a47d8bdb0727cd2182618
```

>注意：OAuth和会话UUID之间用空格分隔

<a name="X-Job-UUID">**X-Job-UUID**</a>

对于[异步API](#async_api)，可以通过`X-Job-UUID` HTTP Header来指定该API Job的UUID，
例如：
```$xslt
X-Job-UUID: d825b1a26f4e474b8c59306081920ff2
```

如果未指定该HTTP Header，ZStack会自动为API Job生成一个UUID。

>注意：X-Job-UUID必须为一个v4版本的UUID（即随机UUID）字符串去掉连接符`-`。ZStack会验证
X-Job-UUID格式的合法性，并对非法的字符串返回一个400 Bad Request的错误。

<a name="X-Web-Hook">**X-Web-Hook**</a>

对于[异步API](#async_api)，可以通过`X-Web-Hook` HTTP Header指定一个回调URL用于接收API
返回。通过使用回调URL的方法，调用者可以避免使用轮询去查询一个异步API的执行结果。举例：

```$xslt
X-Web-Hook: http://localhost:5000/api-callback
```

**X-Job-Success**

当使用了`X-Web-Hook`回调的方式获取异步API结果时，ZStack推送给回调URL的HTTP Post请求中会
包含`X-Job-Success` HTTP Header指明该异步API的执行结果是成功还是失败。例如：

```$xslt
X-Job-Success: true
```

当值为*true*时执行成功，为*false*时执行失败。

## HTTP返回码 (HTTP Status Code)

ZStack使用如下返回码：

#### 200

API执行成功。

#### 202

API请求已被ZStack接受，用户需要通过轮询或Web Hook的方式获取API结果。该返回码只在调用异步API时出现。

#### 400

API请求未包含必要的参数或包含了非法的参数。具体信息可以从HTTP Response Body获得。

#### 404

URL不存在，通常是指定了错误的API URL。
如果访问的URL是异步API返回的轮询地址，表示该轮询地址已经过期。

#### 405

API调用使用了错误的HTTP方法，例如在创建一个资源的时候用了`GET`方法而不是`POST`方法。

#### 500

ZStack RESTful终端遭遇了一个内部错误。

#### 503

API所执行的操作引发了一个错误，例如资源不足无法创建虚拟机。错误的具体信息可以从HTTP Response Body。


## API种类

ZStack的API分为同步API和异步API两种：

#### 同步API

所有使用`GET`方法的API都是同步API，调用方收到的HTTP Response中直接包含了API的结果。例如：

```$xslt
GET /v1/zones/f3fa7671894a40f6a73f5bfc7d90c126

{
	"inventory": {
		"uuid": "f3fa7671894a40f6a73f5bfc7d90c126",
		"name": "zone1",
		"description": "test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 6, 2017 3:51:16 AM",
		"lastOpDate": "Jan 6, 2017 3:51:16 AM"
	}
}

```

#### <a name="async_api">异步API</a>

除了登录相关的API外，所有不使用`GET`方法的API都为异步API。用户调用一个异步API成功后会收到202返回码以及
Body中包含的一个轮询地址（location字段），用户需要周期性的GET该轮询地址以获得API的执行结果。例如：

```$xslt
Status Code: 202
  
Body: 

{
	"location": "http://localhost:8989/v1/api-jobs/967a26b7431c49c0b1d50d709ef1aef3"
}
```

通常情况下GET一个轮询地址可以得到三种返回。202返回码表示该API仍在处理中，用户需要继续轮询；200返回码表示API执行成功，
Body中包含API结果；503返回码表示API执行失败，Body中包含错误码。如果收到404返回码，则表示轮询地址已经过期，产生这种结果的原因
可能是用户访问了一个错误的轮询地址，或者太久没有访问该轮询地址（例如超过2天没有访问），该轮询地址已经被删除。

异步API也可以用Web Hook的方式获得结果，具体方法见后面章节。

## 操作

跟所有的RESTful API类似，绝大多数ZStack API执行的是CRUD(Create, Read, Update, Delete)操作，
以及类RPC操作。

#### 创建资源

所有资源的创建都使用`POST`方法，参数通过HTTP Body传递，例如创建一个虚拟机：

```$xslt
POST /v1/vm-instances

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf

{
	"params": {
		"l3NetworkUuids": ["37a701c7fe4a40758da15593aedd8aff"],
		"defaultL3NetworkUuid": "37a701c7fe4a40758da15593aedd8aff",
		"dataDiskOfferingUuids": [],
		"name": "TestVm",
		"description": "Test",
		"systemTags": [],
		"instanceOfferingUuid": "dd53f94b58924510b0122e40799a4114",
		"type": "UserVm",
		"imageUuid": "cc7b56780879409f98c1f992b75a12b0"
	}
}
```

#### 查询资源

资源的查询使用`GET`方法，查询条件通过Query String传参，例如查询集群*cluster1*中名字**不等于** *web-vm*，
的虚拟机：

```$xslt
GET /v1/vm-instances?condition=name!=web-vm&condition=cluster.name=cluster1

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

如果已知资源的UUID，要直接获取该资源的信息，直接使用`GET`方法不加任何查询条件，例如：

```$xslt
GET /v1/vm-instances/56f0fd314a2647ffb4f9565f6d05858e

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

返回UUID为*56f0fd314a2647ffb4f9565f6d05858e*虚拟机的信息。


#### 删除资源

删除资源使用`DELETE`方法，被删除资源的UUID编码在URL中，例如：

```$xslt
DELETE /v1/vm-instances/56f0fd314a2647ffb4f9565f6d05858e

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

删除UUID为`56f0fd314a2647ffb4f9565f6d05858e`的虚拟机。

#### 修改资源与类PRC操作

但由于IaaS本身业务的性质，一部分操作更类似于RPC(远程调用)而非CRUD操作，例如启动虚拟机。
根据RESTFul API的一些最佳实践，ZStack将这些操作都归为资源的`actions`子资源，例如启动虚拟机、
停止虚拟机都是对虚拟机的`actions`子资源进行操作。举个例子：

启动虚拟机：

```$xslt
PUT /v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions

{
    "startVmInstance": {}
}
```

停止虚拟机：

```$xslt
PUT /v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions

{
    "stopVmInstance": {}
}
```

在上面的例子中，两个操作都访问的是相同的URL */v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions*，
具体的操作类型由包含在Body中的字段名表示，例如`stopVmInstance`，如果该API包含额外参数，则包含在操作字段名对应的
map中。

资源操作的具体字段名和例子参考每个API的详细文档。

## 示例

在下面的例子里面，我们会创建一个Zone，以展示API使用的基本流程：

#### 登录

使用API的第一步是登录以获取一个Session UUID，以供后续API调用使用。

```
PUT /v1/accounts/login

body:

{
	"logIn": {
		"password": "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",
		"accountName": "admin"
	}
}
```

这里的密码是用sha512哈希后的结果。API返回如下：

```
status code: 200

body:

 {
 	"inventory": {
 		"uuid": "00d038b699b74e76a01705918d48d939",
 		"accountUuid": "36c27e8ff05c4780bf6d2fa65700f22e",
 		"userUuid": "36c27e8ff05c4780bf6d2fa65700f22e",
 		"expiredDate": "Jan 1, 2017 11:31:06 AM",
 		"createDate": "Jan 1, 2017 9:31:06 AM"
 	}
 }
```

返回内容中包含账户UUID等其他字段，我们需要的session UUID包含在字段`uuid`中：*00d038b699b74e76a01705918d48d939*。

#### 创建Zone

```
POST /v1/zones

headers:

Authorization: OAuth 00d038b699b74e76a01705918d48d939

body:

{
	"params": {
		"name": "Zone1",
		"description": "Test"
	}
}
```

由于创建Zone操作是一个异步API，API返回不是直接的结果，而是一个轮询地址：

```
status code: 202

body:

{
	"location": "http://localhost:8989/v1/api-jobs/d0345d3ddcae485f8170572b15a2b581"
}
```

用户需要周期性的轮询API结果：

```
GET http://localhost:8989/v1/api-jobs/d0345d3ddcae485f8170572b15a2b581

Authorization: OAuth 00d038b699b74e76a01705918d48d939
```

如果API还未执行完成，上述GET操作得到的仍然是202返回码和上述轮询地址。当操作完成时，得到的结果如下：

```
status code: 200

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```

#### 查询Zone

要获取创建的zone的信息，可以用GET查询：

```
GET /v1/zones/f52fe55b64094ceb99b3893a238c4931

Authorization: OAuth 00d038b699b74e76a01705918d48d939
```

返回：

```
status code: 200

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```


#### 登出

当所有API调用完毕，我们需要对已登录的session进行登出操作：

```
DELETE /v1/accounts/sessions/00d038b699b74e76a01705918d48d939
```

返回

```
status code: 200
```


## Web Hook

对于异步API使用轮询的方式查询操作结果是一种低效的方式，为此ZStack提供Web Hook的方式主动推送异步API
结果给调用者。

要使用Web Hook功能，调用者只需在HTTP headers中指定[X-Job-UUID](#X-Job-UUID)和[X-Web-Hook](#X-Web-Hook)
即可。上面创建Zone的为例，使用Web Hook的API版本为：

```
POST /v1/zones

headers:

Authorization: OAuth 00d038b699b74e76a01705918d48d939
X-Job-UUID: d0345d3ddcae485f8170572b15a2b581
X-Web-Hook: http://127.0.0.1:8989/rest-webhook

body:

{
	"params": {
		"name": "Zone1",
		"description": "Test"
	}
}
```

API返回仍然是202返回码和一个轮询地址，但调用者无需再轮询。API执行成功后，结果会被推送到*http://127.0.0.1:8989/rest-webhook*:

```
POST http://127.0.0.1:8989/rest-webhook

headers:

X-Job-Success: true
X-Job-UUID: d0345d3ddcae485f8170572b15a2b581

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```

推送的结果之中，*X-Job-Success*指明了API执行成功与否，*X-Job-UUID*包含值跟API调用时的*X-Job-UUID*相同，
调用者可以对应结果和API。


## 查询API

用户可以用`GET`方法对一个资源进行查询，并且可以像MySQL一样指定多个查询条件、排序方式、选择字段、以及进行跨表查询等等。
ZStack支持超过400万个单项查询条件，以及400万阶乘的组合查询条件。例如：

```
GET /v1/vm-instances?q=name=vm1
```

查询名字为*vm1*的虚拟机。

```
GET /v1/vm-instances?q=name=vm1&q=state=Running
```

查询名字为*vm1*并且状态为*Running*的虚拟机。这两个例子都是对虚拟机资源本身查询，反应到数据库层面
还属于单表查询。我们可以通过`.`进行跨表查询，例如：

```
GET /v1/vm-instances?q=vmNics.ip=192.168.10.100
```

查询IP地址为*192.168.10.100*的虚拟机，这里对虚拟机和网卡两张表进行了跨表查询。又例如：

```
GET /v1/vm-instances?q=host.managementIp=10.10.20.3
```

查询IP为*10.10.20.3*上运行的所有虚拟机。这里对虚拟机和物理机两张表进行了跨表查询。

所有资源的查询API都支持下列参数：

|名字|类型|位置|描述|可选值|起始版本|
|---|---|---|---|---|---|
|q (可选)|List|query|见[查询条件](#query-conditions)。省略该该字段将返回所有记录，返回记录数的上限受限于`limit`字段||0.6|
|limit (可选)|Integer|query|最多返回的记录数，类似MySQL的limit，默认值1000||0.6|
|start (可选)|Integer|query|起始查询记录位置，类似MySQL的offset。跟`limit`配合使用可以实现分页||0.6|
|count (可选)|Boolean|query|计数查询，相当于MySQL中的count()函数。当设置成`true`时，API只返回的是满足查询条件的记录数||0.6|
|groupBy (可选)|String|query|以字段分组，相当于MySQL中的group by关键字。例如groupBy=type||1.9|
|replyWithCount (可选)|Boolean|query|见上面[分页查询](#query-pagination)||0.6|
|sort (可选)|String|query|以字段排序，等同于MySQL中的sort by关键字，例如sort=+ip。必须跟+或者-配合使用，+表示升序，-表示降序,后面跟排序字段名|<ul><li>+`字段名`</li><li>-`字段名`</li></ul>||0.6|
|sortDirection (可选)|String|query|字段排序方向，必须跟`sortBy`配合使用|<ul><li>asc</li><li>desc</li></ul>|0.6|
|fields (可选)|List|query|指定返回的字段，等同于MySQL中的select字段功能。例如fields=name,uuid，则只返回满足条件记录的`name`和`uuid`字段||0.6|

#### <a name="query-conditions">查询条件</a>

ZStack的查询条件类似于MySQL数据库，例如：

```
uuid=bfa67f956afb430890aa49db14b85153
totalCapacity>2000
vmInstanceUuid not null
```

> **字段名、查询操作符、匹配值三者之间不能有任何空格**。例如`uuid = 25506342d1384c07b7342373a57475b9`就是一个错误的查询条件，必须写为
`uuid=25506342d1384c07b7342373a57475b9`。

多个查询条件之间是**与**关系。总共支持10个查询操作符：

- `=`: 等于，例如：

    ```
    vmInstanceUuid=c4981689088b40f98d2ade2548c323da
    ```
    
- `!=`: 不等于，例如：

    ```
    vmInstanceUuid!=c4981689088b40f98d2ade2548c323da
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

- `is null`: 字段为null：

   ```
   name is null
   ```
   
- `not null`: 字段不为null：

   ```
   name not null
   ```

#### <a name="query-pagination">分页查询</a>

`start`、`limit`、`replyWithCount`三个字段可以配合使用实现分页查询。其中`start`指定其实查询位置，`limit`指定查询返回的最大记录数，而
`replyWithCount`被设置成true后，查询返回中会包含满足查询条件的记录总数，跟`start`值比较就可以得知还需几次分页。

例如总共有1000记录满足查询条件，使用如下组合:

```
start=0 limit=100 replyWithCount=true
```

则API返回将包含头100条记录，以及`total`字段等于1000，表示总共满足条件的记录为1000。

#### 获取资源可查询字段

由于ZStack支持的查询条件数非常巨大，我们无法在文档中枚举所有以查询的条件。用户可以使用我们的命令行工具`zstack-cli`的自动补全功能来查看一个资源可查询的字段以及可跨表查询的字段。
以查询虚拟机为例，在`zstack-cli`里输入`QueryVmInstance`并按Tab键补全，可以看到提示页面：

```
- >>>QueryVmInstance 
[Query Conditions:]
allVolumes.            cluster.               host.                  image.                 instanceOffering.      rootVolume.
vmNics.                zone.                  

__systemTag__=         __userTag__=           allocatorStrategy=     clusterUuid=           cpuNum=                cpuSpeed=
createDate=            defaultL3NetworkUuid=  description=           groupBy=               hostUuid=              hypervisorType=
imageUuid=             instanceOfferingUuid=  lastHostUuid=          lastOpDate=            memorySize=            name=
platform=              rootVolumeUuid=        state=                 type=
uuid=                  zoneUuid=              

[Parameters:]
count=                 fields=                limit=                 replyWithCount=        sortBy=                sortDirection=
start=                 timeout=   
```

这里中间行：

```
__systemTag__=         __userTag__=           allocatorStrategy=     clusterUuid=           cpuNum=                cpuSpeed=
createDate=            defaultL3NetworkUuid=  description=           groupBy=               hostUuid=              hypervisorType=
imageUuid=             instanceOfferingUuid=  lastHostUuid=          lastOpDate=            memorySize=            name=
platform=              rootVolumeUuid=        state=                 type=
uuid=                  zoneUuid=     
```

除`__systemTag__`和`__userTag__`两个特殊查询条件外，其余均为虚拟机表的原生字段，用户可以在API的查询条件里面指定它们，并且可以在`fields`参数中指定这些字段来过滤其它不希望
API返回的字段。例如：

```
GET /v1/vm-instances?q=cpuNum>5
```
返回CPU数量多于5的虚拟机。

```
GET /v1/vm-instances?q=hypervisorType=KVM&fields=uuid&fields=name
```
返回虚拟化类型为KVM的虚拟机，由于在`fields`指定了uuid和name两个字段，API返回中只会包含虚拟机的name和uuid。

>只有资源的原生字段可以被`fields`选取，`__systemTag__`，`__userTag__`以及下面讲到的跨表字段均不能出现在`fields`参数中。

提示的第一行：

```
allVolumes.            cluster.               host.                  image.                 instanceOffering.      rootVolume.
vmNics. 
```

指明了虚拟机资源可以跟哪些资源做跨表查询，例如`allVolumes`代表的云盘，`cluster`代表集群，`vmNics`代表网卡等。如需查看这些资源的具体字段，只需输入资源名加`.`号，并按Tab键补全，例如：

```
- >>>QueryVmInstance vmNics.
[Query Conditions:]
vmNics.eip.                   vmNics.l3Network.             vmNics.loadBalancerListener.  vmNics.portForwarding.        vmNics.securityGroup.
vmNics.vmInstance.            

vmNics.__systemTag__=         vmNics.__userTag__=           vmNics.createDate=            vmNics.deviceId=              vmNics.gateway=
vmNics.ip=                    vmNics.l3NetworkUuid=         vmNics.lastOpDate=            vmNics.mac=                   vmNics.metaData=
vmNics.netmask=               vmNics.uuid=                  vmNics.vmInstanceUuid=  
```

这里我们输入了资源`vmNics`并用`.`号表示我们要做一个跨表查询，Tab键为我们补全了`vmNics`资源的原生字段以及可跨表查询的其它资源。例如这里`vmNics.ip`表示网卡的原生字段`ip`，例如：

```
GET /v1/vm-instances?q=vmNics.ip=192.168.0.100
```

进行了一个跨表查询，条件是网卡表的`ip`字段，返回的结果是`ip`为*192.168.0.100*的虚拟机。网卡资源同样可以跟其它资源进行跨表查询，例如`vmNics.eip.`将网卡表和EIP表进行跨表，如果我们使用：

```
GET /v1/vm-instances?q=vmNics.eip.ip=192.168.0.100
```

则进行了跨3表查询，返回的是EIP为*192.168.0.100*的虚拟机。通过资源间连续跨表，一个资源几乎跟系统中多个有逻辑关系的资源进行跨表，例如：

```
- >>>QueryVmInstance zone.cluster.l2Network.l3Network.
[Query Conditions:]
zone.cluster.l2Network.l3Network.ipRanges.         zone.cluster.l2Network.l3Network.l2Network.        zone.cluster.l2Network.l3Network.networkServices.
zone.cluster.l2Network.l3Network.serviceProvider.  zone.cluster.l2Network.l3Network.vmNic.            zone.cluster.l2Network.l3Network.zone.


zone.cluster.l2Network.l3Network.__systemTag__=    zone.cluster.l2Network.l3Network.__userTag__=      zone.cluster.l2Network.l3Network.createDate=
zone.cluster.l2Network.l3Network.description=      zone.cluster.l2Network.l3Network.dnsDomain=        zone.cluster.l2Network.l3Network.l2NetworkUuid=
zone.cluster.l2Network.l3Network.lastOpDate=       zone.cluster.l2Network.l3Network.name=             zone.cluster.l2Network.l3Network.state=
zone.cluster.l2Network.l3Network.system=           zone.cluster.l2Network.l3Network.type=             zone.cluster.l2Network.l3Network.uuid=
zone.cluster.l2Network.l3Network.zoneUuid= 
```

分别跟zone, cluster, l2Network, l3Network多个资源进行跨表。

>由于一个资源的逻辑关系存在环路，例如以虚拟机为主体可以跟网卡进行跨表，例如`QueryVmInstance vmNics.`，同时以网卡为查询主体也可以跟虚拟机进行跨表`QueryVmNic vmInstance.`，这样就会存环路路径，例如
`QueryVmInstance vmNics.vmInstance.name=vm1`通过跨表查询了name=vm1的虚拟机，它的实际效果跟`QueryVmInstance name=vm1`完全等同。这里的跨表是无意义的，只会生产复杂的SQL语句导致低效的数据库查询。
在使用中应该避免这种环路跨表查询。

`__systemTag__`和`__userTag__`是两个特殊的查询条件，允许用户通过tag查询资源，例如：

```
QueryVmInstance __systemTag__=staticIp:10.10.1.20
```

查询具有*staticIp:10.10.1.20*这个tag的虚拟机。
