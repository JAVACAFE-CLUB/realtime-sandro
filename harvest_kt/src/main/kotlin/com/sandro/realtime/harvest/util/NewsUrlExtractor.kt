package com.sandro.realtime.harvest.util

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 네이버 뉴스 HTML에서 뉴스 URL을 추출하는 유틸리티 클래스
 */
object NewsUrlExtractor {

    /**
     * HTML 파일에서 네이버 뉴스 URL을 추출합니다.
     *
     * @param htmlFilePath HTML 파일 경로
     * @return 추출된 뉴스 URL 리스트 (중복 제거, 정렬됨)
     */
    fun extractNewsUrls(htmlFilePath: String): List<String> {
        val command =
            """grep -oE 'href="https://n\.news\.naver\.com/article/[0-9]+/[0-9]+"' $htmlFilePath | sed 's/href="//g' | sed 's/"//g' | sort | uniq"""

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("bash", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val urls = reader.readLines()
            process.waitFor()
            reader.close()

            urls
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * URL이 네이버 뉴스 기사 URL 형식인지 검증합니다.
     *
     * @param url 검증할 URL
     * @return 유효한 뉴스 URL인지 여부
     */
    fun isValidNewsUrl(url: String): Boolean {
        val pattern = "^https://n\\.news\\.naver\\.com/article/[0-9]+/[0-9]+$".toRegex()
        return pattern.matches(url)
    }

    /**
     * URL에서 언론사 코드를 추출합니다.
     *
     * @param url 뉴스 URL
     * @return 언론사 코드 (숫자)
     */
    fun extractMediaCode(url: String): String? {
        val pattern = "https://n\\.news\\.naver\\.com/article/([0-9]+)/[0-9]+".toRegex()
        return pattern.find(url)?.groupValues?.get(1)
    }

    /**
     * URL에서 기사 번호를 추출합니다.
     *
     * @param url 뉴스 URL
     * @return 기사 번호
     */
    fun extractArticleId(url: String): String? {
        val pattern = "https://n\\.news\\.naver\\.com/article/[0-9]+/([0-9]+)".toRegex()
        return pattern.find(url)?.groupValues?.get(1)
    }
}