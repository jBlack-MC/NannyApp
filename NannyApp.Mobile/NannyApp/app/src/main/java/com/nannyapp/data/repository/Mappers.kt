package com.nannyapp.data.repository

import com.nannyapp.data.api.dto.*
import com.nannyapp.data.db.entity.*
import com.nannyapp.domain.model.*

/** Central DTO <-> domain <-> Room entity mapping so this logic lives in one place. */

fun UserDto.asDomain() = User(
    id = id, fullName = fullName, email = email, phone = phone,
    role = UserRole.fromApi(role),
    status = if (status == "suspended") AccountStatus.SUSPENDED else AccountStatus.ACTIVE,
    emailVerified = emailVerified, profileImageUrl = profileImage,
    dateOfBirth = dateOfBirth, address = address, gender = Gender.fromApi(gender), createdAt = createdAt,
)

fun User.toDto() = UserDto(
    id = id, fullName = fullName, email = email, phone = phone, role = role.toApi(),
    status = if (status == AccountStatus.SUSPENDED) "suspended" else "active",
    emailVerified = emailVerified, profileImage = profileImageUrl, dateOfBirth = dateOfBirth,
    address = address, gender = gender.name.lowercase(), createdAt = createdAt,
)

fun User.toEntity() = UserEntity(
    id = id, fullName = fullName, email = email, phone = phone, role = role.toApi(),
    status = status.name.lowercase(), emailVerified = emailVerified, profileImage = profileImageUrl,
    dateOfBirth = dateOfBirth, address = address, gender = gender.name.lowercase(), createdAt = createdAt,
)

fun UserEntity.asDomain() = User(
    id = id, fullName = fullName, email = email, phone = phone,
    role = UserRole.fromApi(role),
    status = if (status == "suspended") AccountStatus.SUSPENDED else AccountStatus.ACTIVE,
    emailVerified = emailVerified, profileImageUrl = profileImage,
    dateOfBirth = dateOfBirth, address = address, gender = Gender.fromApi(gender), createdAt = createdAt,
)

private fun csv(value: String?): List<String> = value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

fun NannyProfileDto.asDomain() = NannyProfile(
    userId = userId, fullName = fullName, profileImageUrl = photoUrl, bannerImageUrl = bannerImage,
    bio = bio, gender = Gender.fromApi(gender), experienceYears = experienceYears, hourlyRate = hourlyRate,
    location = location, skills = csv(skills), languages = csv(languages), qualifications = qualifications,
    specialisations = csv(specialisations), availabilityText = availability,
    verificationStatus = VerificationStatus.fromApi(verificationStatus),
    averageRating = averageRating, reviewCount = reviewCount, profileViews = profileViews, memberSince = memberSince,
)

fun NannyProfileEntity.asDomain() = NannyProfile(
    userId = userId, fullName = fullName, profileImageUrl = photoUrl, bannerImageUrl = bannerImage,
    bio = bio, gender = Gender.fromApi(gender), experienceYears = experienceYears, hourlyRate = hourlyRate,
    location = location, skills = csv(skills), languages = csv(languages), qualifications = qualifications,
    specialisations = csv(specialisations), availabilityText = availability,
    verificationStatus = VerificationStatus.fromApi(verificationStatus),
    averageRating = averageRating, reviewCount = reviewCount, profileViews = profileViews, memberSince = memberSince,
)

fun NannyProfileDto.toEntity() = NannyProfileEntity(
    userId = userId, fullName = fullName, photoUrl = photoUrl, bannerImage = bannerImage, bio = bio,
    gender = gender, experienceYears = experienceYears, hourlyRate = hourlyRate, location = location,
    skills = skills, languages = languages, qualifications = qualifications, specialisations = specialisations,
    availability = availability, verificationStatus = verificationStatus, averageRating = averageRating,
    reviewCount = reviewCount, profileViews = profileViews, memberSince = memberSince,
    lastSyncedAt = System.currentTimeMillis(),
)

fun ChildDto.asDomain() = Child(
    id = id, parentId = parentId, name = name, age = age, gender = gender, allergies = allergies,
    medicalConditions = medicalConditions, specialNeeds = specialNeeds,
    favouriteActivities = favouriteActivities, notesForNannies = notesForNannies,
)

fun Child.toDto() = ChildDto(
    id = id, parentId = parentId, name = name, age = age, gender = gender, allergies = allergies,
    medicalConditions = medicalConditions, specialNeeds = specialNeeds,
    favouriteActivities = favouriteActivities, notesForNannies = notesForNannies,
)

