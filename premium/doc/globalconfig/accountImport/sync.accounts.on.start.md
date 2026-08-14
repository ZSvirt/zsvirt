
## Name

```
sync.accounts.on.start(启动时同步用户开关)
```

### Description

```
whether execute account synchronization task after system startup
```

### 含义

```
是否在系统启动时自动从第三方用户源同步账户信息；
该配置和 remote.server.auto.sync.enable 配置没有关联。
```

### Type

```
java.lang.Boolean
```

### Category

```
accountImport
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
是否在系统启动时自动从第三方用户源同步账户信息。
如果 true，那么会在系统启动时自动从第三方用户源同步账户信息。
```

### DefaultValue

```
false
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
ThirdPartyUserSourceVO 为第三方用户源
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
