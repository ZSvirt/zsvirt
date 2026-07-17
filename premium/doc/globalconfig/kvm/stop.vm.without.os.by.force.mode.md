
## Name

```
stop.vm.without.os.by.force.mode(对没有操作系统的 VM 关闭使用 cold 模式)
```

### Description

```
use 'cold' mode when stop VM without operating system
```

### 含义

```
对没有操作系统的 VM 关闭使用 cold 模式
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
true
```

### 默认值补充说明

```
当这个配置设置为 true 时，如果使用 grace 模式关闭 VM，且 KVM Agent 判断 VM 没有
操作系统，将改用 cold 模式（等同于关闭电源方式）关闭 VM。

在此之前，用 grace 模式关闭无操作系统的 VM 将耗费至少 1 分钟。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
VM 在关闭时，如果没有操作系统，在 grace 模式下没有组件应答 QEMU 发出的 shutdown 指令，
会导致关闭异常缓慢，直至超时。默认情况下，关闭 VM 的超时时间为 1 分钟。

当配置设为 true 时，KVM Agent 通过判断内存的最近更新时间来判断操作系统是否存在。
如果 VM 没有操作系统，那就不会有组件更新 VM 的内存，查询到的内存的最近更新时间
就应该为 0（表示不存在）。当 KVM Agent 发现 VM 不存在操作系统，将改用 cold 模式
关闭 VM，通过标准的虚机销毁流程（destroy 后 undefine）快速关闭。正常销毁流程会
释放存储客户端；但若 destroy 或 undefine 超时而回退到强制 kill 兜底，则不保证
存储客户端被释放。
```

### UI暴露

```
不需要
```

### CLI手册暴露

```
需要
```

## 注意事项

```
无
```
