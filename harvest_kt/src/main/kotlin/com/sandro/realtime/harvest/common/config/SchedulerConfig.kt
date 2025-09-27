package com.sandro.realtime.harvest.common.config

import com.sandro.realtime.harvest.news.service.NewsCollectionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@ConditionalOnProperty(
    prefix = "scheduling",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableScheduling
@Configuration
class SchedulerConfig(
    private val newsCollectionService: NewsCollectionService,
) {

    /**
     * 5분마다 뉴스를 수집합니다.
     */
    @Scheduled(fixedDelay = 300000) // 5분 = 300,000ms
    fun collectNews() {
        newsCollectionService.collectNews()
    }
}