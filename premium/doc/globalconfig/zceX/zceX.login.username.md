
## Name

```
zceX.login.username(##ZCE-X 登录账号名称##)
```

### Description

```
ZCE-X login username, for ZCE-X v5
```

### 含义

```
ZCE-X 登录账号名称，用于ZCE-X v5
```

### Type

```
java.lang.String
```

### Category

```
zceX
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
admin
```

### 默认值补充说明

```
ZCE-X v5 和 v6 的默认登录账号名称一致
```

### 支持的资源级配置

||
|---|
|org.zstack.zcex.entity.ZceXVO

### 资源粒度说明

```
无
```

### 背景信息

```
ZCE-10505

UI 通过用户名和密码来访问 ZCE-X v5 的页面
为了兼容性，现在让 ZCE-X 以 resource config 的名义存储使用的用户名和密码

正常情况下 ZCE-X v6 的 UI 都使用 access-token 来访问, 不需要依赖此配置
（虽然仍然会存储用户名和密码，但是不会使用）
```

### UI暴露

```
否
```

### CLI手册暴露

```
否
```

## 注意事项

```
无
```
