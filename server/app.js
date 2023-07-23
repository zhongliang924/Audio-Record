const { connect } = require("http2");
const path = require("path");
const WebSocket = require("websocket").server;
const wav = require("wav");
const fs = require("fs");
const { client } = require("websocket");

// ����ÿ����Ƶ�ļ��Ĳ����ʡ�ͨ������������ȶ���ͬ
const SAMPLE_RATE = 16000;
const CHANNELS = 1;
const SAMPLE_WIDTH = 2; // 16 bits per sample

let outputStream = null;
let data; // ����ת���ֽ��

function startServer() {
    const server = require("http").createServer((request, response) => {
        response.writeHead(404);
        response.end();
    });

    const { spawn } = require('child_process')

    server.listen(8080, () => {
        console.log("Server is listening on port 8080");
    });

    const wsServer = new WebSocket({
        httpServer: server,
        autoAcceptConnections: true,
    });

    wsServer.on("connect", (connection) => {
        console.log("WebSocket connection accepted, receive audio:");

        outputStream = new wav.FileWriter(path.join(__dirname, "output.wav"), {
            channels: CHANNELS,
            sampleRate: SAMPLE_RATE,
            bitDepth: SAMPLE_WIDTH * 8,
        });

        connection.on("message", (message) => {
            if (message.type === "binary") {
                outputStream.write(message.binaryData);
                connection.sendUTF("Audio receiving.")
            }
            // ¼������
            else {
                // ���տͻ��˷��͵��ַ�����Ϣ
                let str = JSON.stringify(message);
                str = JSON.parse(str).utf8Data;
                console.log(`Received message from client: ${str}`);
                // ���� Python �ӽ��̣���Ƶ�����������
                const pythonProcess = spawn('python', ['speech/client.py'])
                pythonProcess.stdout.on('data', function(res){
                    data = res.toString();
                    console.log('stdout: ', data)
                })
                // ��������д�뵽�ļ�
                pythonProcess.on('close', () => {
                    fs.writeFile('result.txt', data, (err) => {
                        if (err) throw err;
                        console.log("The file has been saved!")
                    });
                });
            }
        });

        connection.on("close", () => {
            console.log("WebSocket connection closed.");
        });
    });
}

startServer();
