package com.sandro.realtime.harvest.wiki.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "revision")
data class WikiRevision(
    @field:JacksonXmlProperty(localName = "id")
    val id: Long,

    @field:JacksonXmlProperty(localName = "parentid")
    val parentId: Long? = null,

    @field:JacksonXmlProperty(localName = "timestamp")
    val timestamp: String? = null,

    @field:JacksonXmlProperty(localName = "contributor")
    val contributor: WikiContributor? = null,

    @field:JacksonXmlProperty(localName = "minor")
    val minor: String? = null,

    @field:JacksonXmlProperty(localName = "comment")
    val comment: String? = null,

    @field:JacksonXmlProperty(localName = "origin")
    val origin: Long? = null,

    @field:JacksonXmlProperty(localName = "model")
    val model: String? = null,

    @field:JacksonXmlProperty(localName = "format")
    val format: String? = null,

    @field:JacksonXmlProperty(localName = "text")
    val text: String? = null
)