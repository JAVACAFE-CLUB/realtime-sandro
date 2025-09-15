package com.sandro.realtime.codex.service

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeOne
import com.sandro.realtime.codex.domain.Document
import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.dto.CreateDocumentRequest
import com.sandro.realtime.codex.dto.DocumentSearchRequest
import com.sandro.realtime.codex.repository.DocumentRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*

class DocumentServiceTest : BehaviorSpec({

    val documentRepository = mockk<DocumentRepository>()
    val documentService = DocumentService(documentRepository)

    val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .plugin(JakartaValidationPlugin())
        .build()

    Given("문서 생성 요청이 주어졌을 때") {
        val request = CreateDocumentRequest(
            documentType = DocumentType.WIKI,
            title = "지미 카터",
            content = "제임스 얼 '지미' 카터 주니어는 미국의 제39대 대통령이다.",
            source = "wikipedia",
            publishedAt = LocalDateTime.now(),
            author = "TedBot",
            categories = listOf("미국의 대통령", "노벨 평화상 수상자"),
            tags = listOf("대통령", "정치인"),
            url = "https://ko.wikipedia.org/wiki/지미_카터",
            metadata = mapOf("pageId" to 5, "namespace" to 0)
        )

        val savedDocument = Document(
            id = "generated-id",
            documentType = request.documentType,
            title = request.title,
            content = request.content,
            source = request.source,
            publishedAt = request.publishedAt,
            author = request.author,
            categories = request.categories,
            tags = request.tags,
            url = request.url,
            metadata = request.metadata
        )

        When("문서를 생성하면") {
            every { documentRepository.save(any()) } returns savedDocument

            val result = documentService.createDocument(request)

            Then("문서가 저장되고 응답이 반환된다") {
                result.id shouldBe "generated-id"
                result.documentType shouldBe DocumentType.WIKI
                result.title shouldBe "지미 카터"
                result.categories shouldHaveSize 2
                result.tags shouldHaveSize 2

                verify(exactly = 1) { documentRepository.save(any()) }
            }
        }
    }

    Given("여러 문서 생성 요청이 주어졌을 때") {
        val wikiRequest = CreateDocumentRequest(
            documentType = DocumentType.WIKI,
            title = "테스트 위키",
            content = "테스트 위키 내용"
        )

        val newsRequest = CreateDocumentRequest(
            documentType = DocumentType.NEWS,
            title = "테스트 뉴스",
            content = "테스트 뉴스 내용"
        )

        val tweetRequest = CreateDocumentRequest(
            documentType = DocumentType.TWEET,
            content = "테스트 트윗 내용"
        )

        val requests = listOf(wikiRequest, newsRequest, tweetRequest)

        val savedDocuments = requests.mapIndexed { index, req ->
            Document(
                id = "id-$index",
                documentType = req.documentType,
                title = req.title,
                content = req.content,
                source = req.source,
                publishedAt = req.publishedAt,
                author = req.author,
                categories = req.categories,
                tags = req.tags,
                url = req.url,
                metadata = req.metadata
            )
        }

        When("배치로 문서를 생성하면") {
            every { documentRepository.saveAll(any<Iterable<Document>>()) } returns savedDocuments

            val result = documentService.createDocumentsBatch(requests)

            Then("모든 문서가 저장되고 응답이 반환된다") {
                result shouldHaveSize 3
                result[0].documentType shouldBe DocumentType.WIKI
                result[1].documentType shouldBe DocumentType.NEWS
                result[2].documentType shouldBe DocumentType.TWEET

                verify(exactly = 1) { documentRepository.saveAll(any<Iterable<Document>>()) }
            }
        }
    }

    Given("문서 조회 요청이 주어졌을 때") {
        val document = fixtureMonkey.giveMeOne<Document>()
        val documentId = "test-id"

        When("존재하는 문서를 조회하면") {
            every { documentRepository.findById(documentId) } returns Optional.of(document)

            val result = documentService.getDocument(documentId)

            Then("문서 정보가 반환된다") {
                result shouldNotBe null
                result?.title shouldBe document.title
                result?.content shouldBe document.content
                result?.documentType shouldBe document.documentType
            }
        }

        When("존재하지 않는 문서를 조회하면") {
            every { documentRepository.findById(documentId) } returns Optional.empty()

            val result = documentService.getDocument(documentId)

            Then("null이 반환된다") {
                result shouldBe null
            }
        }
    }

    Given("문서 검색 요청이 주어졌을 때") {
        val searchRequest = DocumentSearchRequest(
            query = "카터",
            documentType = DocumentType.WIKI,
            category = null,
            page = 0,
            size = 20
        )

        val documents = listOf(
            fixtureMonkey.giveMeOne<Document>(),
            fixtureMonkey.giveMeOne<Document>()
        )

        val pageable = PageRequest.of(0, 20, Sort.by("indexedAt").descending())
        val pageResult = PageImpl(documents, pageable, documents.size.toLong())

        When("문서 타입과 함께 검색하면") {
            every {
                documentRepository.searchByQueryAndDocumentType(
                    searchRequest.query,
                    searchRequest.documentType!!,
                    any()
                )
            } returns pageResult

            val result = documentService.searchDocuments(searchRequest)

            Then("검색 결과가 반환된다") {
                result.content shouldHaveSize 2
                result.totalElements shouldBe 2

                verify(exactly = 1) {
                    documentRepository.searchByQueryAndDocumentType(
                        searchRequest.query,
                        searchRequest.documentType!!,
                        any()
                    )
                }
            }
        }
    }

    Given("문서 타입별 검색 요청이 주어졌을 때") {
        val documentType = DocumentType.NEWS
        val documents = listOf(fixtureMonkey.giveMeOne<Document>())
        val pageable = PageRequest.of(0, 20)
        val pageResult = PageImpl(documents, pageable, 1)

        When("문서 타입으로 검색하면") {
            every {
                documentRepository.findByDocumentType(documentType, any())
            } returns pageResult

            val result = documentService.searchByDocumentType(documentType)

            Then("해당 타입의 문서들이 반환된다") {
                result.content shouldHaveSize 1
                result.totalElements shouldBe 1

                verify(exactly = 1) {
                    documentRepository.findByDocumentType(documentType, any())
                }
            }
        }
    }

    Given("문서 삭제 요청이 주어졌을 때") {
        When("존재하는 문서를 삭제하면") {
            val documentId = "delete-test-id-exists"
            every { documentRepository.existsById(documentId) } returns true
            every { documentRepository.deleteById(documentId) } returns Unit

            val result = documentService.deleteDocument(documentId)

            Then("삭제가 성공한다") {
                result shouldBe true

                verify(exactly = 1) { documentRepository.existsById(documentId) }
                verify(exactly = 1) { documentRepository.deleteById(documentId) }
            }
        }

        When("존재하지 않는 문서를 삭제하려 하면") {
            val documentId = "delete-test-id-not-exists"
            every { documentRepository.existsById(documentId) } returns false

            val result = documentService.deleteDocument(documentId)

            Then("삭제가 실패한다") {
                result shouldBe false

                verify(exactly = 1) { documentRepository.existsById(documentId) }
            }
        }
    }

    Given("전체 문서 개수 조회 요청이 주어졌을 때") {
        When("count를 호출하면") {
            every { documentRepository.count() } returns 100L

            val result = documentService.count()

            Then("전체 문서 개수가 반환된다") {
                result shouldBe 100L

                verify(exactly = 1) { documentRepository.count() }
            }
        }
    }

    Given("문서 타입별 개수 조회 요청이 주어졌을 때") {
        val documentType = DocumentType.TWEET
        val pageResult = PageImpl<Document>(emptyList(), PageRequest.of(0, 1), 42L)

        When("countByDocumentType을 호출하면") {
            every {
                documentRepository.findByDocumentType(documentType, any())
            } returns pageResult

            val result = documentService.countByDocumentType(documentType)

            Then("해당 타입의 문서 개수가 반환된다") {
                result shouldBe 42L

                verify(exactly = 1) {
                    documentRepository.findByDocumentType(documentType, any())
                }
            }
        }
    }
})