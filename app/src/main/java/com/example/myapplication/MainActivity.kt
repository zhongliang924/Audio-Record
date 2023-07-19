package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString


class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val WS_URL = "ws://192.168.31.241:8080/ws"     // WebSocket 服务器地址

        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO     // 录音通道设置
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT    // 录音格式
        private const val SAMPLE_RATE = 16000 // 采样率
    }

    private var audioRecord: AudioRecord? = null
    private var readThread: Thread? = null

    private var webSocket: WebSocket? = null

    private var recordingStartTime: Long = 0
    private var recordDurationMs = 3000 // 指定录音时长 （毫秒）

    private lateinit var messageTextView: TextView
    private lateinit var startPlayer: MediaPlayer
    private lateinit var stopPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyPermissions(this)
        init()
    }

    /**
     * 初始化
     */
    private fun init() {
        setContentView(R.layout.activity_main)
        val btnRecord = findViewById<Button>(R.id.start)
        val btnStop = findViewById<Button>(R.id.stop)

        messageTextView = findViewById(R.id.messageTextView)

        startPlayer = MediaPlayer.create(this, R.raw.ding)
        stopPlayer = MediaPlayer.create(this, R.raw.dong)

        // 开始录音事件触发
        btnRecord.setOnClickListener {
            startPlayer.start()
            Thread.sleep(1500)      // 添加了 1.5s 延时，这个延时是等待提示音播完
            startRecord()
            connectWebSocket()
        }
        // 停止录音事件出发
        btnStop.setOnClickListener {
            stopRecord()
            stopPlayer.start()
            disconnectWebSocket()
        }
    }

    /**
     * 申请录音权限
     */
    private fun verifyPermissions(activity: Activity?) {
        val GET_RECODE_AUDIO = 1
        val PERMISSION_ALL = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        val permission = ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED
        if (permission) {
            ActivityCompat.requestPermissions(
                activity, PERMISSION_ALL,
                GET_RECODE_AUDIO
            )
        }
    }

    /**
     * 开始录音
     */
    private fun startRecord() {
        Log.d(TAG, "StartRecording:")

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

        recordingStartTime = System.currentTimeMillis()
        readThread = Thread {
            val buffer = ByteArray(minBufferSize)
            audioRecord?.startRecording()
            while (System.currentTimeMillis() - recordingStartTime < recordDurationMs) {
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
    @SuppressLint("SetTextI18n")
    private fun stopRecord() {
        // 等待读取线程完成执行，确保所有数据都已被写入并读取完毕
        try {
            readThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        // 停止录音并释放相关资源
        audioRecord?.stop()
        audioRecord?.release()
        disconnectWebSocket()
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
                // 接收到服务器的消息，更新 UI 显示
                runOnUiThread { messageTextView.setText(text) }
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

    /**
     * 断开 WebSocket 服务器
     */
    private fun disconnectWebSocket() {
        webSocket?.cancel()
    }

}

