package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.*
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: NannyProfile? = null,
    val upcomingBookings: List<Booking> = emptyList(),
    val pendingRequests: List<Booking> = emptyList(),
    val completedCount: Int = 0,
    val earnings: EarningsSummary? = null,
    val unreadNotifications: Int = 0,
    val viewers: com.nannyapp.data.api.dto.ProfileViewersDto? = null,
)

@HiltViewModel
class NannyDashboardViewModel @Inject constructor(
    private val nannyRepository: NannyRepository,
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository,
    private val plusApi: com.nannyapp.data.api.PlusApi,
) : ViewModel() {
    private val _state = MutableStateFlow(NannyDashboardUiState())
    val state: StateFlow<NannyDashboardUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            notificationRepository.unreadCount().collect { _state.value = _state.value.copy(unreadNotifications = it) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            // Independent requests run together rather than one after another.
            val bookingsRequest = async { bookingRepository.getBookings(UserRole.NANNY).first { it !is Resource.Loading } }
            val earningsRequest = async { nannyRepository.getEarnings() }
            // Own profile is fetched by id 0 sentinel resolved server-side to "me" for nanny role.
            val profileRequest = async { nannyRepository.getNannyDetail(0) }
            val viewersRequest = async { (com.nannyapp.util.safeApiCall { plusApi.viewers() } as? Resource.Success)?.data }

            val bookingsResult = bookingsRequest.await()
            val bookings = (bookingsResult as? Resource.Success)?.data.orEmpty()
            val earningsResult = earningsRequest.await()
            val profileResult = profileRequest.await()

            _state.value = _state.value.copy(
                loading = false,
                profile = (profileResult as? Resource.Success)?.data,
                upcomingBookings = bookings.filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.IN_PROGRESS }.sortedBy { it.dateTime },
                pendingRequests = bookings.filter { it.status == BookingStatus.PENDING },
                completedCount = bookings.count { it.status == BookingStatus.COMPLETED },
                earnings = (earningsResult as? Resource.Success)?.data,
                viewers = viewersRequest.await(),
                error = (bookingsResult as? Resource.Error)?.message,
            )
        }
    }
}