fun ChildDto.toEntity() = ChildEntity(
    id = id, parentId = parentId, name = name, age = age, gender = gender, allergies = allergies,
    medicalConditions = medicalConditions, specialNeeds = specialNeeds,
    favouriteActivities = favouriteActivities, notesForNannies = notesForNannies,
)

fun ChildEntity.asDomain() = Child(
    id = id, parentId = parentId, name = name, age = age, gender = gender, allergies = allergies,
    medicalConditions = medicalConditions, specialNeeds = specialNeeds,
    favouriteActivities = favouriteActivities, notesForNannies = notesForNannies,
)

fun BookingDto.asDomain() = Booking(
    id = id, bookingRef = bookingRef, parentId = parentId, parentName = parentName, nannyId = nannyId,
    nannyName = nannyName, nannyPhotoUrl = nannyPhotoUrl, dateTime = dateTime, durationHours = duration,
    location = location, notes = notes, childrenDetails = childrenDetails, bookingAddress = bookingAddress,
    status = BookingStatus.fromApi(status), hourlyRate = hourlyRate, amount = amount, checkInCode = checkInCode,
    checkInAttempts = checkInAttempts, checkedInAt = checkedInAt, checkedOutAt = checkedOutAt,
    parentConfirmedAt = parentConfirmedAt, disputeReason = disputeReason, disputedAt = disputedAt,
    paymentStatus = PaymentStatus.fromApi(paymentStatus), payoutStatus = PayoutStatus.fromApi(payoutStatus),
    createdAt = createdAt,
)

fun BookingDto.toEntity() = BookingEntity(
    id = id, bookingRef = bookingRef, parentId = parentId, parentName = parentName, nannyId = nannyId,
    nannyName = nannyName, nannyPhotoUrl = nannyPhotoUrl, dateTime = dateTime, duration = duration,
    location = location, notes = notes, childrenDetails = childrenDetails, bookingAddress = bookingAddress,
    status = status, hourlyRate = hourlyRate, amount = amount, checkInCode = checkInCode,
    checkInAttempts = checkInAttempts, checkedInAt = checkedInAt, checkedOutAt = checkedOutAt,
    parentConfirmedAt = parentConfirmedAt, disputeReason = disputeReason, disputedAt = disputedAt,
    paymentStatus = paymentStatus, payoutStatus = payoutStatus, createdAt = createdAt,
)

fun BookingEntity.asDomain() = Booking(
    id = id, bookingRef = bookingRef, parentId = parentId, parentName = parentName, nannyId = nannyId,
    nannyName = nannyName, nannyPhotoUrl = nannyPhotoUrl, dateTime = dateTime, durationHours = duration,
    location = location, notes = notes, childrenDetails = childrenDetails, bookingAddress = bookingAddress,
    status = BookingStatus.fromApi(status), hourlyRate = hourlyRate, amount = amount, checkInCode = checkInCode,
    checkInAttempts = checkInAttempts, checkedInAt = checkedInAt, checkedOutAt = checkedOutAt,
    parentConfirmedAt = parentConfirmedAt, disputeReason = disputeReason, disputedAt = disputedAt,
    paymentStatus = PaymentStatus.fromApi(paymentStatus), payoutStatus = PayoutStatus.fromApi(payoutStatus),
    createdAt = createdAt,
)

fun PaymentDto.asDomain() = Payment(
    id = id, bookingId = bookingId, bookingRef = bookingRef, amount = amount, method = method,
    transactionId = transactionId, status = PaymentStatus.fromApi(status),
    payoutStatus = PayoutStatus.fromApi(payoutStatus), releasedAt = releasedAt, createdAt = createdAt,
)

fun PaymentDto.toEntity() = PaymentEntity(
    id = id, bookingId = bookingId, bookingRef = bookingRef, amount = amount, method = method,
    transactionId = transactionId, status = status, payoutStatus = payoutStatus, releasedAt = releasedAt,
    createdAt = createdAt,
)

fun PaymentEntity.asDomain() = Payment(
    id = id, bookingId = bookingId, bookingRef = bookingRef, amount = amount, method = method,
    transactionId = transactionId, status = PaymentStatus.fromApi(status),
    payoutStatus = PayoutStatus.fromApi(payoutStatus), releasedAt = releasedAt, createdAt = createdAt,
)

fun ReviewDto.asDomain() = Review(
    id = id, bookingId = bookingId, reviewerId = reviewerId, reviewerName = reviewerName, nannyId = nannyId,
    rating = rating, comment = comment, createdAt = createdAt,
)

fun ReviewDto.toEntity() = ReviewEntity(
    id = id, bookingId = bookingId, reviewerId = reviewerId, reviewerName = reviewerName,
    nannyId = nannyId, rating = rating, comment = comment, createdAt = createdAt,
)

