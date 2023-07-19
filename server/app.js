const { connect } = require("http2");
const path = require("path");
const WebSocket = require("websocket").server;
const wav = require("wav");
const { client } = require("websocket");

// 假设每个音频文件的采样率、通道数和样本宽度都相同
const SAMPLE_RATE = 16000;
const CHANNELS = 1;
const SAMPLE_WIDTH = 2; // 16 bits per sample

let outputStream = null;

function startServer() {
    const server = require("http").createServer((request, response) => {
        response.writeHead(404);
        response.end();
    });

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
        });

        connection.on("close", () => {
            console.log("WebSocket connection closed.");
            connection.sendUTF("1234564");
        });
    });
}

startServer();
