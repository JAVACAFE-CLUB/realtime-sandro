package com.sandro.realtime.harvest.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "page")
data class WikiPage(
    @field:JacksonXmlProperty(localName = "title")
    val title: String? = null,

    @field:JacksonXmlProperty(localName = "ns")
    val namespace: Int? = null,

    @field:JacksonXmlProperty(localName = "id")
    val id: Long? = null,

    @field:JacksonXmlProperty(localName = "redirect")
    val redirect: WikiRedirect? = null,

    @field:JacksonXmlProperty(localName = "revision")
    val revision: WikiRevision? = null
)