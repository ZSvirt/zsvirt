
## Name

```
enable.vm.internal.ip.overwrite(启用云主机内部IP地址读取覆盖平台记录)
```

### Description

```
enable vm internal ip address overwrite db record
```

### 含义

```
启用云主机内部IP地址读取覆盖平台记录功能
```

### Type

```
java.lang.Boolean
```

### Category

```
vm
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
true
```

### 默认值补充说明

```
默认打开, 读取云主机内部IP同时覆盖平台记录
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
不暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
启用云主机内部IP地址读取覆盖平台记录功能, 只有云主机内部IP发生改变后才会触发覆盖, 
开关功能本身不会更新数据
```
