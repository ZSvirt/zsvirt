
## Name

```
session.grace.period.millis(##ZStone Session 宽恕期时长##)
```

### Description

```
The grace period of ZStone session in millis. During the time after logging into ZStone server, the system will not check the validity of the session. Only valid for ZStone services with 'Remote' authorization server type
```

### 含义

```
ZStone Session 宽恕期时长（毫秒）。
在登录 ZStone 服务器后的一段时间内，系统不会检查会话的有效性。
只对使用 'Remote' 授权服务器类型的 ZStone 服务有效。
```

### Type

```
java.lang.Long
```

### Category

```
zstonePlugin
```

### 取值范围

```
[0, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
30000
```

### 默认值补充说明

```
##对默认值的解读-如无需写：无##
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
