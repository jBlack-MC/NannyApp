package com.nannyapp.ui.navigation

/** Central route table (request #37). Args are appended as path segments. */
object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFY_EMAIL = "verify_email?token={token}"
    fun verifyEmail(token: String? = null) = "verify_email?token=${token ?: ""}"
    const val STATIC_PAGE = "static_page/{pageKey}"
    fun staticPage(pageKey: String) = "static_page/$pageKey"

    // Parent
    const val PARENT_DASHBOARD = "parent_dashboard"
    const val FIND_NANNIES = "find_nannies"
    const val NANNY_DETAIL = "nanny_detail/{nannyId}"
    fun nannyDetail(id: Int) = "nanny_detail/$id"
    const val SAVED_NANNIES = "saved_nannies"
    const val CHILDREN = "children"
    const val CHILD_FORM = "child_form?childId={childId}"
    fun childForm(childId: Int? = null) = "child_form?childId=${childId ?: -1}"
    const val PARENT_BOOKINGS = "parent_bookings"
    const val PARENT_PAYMENTS = "parent_payments"

    // Booking wizard
    const val BOOKING_WIZARD = "booking_wizard/{nannyId}"
    fun bookingWizard(nannyId: Int) = "booking_wizard/$nannyId"
    const val BOOKING_DETAIL = "booking_detail/{bookingId}"
    fun bookingDetail(id: Int) = "booking_detail/$id"
    const val LEAVE_REVIEW = "leave_review/{bookingId}"
    fun leaveReview(bookingId: Int) = "leave_review/$bookingId"

    // Nanny
    const val NANNY_DASHBOARD = "nanny_dashboard"
    const val NANNY_BOOKINGS = "nanny_bookings"
    const val NANNY_AVAILABILITY = "nanny_availability"
    const val NANNY_EARNINGS = "nanny_earnings"
    const val NANNY_REVIEWS = "nanny_reviews"
    const val NANNY_PROFILE_EDIT = "nanny_profile_edit"
    const val NANNY_PORTFOLIO = "nanny_portfolio"
    const val CHECK_IN = "check_in/{bookingId}"
    fun checkIn(bookingId: Int) = "check_in/$bookingId"

    // Shared
    const val MESSAGES = "messages"
    const val CHAT = "chat/{userId}?peerName={peerName}"
    fun chat(userId: Int, peerName: String = "") = "chat/$userId?peerName=${java.net.URLEncoder.encode(peerName, "UTF-8")}"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SUPPORT = "support"
    const val SUPPORT_NEW_TICKET = "support_new_ticket"

    // Admin
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_USER_DETAIL = "admin_user_detail/{userId}"
    fun adminUserDetail(id: Int) = "admin_user_detail/$id"
    const val ADMIN_VERIFICATIONS = "admin_verifications"
    const val ADMIN_VERIFICATION_DETAIL = "admin_verification_detail/{nannyId}"
    fun adminVerificationDetail(id: Int) = "admin_verification_detail/$id"
    const val ADMIN_BOOKINGS = "admin_bookings"
    const val ADMIN_PAYMENTS = "admin_payments"
    const val ADMIN_REPORTS = "admin_reports"
    const val ADMIN_SUPPORT = "admin_support"
}
