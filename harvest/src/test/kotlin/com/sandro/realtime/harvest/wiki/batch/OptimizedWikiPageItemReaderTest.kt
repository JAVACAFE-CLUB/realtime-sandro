package com.sandro.realtime.harvest.wiki.batch

import com.sandro.realtime.harvest.wiki.domain.WikiPage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.batch.item.ExecutionContext
import org.springframework.core.io.ClassPathResource

class OptimizedWikiPageItemReaderTest : BehaviorSpec() {
    init {
        Given("sample-wiki.xml.bz2 파일이 주어졌을 때") {
            val resource = ClassPathResource("sample-wiki.xml.bz2")
            val reader = OptimizedWikiPageItemReader(resource.inputStream)
            When("WikiPageItemReader가 파일을 읽으면") {
                reader.open(ExecutionContext())
                Then("WikiPage 객체들이 올바르게 파싱된다") {
                    var pageCount = 0

                    var page: WikiPage? = reader.read()

                    // 파일 끝까지 모든 page를 읽으면서 검증
                    while (page != null) {
                        pageCount++

                        // 각 page의 필수 속성 검증
                        page.title shouldNotBe null
                        page.id shouldNotBe null
                        val revision = page.revision
                        revision shouldNotBe null
                        revision.id shouldNotBe null
                        revision.text shouldNotBe null

                        page = reader.read()
                    }

                    // 최소 1개 이상의 page가 있어야 함
                    pageCount shouldNotBe 0

                    reader.close()
                }
            }
        }
    }
}