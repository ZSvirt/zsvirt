
## Name

```
self.fencer.strategy(fencer策略)
```

### Description

```
Self fencer strategy. when set to Permissive, self fencer do not kill the vm when storage fails and also do not ha start the vm to any other host.
        When this strategy set to Force, self fencer do kill the vms and management node will start the vm on suitable host
```

### 含义

```
用于设置云主机高可用触发策略。可选策略为：保守策略（Permissive）、激进策略（Force）。
保守策略（Permissive）：管理节点不会对未知状态的云主机主动触发高可用迁移，且self fencer不会强制停止云主机。
激进策略（Force）：只要满足高可用条件，就可对云主机触发高可用迁移。
```

### Type

```
java.lang.String
```

### Category

```
ha
```

### 取值范围

```
{Force, Permissive}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
Force
```

### 默认值补充说明

```
1 原本默认值为保守策略，为了解决存储故障时kill vm，但是客户认为是云平台自身的问题
2 根据JIRA ZSTAC-56505 的要求默认修改为激进策略，解决前线对HA功能还需要打开的不满
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.primary.PrimaryStorageVO

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
是
```

## 注意事项

```
无
```
