package com.hermes.agent.data.llm.formatters

interface PromptFormatter {
    fun format(systemPrompt: String, userPrompt: String): String
}

class Llama3Strategy : PromptFormatter {
    override fun format(systemPrompt: String, userPrompt: String): String {
        return "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" +
               "<|start_header_id|>user<|end_header_id|>\n\n$userPrompt<|eot_id|>" +
               "<|start_header_id|>assistant<|end_header_id|>\n\n"
    }
}
