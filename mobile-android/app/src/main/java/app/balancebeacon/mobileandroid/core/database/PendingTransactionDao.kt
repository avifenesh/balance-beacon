package app.balancebeacon.mobileandroid.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingTransactionEntity): Long

    @Query(
        """
        SELECT * FROM pending_transactions
        WHERE attempts < :maxAttempts
        ORDER BY createdAtEpochMs ASC
        LIMIT :limit
        """
    )
    suspend fun listPending(limit: Int = 50, maxAttempts: Int = 3): List<PendingTransactionEntity>

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        UPDATE pending_transactions
        SET attempts = attempts + 1,
            lastError = :error
        WHERE id = :id
        """
    )
    suspend fun markFailed(id: Long, error: String?)
}
