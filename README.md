#Wei.IM2A

即时通信框架客户端 Android 版。本实现是基于可靠的 TCP Socket 连接，通过自定义简单协议（约定）与服务端通信。本实现架构合理，并且有一个非常高效的“有效消息实体字节流碎片匹配拆解”算法（详见 com.wei.c.im.core.ByteStreamMatcher.java 和 com.wei.c.im.core.Receiver.java, 已通过极限测试），但本实现不推荐作为非可靠网络或大型项目使用（没有通信协议，不够严谨，会造成消息的丢失，如：接收到消息而未来得及存入数据库的时候结束了进程，而服务端认为已经发送完毕。但是协议可以处理丢失等等的一系列问题如MQTT），仅供学习参考。稍后会推出基于 MQTT 协议的版本。

本框架为运行于 Android Service 进程的抽象基础通信服务框架，需继承并实现 IMService.java 类的抽象方法，并注册为 Android Service 才可运行，同时需要将接收消息的存储模块和发送消息的 Adapter 数据源（类似 ListView 的 Adapter, 你懂的。先询问有没有待发送的数据，有就发，没有就休眠，有新数据要发了就吵醒我）使用 ContentProvider 来实现（已接收到的消息和待发送的消息都需要先持久化，后处理，避免丢失）。具体项目使用请参考 Wei.IM2A.Demo.

#Wei.IM2A.Demo

即时通信框架 Wei.IM2A 应用示例。

但是由于没有配套的服务端正在运行，所有本Demo并不能真正跑起来进行收发消息。只是示范了如何使用。


###欢迎加入我的 [QQ群:215621863](http://shang.qq.com/wpa/qunwpa?idkey=c39a32d6e9036209430732ff38f23729675bf1fac1d3e9faac09ed2165ae6e17 "Android编程&移动互联") 相互学习探讨！