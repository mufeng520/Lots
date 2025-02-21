package com.mufeng.lots.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CardDeck(
    @JsonProperty("info")
    val info: InfoSection = InfoSection(),

    @JsonProperty("data")
    val data: ArrayNode = JsonNodeFactory.instance.arrayNode(),

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val dynamicSections: MutableMap<String, ArrayNode> = mutableMapOf()
) {
    data class InfoSection(
        @JsonProperty("title") var title: String = "",
        @JsonProperty("author") var author: String = "",
        @JsonProperty("version") var version: String = "1.0.0",
        @JsonProperty("explain") var explain: String = ""
    )

    data class EditableItem(
        val id: String = UUID.randomUUID().toString(),
        var content: String = ""
    )
}