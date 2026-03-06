-- CreateIndex
CREATE INDEX "Category_userId_isArchived_idx" ON "Category"("userId", "isArchived");

-- CreateIndex
CREATE INDEX "Transaction_accountId_month_deletedAt_idx" ON "Transaction"("accountId", "month", "deletedAt");

-- CreateIndex
CREATE INDEX "TransactionRequest_fromId_idx" ON "TransactionRequest"("fromId");

-- CreateIndex
CREATE INDEX "TransactionRequest_status_idx" ON "TransactionRequest"("status");
