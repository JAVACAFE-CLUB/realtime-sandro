package com.sandro.realtime.harvest.wiki.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "page")
data class WikiPage(
    @field:JacksonXmlProperty(localName = "id")
    val id: Long,

    @field:JacksonXmlProperty(localName = "title")
    val title: String? = null,

    @field:JacksonXmlProperty(localName = "ns")
    val namespace: Int? = null,

    @field:JacksonXmlProperty(localName = "redirect")
    val redirect: WikiRedirect? = null,

    @field:JacksonXmlProperty(localName = "revision")
    val revision: WikiRevision
)