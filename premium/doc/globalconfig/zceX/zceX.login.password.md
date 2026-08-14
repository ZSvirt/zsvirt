
## Name

```
zceX.login.password(##ZCE-X 登录账号密码##)
```

### Description

```
ZCE-X login password, for ZCE-X v5
```

### 含义

```
ZCE-X 登录账号密码，用于ZCE-X v5
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
Admin@123
```

### 默认值补充说明

```
注意 ZCE-X v5 的默认登录密码是 password
ZCE-X v6 的默认登录密码是 Admin@123, 这是因为 ZCE-X 在 v6 更新了密码设置规则, 要求有大小写字母、数字、特殊字符的组合
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



因为该配置从 zsv_4.10.20 版本开始支持，如果在之前版本已经初始化接管了 ZCE-X,
那该配置的密码将从全局配置 sds.admin.password 中获取。
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
