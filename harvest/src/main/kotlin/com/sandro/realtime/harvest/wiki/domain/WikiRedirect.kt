package com.sandro.realtime.harvest.wiki.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "redirect")
data class WikiRedirect(
    @field:JacksonXmlProperty(isAttribute = true, localName = "title")
    val title: String? = null
)