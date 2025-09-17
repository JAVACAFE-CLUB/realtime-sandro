package com.sandro.realtime.harvest.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "contributor")
data class WikiContributor(
    @field:JacksonXmlProperty(localName = "username")
    val username: String? = null,

    @field:JacksonXmlProperty(localName = "id")
    val id: Long? = null,

    @field:JacksonXmlProperty(localName = "ip")
    val ip: String? = null
)