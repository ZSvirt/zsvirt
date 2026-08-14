
## Name

```
proxy.tls.version(控制台代理tls版本)
```

### Description

```
The tls version proxy to use
```

### 含义

```
指定控制台代理要使用的tls版本
```

### Type

```
java.lang.String
```

### Category

```
console
```

### 取值范围

```
{NONE, TLSV1_1, TLSV1_2}
```

### 取值范围补充说明

```
NONE表示不指定使用的版本

TLSV1_1表示使用tls1.1

TLSV1_2表示使用tls1.2

因为目前控制台代理运行在Python2.7上 只能支持到tls1.2
```

### DefaultValue

```
NONE
```

### 默认值补充说明

```
无
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
jira: http://jira.zstack.io/browse/ZSTAC-46240
```

### UI暴露

```
暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
设置之后需要重连控制台代理生效
```
