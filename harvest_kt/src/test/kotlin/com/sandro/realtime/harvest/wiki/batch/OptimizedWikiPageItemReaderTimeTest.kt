package com.sandro.realtime.harvest.wiki.batch

import com.sandro.realtime.harvest.wiki.domain.WikiPage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.batch.item.ExecutionContext
import java.io.FileInputStream
import kotlin.system.measureTimeMillis

/**
 * 성능을 테스트하기 위한 테스트
 * 이 테스트는 성능 측정용이므로 일반 테스트 실행에서 제외됩니다.
 */
class OptimizedWikiPageItemReaderTimeTest : BehaviorSpec() {

    init {
        xGiven("kowiki-20250820-pages-articles-multistream1.xml-p1p82407.bz2 파일이 주어졌을 때") {
            val filePath = "${System.getProperty("user.home")}/bigdata/kowiki-20250820-pages-articles-multistream1.xml-p1p82407.bz2"
            val reader = OptimizedWikiPageItemReader(FileInputStream(filePath))
            When("WikiPageItemReader가 파일을 읽으면") {
                reader.open(ExecutionContext())

                Then("WikiPage 객체들이 올바르게 파싱되고 성능이 측정된다") {
                    var pageCount = 0
                    var totalReadTime = 0L

                    // 전체 파일 읽기 시간 측정
                    val totalTime = measureTimeMillis {
                        var page: WikiPage? = reader.read()

                        // 파일 끝까지 모든 page를 읽으면서 검증
                        while (page != null) {
                            pageCount++

                            // 다음 page 읽기 시간 측정
                            val readTime = measureTimeMillis {
                                page = reader.read()
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

                    reader.close()
                }
            }
        }
    }
}