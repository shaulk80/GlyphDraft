package com.glyphdraft.engine

/**
 * A 116-byte ONNX model, `z = x + y` over float tensors of shape [1, 4] (opset 13), written by hand as protobuf.
 * It lets the apps prove that ONNX Runtime and its native library load, without downloading a real model.
 */
internal object TinyModel {
    const val INPUT_X = "x"
    const val INPUT_Y = "y"
    const val OUTPUT = "z"

    private const val HEX =
        "0808120a676c79706864726166743a5e0a130a01780a017912017a1a036164642203416464120874696e795f6164645a130a0178" +
            "120e0a0c080112080a0208010a0208045a130a0179120e0a0c080112080a0208010a02080462130a017a120e0a0c080112080a" +
            "0208010a02080442040a00100d"

    fun bytes(): ByteArray = ByteArray(HEX.length / 2) { HEX.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
