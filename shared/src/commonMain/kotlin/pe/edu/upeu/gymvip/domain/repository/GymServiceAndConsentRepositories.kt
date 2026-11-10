package pe.edu.upeu.gymvip.domain.repository

import pe.edu.upeu.gymvip.domain.model.GymService
import pe.edu.upeu.gymvip.domain.model.MemberConsent
import kotlinx.coroutines.flow.Flow

interface GymServiceRepository {
    fun observeServices(): Flow<List<GymService>>
    suspend fun refreshServices(): Result<List<GymService>>
}

interface LegalConsentRepository {
    fun observeConsent(accountId: String): Flow<MemberConsent?>
    suspend fun fetchConsent(accountId: String): Result<MemberConsent>
    suspend fun submitConsent(accountId: String, termsAccepted: Boolean, privacyAcknowledged: Boolean, whatsappConsent: Boolean): Result<MemberConsent>
}

