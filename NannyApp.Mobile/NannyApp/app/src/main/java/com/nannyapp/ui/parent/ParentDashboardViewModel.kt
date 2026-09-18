package com.nannyapp.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.*
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: User? = null,
    val profileCompletion: Int = 0,
    val upcomingBooking: Booking? = null,
    val bookingStats: Map<BookingStatus, Int> = emptyMap(),
    val savedNannies: List<SavedNanny> = emptyList(),
    val recentConversations: List<Conversation> = emptyList(),
    val unreadNotifications: Int = 0,
    val featuredNannies: List<NannyProfile> = emptyList(),
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val savedNannyRepository: SavedNannyRepository,
    private val messageRepository: MessageRepository,
    private val notificationRepository: NotificationRepository,
    private val nannyRepository: NannyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentDashboardUiState())
    val state: StateFlow<ParentDashboardUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            notificationRepository.unreadCount().collect { count ->
                _state.value = _state.value.copy(unreadNotifications = count)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            val profileResult = userRepository.getProfile()
            if (profileResult is Resource.Error) {
                _state.value = _state.value.copy(loading = false, error = profileResult.message)
                return@launch
            }
            val user = (profileResult as? Resource.Success)?.data
            if (user == null) {
                _state.value = _state.value.copy(loading = false, error = "Unable to load your profile right now.")
                return@launch
            }
            val completion = userRepository.profileCompletionPercent(user)

            val bookings = bookingRepository.getBookings(UserRole.PARENT).first { it !is Resource.Loading }
            val bookingList = (bookings as? Resource.Success)?.data.orEmpty()
            val upcoming = bookingList
                .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.PENDING }
                .minByOrNull { it.dateTime }
            val stats = bookingList.groupingBy { it.status }.eachCount()

            val saved = savedNannyRepository.getSavedNannies().first { it !is Resource.Loading }
            val savedList = (saved as? Resource.Success)?.data.orEmpty()

            val conversations = messageRepository.getConversations().first { it !is Resource.Loading }
            val conversationList = (conversations as? Resource.Success)?.data.orEmpty()

            val featured = nannyRepository.searchNannies(NannySearchFilters(verifiedOnly = true)).first { it !is Resource.Loading }
            val featuredList = (featured as? Resource.Success)?.data.orEmpty().take(6)

            _state.value = _state.value.copy(
                loading = false,
                user = user,
                profileCompletion = completion,
                upcomingBooking = upcoming,
                bookingStats = stats,
                savedNannies = savedList,
                recentConversations = conversationList.take(3),
                featuredNannies = featuredList,
            )
        }
    }
}
