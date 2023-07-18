const WebSocket = require('ws');
const fs = require('fs');

const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', function connection(ws) {
    console.log('client connected');

    // 处理接收到的消息
    ws.on('message', function incoming(data) {
        console.log('received text message: %s', data);
        if (typeof data === 'string') {
            console.log('received text message: %s', data);
        } else {
            console.log('received binary message with %d bytes', data.length);

            // 将二进制数据保存为音频文件
            fs.writeFile('audio.wav', data, function (err) {
                if (err) throw err;
                console.log('saved audio file');
            });
        }
    });
    // 发送欢迎消息
    ws.send('Welcome to my WebSocket server!');
});

console.log('WebSocket server started on port 8080');