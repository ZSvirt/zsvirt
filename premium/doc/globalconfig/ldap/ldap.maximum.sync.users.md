
## Name

```
ldap.maximum.sync.users(LDAP最大同步用户人数)
```

### Description

```
maximum users sync from ldap server
```

### 含义

```
每次从LDAP服务器中同步的用户最大值
```

### Type

```
java.lang.Integer
```

### Category

```
ldap
```

### 取值范围

```
[1, 2147483647]
```

### 取值范围补充说明

```
不能为 0 和负数
```

### DefaultValue

```
10000
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO

### 资源粒度说明

```
ThirdPartyAccountSourceVO实际上为LDAP服务器
```

### 背景信息

```
无
```

### UI暴露

```
否
```

### CLI手册暴露

```
是
```

## 注意事项

```
无
```
