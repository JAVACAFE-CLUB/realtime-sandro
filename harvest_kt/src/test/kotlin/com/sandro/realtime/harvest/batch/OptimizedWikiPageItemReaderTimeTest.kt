package com.sandro.realtime.harvest.batch

import com.sandro.realtime.harvest.domain.WikiPage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import net.jqwik.api.Disabled
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.system.measureTimeMillis

/**
 * 성능을 테스트하기 위한 테스트
 */
@Disabled
@ActiveProfiles("wiki-dump-test")
@SpringBootTest
class OptimizedWikiPageItemReaderTimeTest : BehaviorSpec() {

    @Autowired
    private lateinit var optimizedWikiPageItemReader: ItemStreamReader<WikiPage>

    override fun extensions() = listOf(SpringExtension)

    init {
        Given(".xml.bz2 파일이 주어졌을 때") {
            When("WikiPageItemReader가 파일을 읽으면") {
                optimizedWikiPageItemReader.open(ExecutionContext())

                Then("WikiPage 객체들이 올바르게 파싱된다") {
                    var pageCount = 0
                    var totalReadTime = 0L

                    // 전체 파일 읽기 시간 측정
                    val totalTime = measureTimeMillis {
                        var page: WikiPage? = optimizedWikiPageItemReader.read()

                        // 파일 끝까지 모든 page를 읽으면서 검증
                        while (page != null) {
                            pageCount++

                            // 다음 page 읽기 시간 측정
                            val readTime = measureTimeMillis {
                                page = optimizedWikiPageItemReader.read()
                            }
                            totalReadTime += readTime
                        }
                    }

                    // 최소 1개 이상의 page가 있어야 함
                    pageCount shouldNotBe 0

                    // 성능 측정 결과 출력
                    println("========== 성능 측정 결과 ==========")
                    println("총 읽은 페이지 수: $pageCount")
                    println("전체 처리 시간: ${totalTime}ms")
                    println("페이지당 평균 처리 시간: ${if (pageCount > 0) totalTime / pageCount else 0}ms")
                    println("순수 읽기 시간 합계: ${totalReadTime}ms")
                    println("검증 및 기타 처리 시간: ${totalTime - totalReadTime}ms")
                    println("===================================")

                    optimizedWikiPageItemReader.close()
                }
            }
        }
    }
}