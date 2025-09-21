package com.sandro.realtime.harvest.repository

import com.sandro.realtime.harvest.domain.SourceContent
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SourceContentRepository : MongoRepository<SourceContent, String>