fun ReviewEntity.asDomain() = Review(
    id = id, bookingId = bookingId, reviewerId = reviewerId, reviewerName = reviewerName,
    nannyId = nannyId, rating = rating, comment = comment, createdAt = createdAt,
)

fun ChatMessageDto.asDomain(myUserId: Int) = ChatMessage(
    id = id, senderId = senderId, receiverId = receiverId, content = content, isRead = isRead,
    createdAt = createdAt, isMine = senderId == myUserId,
)

fun ChatMessageDto.toEntity() = ChatMessageEntity(
    id = id, senderId = senderId, receiverId = receiverId, content = content, isRead = isRead, createdAt = createdAt,
)

fun ChatMessageEntity.asDomain(myUserId: Int) = ChatMessage(
    id = id, senderId = senderId, receiverId = receiverId, content = content, isRead = isRead,
    createdAt = createdAt, isMine = senderId == myUserId,
)

fun ConversationDto.asDomain() = Conversation(
    withUserId = withUserId, withUserName = withUserName, withUserPhotoUrl = withUserPhotoUrl,
    lastMessage = lastMessage, lastMessageAt = lastMessageAt, unreadCount = unreadCount,
)

fun NotificationDto.asDomain() = AppNotification(
    id = id, title = title, message = message, url = url, isRead = isRead, createdAt = createdAt,
)

fun NotificationDto.toEntity() = NotificationEntity(
    id = id, title = title, message = message, url = url, isRead = isRead, createdAt = createdAt,
)

fun NotificationEntity.asDomain() = AppNotification(
    id = id, title = title, message = message, url = url, isRead = isRead, createdAt = createdAt,
)

fun SavedNannyDto.asDomain() = SavedNanny(id = id, nannyId = nannyId, nanny = nanny?.asDomain(), createdAt = createdAt)

fun SavedNannyDto.toEntity() = SavedNannyEntity(id = id, nannyId = nannyId, createdAt = createdAt)

fun SavedNannyEntity.asDomain() = SavedNanny(id = id, nannyId = nannyId, nanny = null, createdAt = createdAt)

fun DayAvailabilityDto.asDomain() = DayAvailability(
    dayOfWeek = dayOfWeek, isAvailable = isAvailable, timeStart = timeStart, timeEnd = timeEnd,
    slots = slots.mapNotNull { s -> runCatching { AvailabilitySlot.fromApi(s) }.getOrNull() }.toSet(),
)

fun DayAvailability.toDto() = DayAvailabilityDto(
    dayOfWeek = dayOfWeek, isAvailable = isAvailable, timeStart = timeStart, timeEnd = timeEnd,
    slots = slots.map { it.toApi() },
)

fun PortfolioItemDto.asDomain() = PortfolioItem(
    id = id, type = PortfolioType.fromApi(type), title = title, fileUrl = filePath,
    adminVerified = adminVerified, createdAt = createdAt,
)

fun EarningsSummaryDto.asDomain() = EarningsSummary(
    totalEarnings = totalEarnings, heldEarnings = heldEarnings, releasedEarnings = releasedEarnings,
    completedJobs = completedJobs, recentPayments = recentPayments.map { it.asDomain() },
    earningsByDay = earningsByDay.entries.map { it.key to it.value },
)

fun SupportTicketDto.asDomain() = SupportTicket(
    id = id, userId = userId, name = name, email = email, category = SupportCategory.fromApi(category),
    subject = subject, message = message, status = SupportStatus.fromApi(status), adminNotes = adminNotes,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun AdminDashboardStatsDto.asDomain() = AdminDashboardStats(
    totalUsers = totalUsers, totalParents = totalParents, totalNannies = totalNannies,
    verifiedNannies = verifiedNannies, totalBookings = totalBookings, pendingBookings = pendingBookings,
    totalRevenue = totalRevenue, pendingVerifications = pendingVerifications, pendingDocuments = pendingDocuments,
    openSupportTickets = openSupportTickets, recentUsers = recentUsers.map { it.asDomain() },
    recentBookings = recentBookings.map { it.asDomain() },
    topEarningNannies = topEarningNannies.map { it.nanny.asDomain() to it.totalEarnings },
    revenueByDay = revenueByDay.entries.map { it.key to it.value },
    registrationsByDay = registrationsByDay.entries.map { it.key to it.value },
    bookingsByStatus = bookingsByStatus.entries.associate { BookingStatus.fromApi(it.key) to it.value },
    topLocations = topLocations.entries.map { it.key to it.value },
)
