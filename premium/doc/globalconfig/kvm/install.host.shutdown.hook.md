
## Name

```
install.host.shutdown.hook(关机是否提前清理vm)
```

### Description

```
enable install host shutdown hook
```

### 含义

```
设置为true.会下发一个init.d脚本，在关机的时候会优雅的关闭虚拟机,存在sanlock锁，会清理sanlock 锁
```

### Type

```
java.lang.Boolean
```

### Category

```
kvm
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
false
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
poc的时候，有这个脚本，关机可以优雅关闭虚拟机，同时及时通知mn， 快速拉起
```

### UI暴露

```
需要
```

### CLI手册暴露

```
需要
```

## 注意事项

```
无
```
