package net.echonolix.slang

enum class SlangCompileTarget(val optionName: String, val fileExtension: String) {
    SPIR_V("spirv", "spv"),
//    GLSL("glsl", "glsl"),
//    HLSL("hlsl", "hlsl"),
}