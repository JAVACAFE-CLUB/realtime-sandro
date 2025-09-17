package com.sandro.realtime.harvest.batch

import com.sandro.realtime.harvest.domain.WikiPage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.xml.StaxEventItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class WikiPageItemReaderTest : BehaviorSpec() {

    @Autowired
    private lateinit var wikiPageItemReader: StaxEventItemReader<WikiPage>

    override fun extensions() = listOf(SpringExtension)

    init {
        Given("sample-wiki.xml.bz2 파일이 주어졌을 때") {
            When("WikiPageItemReader가 파일을 읽으면") {
                wikiPageItemReader.open(ExecutionContext())

                Then("WikiPage 객체들이 올바르게 파싱된다") {
                    var pageCount = 0

                    var page: WikiPage? = wikiPageItemReader.read()

                    // 파일 끝까지 모든 page를 읽으면서 검증
                    while (page != null) {
                        pageCount++

                        // 각 page의 필수 속성 검증
                        page.title shouldNotBe null
                        page.id shouldNotBe null
                        val revision = page.revision
                        revision shouldNotBe null
                        revision?.id shouldNotBe null
                        revision?.text shouldNotBe null

                        page = wikiPageItemReader.read()
                    }

                    // 최소 1개 이상의 page가 있어야 함
                    pageCount shouldNotBe 0

                    wikiPageItemReader.close()
                }
            }
        }
    }
}