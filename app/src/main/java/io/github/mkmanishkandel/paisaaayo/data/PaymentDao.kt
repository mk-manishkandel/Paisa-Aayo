package io.github.mkmanishkandel.paisaaayo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun since(from: Long): Flow<List<PaymentEntity>>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM payments WHERE direction = 'CREDIT' AND isTest = 0 AND timestamp >= :from")
    fun creditTotalSince(from: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM payments WHERE direction = 'CREDIT' AND isTest = 0 AND timestamp >= :from")
    fun creditCountSince(from: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM payments WHERE reference = :reference AND timestamp >= :since)")
    suspend fun referenceExists(reference: String, since: Long): Boolean

    @Query("SELECT packageName, MAX(sourceLabel) AS sourceLabel FROM payments WHERE isTest = 0 GROUP BY packageName ORDER BY sourceLabel")
    fun sources(): Flow<List<SourceApp>>

    @Query("DELETE FROM payments WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long): Int

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
