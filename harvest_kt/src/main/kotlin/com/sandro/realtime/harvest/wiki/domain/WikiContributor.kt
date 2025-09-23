package com.sandro.realtime.harvest.wiki.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class WikiContributor(
    @field:JacksonXmlProperty(localName = "username")
    val username: String? = null,

    @field:JacksonXmlProperty(localName = "id")
    val id: Long? = null,

    @field:JacksonXmlProperty(localName = "ip")
    val ip: String? = null  // contributor가 익명인 경우 IP 주소
)