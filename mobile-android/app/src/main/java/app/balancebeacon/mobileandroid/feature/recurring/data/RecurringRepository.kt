package app.balancebeacon.mobileandroid.feature.recurring.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.DeleteRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.UpsertRecurringTemplateRequest

class RecurringRepository(
    private val recurringApi: RecurringApi
) {
    suspend fun getRecurringTemplates(
        accountId: String,
        isActive: Boolean? = null
    ): AppResult<List<RecurringTemplateDto>> {
        return runAppResult {
            recurringApi.getRecurringTemplates(accountId = accountId, isActive = isActive).recurringTemplates
        }
    }

    suspend fun upsertRecurringTemplate(
        request: UpsertRecurringTemplateRequest
    ): AppResult<RecurringTemplateDto> {
        return runAppResult { recurringApi.upsertRecurringTemplate(request = request) }
    }

    suspend fun toggleRecurringTemplate(
        id: String,
        isActive: Boolean
    ): AppResult<ToggleRecurringResponse> {
        return runAppResult {
            recurringApi.toggleRecurringTemplate(id = id, request = ToggleRecurringRequest(isActive = isActive))
        }
    }

    suspend fun deleteRecurringTemplate(
        id: String
    ): AppResult<DeleteRecurringResponse> {
        return runAppResult { recurringApi.deleteRecurringTemplate(id = id) }
    }

    suspend fun applyRecurringTemplates(
        monthKey: String,
        accountId: String? = null,
        templateIds: List<String>? = null
    ): AppResult<ApplyRecurringResponse> {
        return runAppResult {
            recurringApi.applyRecurringTemplates(
                request = ApplyRecurringRequest(
                    monthKey = monthKey,
                    accountId = accountId,
                    templateIds = templateIds
                )
            )
        }
    }
}
