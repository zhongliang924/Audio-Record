package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

class MainActivity : ComponentActivity() {
    companion object {
        private const val WS_URL = "ws://yourserver.com/ws"     // WebSocket 服务器地址

        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;     // 录音通道设置
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;    // 录音格式
        private const val SAMPLE_RATE = 16000 // 采样率
    }

    private var audioRecord: AudioRecord? = null
    private var readThread: Thread? = null
    private var isRecording = false

    private var webSocket: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recoder)

        btn_record.setOnClickListener {
            if (!isRecording){
                // 开始录音
                startRecord()
                // 连接 WebSocket 服务器
                connectWebSocket()
                isRecording = true
            } else {
                // 停止录音
                stopRecord()
                // 断开 WebSocket 连接
                disconnectWebSocket()
                isRecording = false
            }
        }
    }

    /**
     * 开始录音
     */
    private fun startRecord() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize)

        val buffer = ByteArray(minBufferSize)
        audioRecord?.startRecording()

        readThread = Thread {
            while (isRecording) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readBytes > 0 && webSocket != null) {
                    // 发送录音数据到 WebSocket 服务器
                    webSocket?.let {
                        if (!it.send(buffer.toByteString(0, readBytes))){
                            Log.w("WebSocket Info", "send failed")
                        }
                    }
                }
            }
        }
        readThread?.start()
    }

    /**
     * 停止录音
     */
    private fun stopRecord() {
        isRecording = false
        // 等待读取线程完成执行，确保所有数据都已被写入并读取完毕
        try {
            readThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        // 停止录音并释放相关资源
        audioRecord?.stop()
        audioRecord?.release()
    }

    /**
     * 连接 WebSocket 服务器
     */
    private fun connectWebSocket() {
        val request = Request.Builder().url(WS_URL).build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket Info", "onOpen")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket Info", "OnMessage: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket Info", "onClosing")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket Info", "onClosed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket Error", t.message ?: "")
            }
        })
    }

}

