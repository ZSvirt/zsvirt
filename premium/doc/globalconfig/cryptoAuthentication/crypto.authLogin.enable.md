
## Name

```
crypto.authLogin.enable(是否启用密评身份认证)
```

### Description

```
enable login by CCS certificate authentication
```

### 含义

```
是否启用 CCS 证书 UKey 认证登录
```

### Type

```
java.lang.Boolean
```

### Category

```
cryptoAuthentication
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
true:
表示启用 CCS 证书认证登录, 此时所有绑定 CCS 证书的用户登录就需要使用 UKey 认证了。
对于未绑定 CCS 证书的用户不需要 UKey 的额外验证。
开启该功能后所有已绑定 CCS 证书的用户将强制下线。

false:
表示禁用 CCS 证书认证登录. 设置成禁用将不会将用户强制下线
```

### DefaultValue

```
false
```

### 默认值补充说明

```
不能同时开启 Google 验证器认证、CCS 证书认证。
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
