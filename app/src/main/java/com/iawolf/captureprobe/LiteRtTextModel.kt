package com.iawolf.captureprobe

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message as LiteMessage
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Experimental REAL offline inference. Only call from a background worker.
 * Does not affect the existing Lab4 voice/capture path.
 */
class LiteRtTextModel(modelFile: File, cacheDir: File) : ConversationCore.LocalTextModel, AutoCloseable {
    private var engine: Engine? = null

    init {
        require(modelFile.isFile && modelFile.length() > 1024 * 1024) {
            "Archivo de modelo inexistente o incompleto"
        }
        require(modelFile.extension.equals("litertlm", ignoreCase = true)) {
            "Se necesita un modelo .litertlm"
        }
        val candidate = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = cacheDir.absolutePath
            )
        )
        // initialize() can take seconds and MUST run off the Android main thread.
        // LiteRT-LM advises not closing an Engine whose initialize() failed.
        runBlocking { candidate.initialize() }
        engine = candidate
    }

    override fun generate(messages: List<ConversationCore.Message>): String {
        val active = checkNotNull(engine) { "Modelo no inicializado" }
        val system = messages.firstOrNull { it.role == "system" }?.content
            ?: error("Falta contexto del sistema")
        val current = messages.lastOrNull { it.role == "user" }?.content
            ?: error("Falta pregunta")
        val previous = messages.drop(1).dropLast(1).mapNotNull { turn ->
            when (turn.role) {
                "user" -> LiteMessage.user(turn.content)
                "assistant" -> LiteMessage.model(turn.content)
                else -> null
            }
        }
        val dialogue = active.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(system),
                initialMessages = previous
            )
        )
        try {
            return runBlocking { dialogue.sendMessage(current).toString() }.trim()
        } finally {
            dialogue.close()
        }
    }

    override fun close() {
        val old = engine ?: return
        engine = null
        old.close()
    }
}
