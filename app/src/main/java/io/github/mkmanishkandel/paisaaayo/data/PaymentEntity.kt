package io.github.mkmanishkandel.paisaaayo.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One detected payment. Deliberately minimal: the raw notification text is never stored,
 * only what is needed for the statement.
 */
@Entity(
    tableName = "payments",
    indices = [Index("timestamp"), Index("reference"), Index("packageName")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaisa: Long,
    /** "CREDIT" or "DEBIT" — see [io.github.mkmanishkandel.paisaaayo.parser.Direction]. */
    val direction: String,
    val sourceLabel: String,
    val packageName: String,
    val reference: String?,
    val timestamp: Long,
    val isTest: Boolean = false,
)

data class SourceApp(val packageName: String, val sourceLabel: String)
