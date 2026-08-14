
## Name

```
ldap.entry.search.mode(ldap搜索模式)
```

### Description

```
set ldap preferred search mode
```

### 含义

```
用来设定ldap搜索时使用什么特性来查询
```

### Type

```
java.lang.String
```

### Category

```
ldap
```

### 取值范围

```
{AUTO, NONE, PAGE}
```

### 取值范围补充说明

```
AUTO: 根据ldap服务器支持的特性选择合适的搜索方法，主要是以下几种，生效顺序为PAGE, NONE
NONE: 不使用特殊的特性，直接查询
PAGE: 使用分页查询，要求ldap服务器支持PagedResultControl，OID：1.2.840.113556.1.4.319
```

### DefaultValue

```
AUTO
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
jira: http://jira.zstack.io/browse/ZSTAC-46104
github issue: https://github.com/spring-projects/spring-ldap/issues/80
```

### UI暴露

```
不暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
无
```
