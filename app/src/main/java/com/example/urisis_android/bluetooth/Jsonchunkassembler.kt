package com.example.urisis_android.bluetooth

/**
 * Reassembles the Arduino's chunked JSON notifications into complete
 * JSON documents.
 *
 * The Arduino splits each JSON payload into 20-byte chunks before
 * notifying (see Bluetooth.cpp::sendJsonData). There is no end-of-message
 * delimiter, so this assembler tracks brace depth:
 *   - First '{' starts a document
 *   - Matching '}' that returns depth to 0 ends it
 *
 * The Arduino's payloads never contain `{` or `}` inside string values
 * (device names, hex colour strings like "#RRGGBB" — all brace-free), so
 * this is safe in practice. A bounded buffer guards against runaway
 * input if the protocol ever desyncs.
 */
internal class JsonChunkAssembler(
    private val onDocument: (String) -> Unit,
    private val onProtocolError: (String) -> Unit = {},
) {
    private val buffer = StringBuilder()
    private var depth = 0
    private var inDocument = false

    fun feed(chunk: ByteArray) {
        feed(chunk.toString(Charsets.UTF_8))
    }

    fun feed(text: String) {
        for (ch in text) {
            when (ch) {
                '{' -> {
                    if (!inDocument) {
                        inDocument = true
                        buffer.clear()
                        depth = 0
                    }
                    depth++
                    buffer.append(ch)
                }
                '}' -> {
                    if (!inDocument) continue
                    depth--
                    buffer.append(ch)
                    if (depth == 0) {
                        val complete = buffer.toString()
                        buffer.clear()
                        inDocument = false
                        onDocument(complete)
                    } else if (depth < 0) {
                        onProtocolError("Negative brace depth — resetting buffer")
                        reset()
                    }
                }
                else -> if (inDocument) buffer.append(ch)
            }
            if (buffer.length > MAX_BUFFER_BYTES) {
                onProtocolError("Buffer exceeded $MAX_BUFFER_BYTES bytes — resetting")
                reset()
            }
        }
    }

    fun reset() {
        buffer.clear()
        depth = 0
        inDocument = false
    }

    companion object {
        // Generous cap. Arduino's JSON_BUFFER_SIZE is 512 and a typical
        // payload from startTest() is around 400 bytes including the
        // optional camera block.
        private const val MAX_BUFFER_BYTES = 4_096
    }
}