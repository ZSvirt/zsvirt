
## Name

```
current.ldap.server.uuid(当前正启用的LDAP服务器UUID)
```

### Description

```
The currently enabled ldap server uuid, or NONE indicates that all ldap servers are currently disabled
```

### 含义

```
当前正启用的LDAP服务器UUID，NONE表示所有LDAP服务器当前都禁用
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
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
应当为LdapServerVO.uuid。
如果所有LDAP服务器当前都禁用，该值为NONE
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
无
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
无
```
