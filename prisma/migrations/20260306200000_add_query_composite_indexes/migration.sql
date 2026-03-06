-- Add composite indexes for common query patterns

-- Transaction: category-level budget aggregations
CREATE INDEX "Transaction_accountId_categoryId_month_deletedAt_idx" ON "Transaction"("accountId", "categoryId", "month", "deletedAt");

-- ExpenseParticipant: join optimization
CREATE INDEX "ExpenseParticipant_sharedExpenseId_deletedAt_idx" ON "ExpenseParticipant"("sharedExpenseId", "deletedAt");

-- TransactionRequest: pending request queries
CREATE INDEX "TransactionRequest_toId_status_idx" ON "TransactionRequest"("toId", "status");
