# Audio-Record
Android APP for Audio Record

在这个示例代码中，使用 AudioRecord 类来实现录音功能，同时将录音数据发送到 WebSocket 服务器。需要注意的是，在使用 AudioRecord 时，需要先获取最小缓冲区大小并初始化 AudioRecord 对象。在录音的过程中，将录音数据读取到 ByteArray 缓冲区中，并在回调函数中发送到 WebSocket 服务器。停止录音时需要停止线程并释放相应资源。
