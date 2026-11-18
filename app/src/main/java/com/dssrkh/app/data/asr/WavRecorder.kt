package com.dssrkh.app.data.asr

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/**
 * 16 kHz / 16 bit / 单声道 WAV 录音，最长 120 秒由调用方控制。
 * 边录边写，停止时回填 WAV 头。
 */
class WavRecorder(private val context: Context) {

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private var output: File? = null

    @Volatile
    private var running = false

    fun start(): Boolean {
        if (running) return true
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        val dir = File(context.cacheDir, "asr").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.wav")
        output = file

        recorder.startRecording()
        record = recorder
        running = true

        thread = Thread {
            try {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.write(ByteArray(HEADER_SIZE))
                    raf.seek(HEADER_SIZE.toLong())
                    val buffer = ByteArray(minBuffer)
                    var total = 0
                    while (running) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            raf.write(buffer, 0, read)
                            total += read
                        }
                    }
                    writeHeader(raf, total)
                }
            } catch (_: Exception) {
                // 录音失败由调用方根据文件大小判断
            }
        }.apply { start() }

        return true
    }

    fun stop(): File? {
        if (!running) return output?.takeIf { it.length() > HEADER_SIZE }
        running = false
        runCatching { record?.stop() }
        runCatching { thread?.join(1_500) }
        runCatching { record?.release() }
        record = null
        thread = null
        return output?.takeIf { it.length() > HEADER_SIZE }
    }

    private fun writeHeader(raf: RandomAccessFile, dataSize: Int) {
        raf.seek(0)
        val byteRate = SAMPLE_RATE * 2
        val header = ByteArray(HEADER_SIZE)
        "RIFF".toByteArray().copyInto(header, 0)
        putIntLE(header, 4, 36 + dataSize)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        putIntLE(header, 16, 16)
        putShortLE(header, 20, 1)
        putShortLE(header, 22, 1)
        putIntLE(header, 24, SAMPLE_RATE)
        putIntLE(header, 28, byteRate)
        putShortLE(header, 32, 2)
        putShortLE(header, 34, 16)
        "data".toByteArray().copyInto(header, 36)
        putIntLE(header, 40, dataSize)
        raf.write(header)
    }

    private fun putIntLE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value shr 8) and 0xFF).toByte()
        target[offset + 2] = ((value shr 16) and 0xFF).toByte()
        target[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun putShortLE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val HEADER_SIZE = 44
    }
}
