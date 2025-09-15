package com.sandro.realtime.codex.service

import com.sandro.realtime.codex.domain.Document
import com.sandro.realtime.codex.domain.DocumentType
import com.sandro.realtime.codex.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SampleDataService(
    private val documentRepository: DocumentRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(SampleDataService::class.java)

    override fun run(vararg args: String?) {
        if (documentRepository.count() == 0L) {
            logger.info("샘플 데이터 생성 시작")
            createSampleData()
            logger.info("샘플 데이터 생성 완료")
        } else {
            logger.info("기존 데이터가 존재하여 샘플 데이터 생성을 건너뜁니다")
        }
    }

    private fun createSampleData() {
        val sampleDocuments = listOf(
            // 뉴스 샘플 (HTML 파일 기반)
            Document(
                documentType = DocumentType.NEWS,
                title = "핵잠수함도 샅샅이 찾아낸다…中연구팀 \"AI기술로 95% 추적 성공\"",
                content = """
                    중국의 해상 전력에 가장 큰 위협으로 꼽혀온 저소음 잠수함이 인공지능(AI) 기술의 비약적 발전으로 거의 무력화될 수도 있다는 중국 연구팀의 주장이 나왔다. 
                    15일 홍콩 일간 사우스차이나모닝포스트(SCMP)에 따르면 중국 해군공학대학교 연구팀은 AI 기술을 활용한 새로운 잠수함 탐지 시스템을 개발해 95%의 정확도로 잠수함을 추적하는 데 성공했다고 발표했다.
                    이 기술은 수중 음향 신호를 분석하여 잠수함의 위치를 파악하는 방식으로, 기존의 소나 시스템보다 훨씬 정확하고 빠른 탐지가 가능하다고 연구팀은 설명했다.
                """.trimIndent(),
                source = "디지털타임스",
                publishedAt = LocalDateTime.of(2025, 9, 15, 14, 7),
                author = "디지털타임스",
                categories = listOf("정치", "국제", "군사"),
                tags = listOf("AI", "잠수함", "중국", "군사기술", "탐지시스템"),
                url = "https://n.news.naver.com/article/029/0002982202",
                metadata = mapOf(
                    "articleId" to "0002982202",
                    "officeId" to "029",
                    "sectionId" to "104",
                    "imageUrl" to "https://imgnews.pstatic.net/image/029/2025/09/15/0002982202_001_20250915140712623.png",
                    "summary" to "중국의 해상 전력에 가장 큰 위협으로 꼽혀온 저소음 잠수함이 인공지능(AI) 기술의 비약적 발전으로 거의 무력화될 수도 있다는 중국 연구팀의 주장이 나왔다."
                )
            ),

            // 위키페이지 샘플
            Document(
                documentType = DocumentType.WIKI,
                title = "인공지능",
                content = """
                    인공지능(人工知能, 영어: artificial intelligence, AI)은 기계가 인간의 인지 기능을 모방하는 기술이다. 
                    기계 학습, 딥 러닝, 자연어 처리, 컴퓨터 비전 등의 기술을 포함한다.
                    1956년 다트머스 회의에서 존 매카시가 처음 제안한 용어로, 현재는 4차 산업혁명의 핵심 기술로 여겨진다.
                    
                    주요 분야:
                    - 기계 학습 (Machine Learning)
                    - 딥 러닝 (Deep Learning)  
                    - 자연어 처리 (Natural Language Processing)
                    - 컴퓨터 비전 (Computer Vision)
                    - 로봇공학 (Robotics)
                """.trimIndent(),
                source = "wikipedia",
                author = "Wikipedia 편집자",
                categories = listOf("기술", "컴퓨터과학", "인공지능"),
                tags = listOf("AI", "머신러닝", "딥러닝", "기술", "컴퓨터과학"),
                url = "https://ko.wikipedia.org/wiki/인공지능",
                metadata = mapOf(
                    "pageId" to 12345,
                    "namespace" to 0,
                    "lastModified" to "2025-09-15T10:30:00",
                    "contributors" to 156
                )
            ),

            // 트윗 샘플
            Document(
                documentType = DocumentType.TWEET,
                title = null,
                content = "OpenAI의 새로운 GPT-5 모델이 곧 출시될 예정이라고 합니다! 🤖 이번에는 멀티모달 기능이 대폭 향상되었다고 하네요. #AI #OpenAI #GPT5 #TechNews",
                source = "twitter",
                publishedAt = LocalDateTime.of(2025, 9, 15, 13, 45),
                author = "@tech_insider_kr",
                categories = listOf("기술", "소셜미디어"),
                tags = listOf("OpenAI", "GPT5", "AI", "멀티모달", "기술뉴스"),
                url = "https://twitter.com/tech_insider_kr/status/1234567890",
                metadata = mapOf(
                    "tweetId" to "1234567890",
                    "userId" to "tech_insider_kr",
                    "retweetCount" to 45,
                    "likeCount" to 123,
                    "hashtags" to listOf("AI", "OpenAI", "GPT5", "TechNews"),
                    "mentions" to emptyList<String>()
                )
            ),

            // 추가 뉴스 샘플
            Document(
                documentType = DocumentType.NEWS,
                title = "구글, 차세대 AI 칩 'TPU v6' 발표…성능 3배 향상",
                content = """
                    구글이 차세대 인공지능(AI) 전용 칩인 'TPU v6'를 공개했다고 15일 발표했다.
                    새로운 TPU v6는 기존 v5 대비 3배 향상된 성능을 보이며, 대규모 언어모델 훈련에 최적화되었다.
                    구글은 이 칩을 통해 AI 모델 훈련 비용을 40% 절감할 수 있을 것으로 전망한다고 밝혔다.
                    
                    주요 특징:
                    - 성능: 기존 대비 3배 향상
                    - 전력 효율: 50% 개선
                    - 메모리: 고대역폭 메모리 탑재
                    - 용도: 대규모 언어모델 훈련 최적화
                """.trimIndent(),
                source = "테크크런치",
                publishedAt = LocalDateTime.of(2025, 9, 15, 11, 20),
                author = "김기자",
                categories = listOf("기술", "하드웨어", "AI"),
                tags = listOf("구글", "TPU", "AI칩", "하드웨어", "성능향상"),
                url = "https://techcrunch.com/google-tpu-v6",
                metadata = mapOf(
                    "articleId" to "tc_20250915_001",
                    "section" to "tech",
                    "readTime" to "3분",
                    "imageCount" to 2
                )
            ),

            // 추가 위키페이지 샘플
            Document(
                documentType = DocumentType.WIKI,
                title = "머신러닝",
                content = """
                    머신러닝(기계학습, 영어: machine learning)은 인공지능의 한 분야로, 컴퓨터가 학습할 수 있도록 하는 알고리즘과 기술을 개발하는 분야이다.
                    
                    머신러닝은 크게 세 가지 유형으로 분류된다:
                    
                    1. 지도학습 (Supervised Learning)
                    - 입력과 출력 데이터 쌍이 주어진 상태에서 학습
                    - 분류(Classification)와 회귀(Regression) 문제 해결
                    
                    2. 비지도학습 (Unsupervised Learning)  
                    - 출력 데이터 없이 입력 데이터만으로 학습
                    - 클러스터링, 차원 축소 등에 활용
                    
                    3. 강화학습 (Reinforcement Learning)
                    - 환경과의 상호작용을 통해 보상을 최대화하는 방향으로 학습
                    - 게임, 로봇 제어 등에 응용
                """.trimIndent(),
                source = "wikipedia",
                author = "Wikipedia 편집자",
                categories = listOf("기술", "컴퓨터과학", "인공지능", "머신러닝"),
                tags = listOf("머신러닝", "지도학습", "비지도학습", "강화학습", "알고리즘"),
                url = "https://ko.wikipedia.org/wiki/머신러닝",
                metadata = mapOf(
                    "pageId" to 23456,
                    "namespace" to 0,
                    "lastModified" to "2025-09-14T16:20:00",
                    "contributors" to 89,
                    "length" to 15000
                )
            )
        )

        documentRepository.saveAll(sampleDocuments)
        logger.info("${sampleDocuments.size}개의 샘플 문서가 저장되었습니다")
    }
}