package com.sandro.realtime.config

import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.P6SpyOptions
import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import jakarta.annotation.PostConstruct
import org.hibernate.engine.jdbc.internal.FormatStyle
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class P6SpyFormatter : MessageFormattingStrategy {
    @PostConstruct
    fun setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().logMessageFormat = this::class.java.name
    }

    override fun formatMessage(
        connectionId: Int,
        now: String,
        elapsed: Long,
        category: String,
        prepared: String,
        sql: String,
        url: String,
    ): String {
        val formattedSql = formatSql(category, sql)
        return "[$category] | $elapsed ms | $formattedSql"
    }

    private fun formatSql(
        category: String,
        sql: String,
    ): String {
        if (sql.isNotBlank() && Category.STATEMENT.name == category) {
            val trimmedSQL = sql.trim().lowercase(Locale.ROOT)
            return when {
                trimmedSQL.startsWith("create") ||
                        trimmedSQL.startsWith("alter") ||
                        trimmedSQL.startsWith("comment") ->
                    FormatStyle.DDL.formatter.format(sql)

                else ->
                    FormatStyle.BASIC.formatter.format(sql)
            }
        }
        return sql
    }
}
