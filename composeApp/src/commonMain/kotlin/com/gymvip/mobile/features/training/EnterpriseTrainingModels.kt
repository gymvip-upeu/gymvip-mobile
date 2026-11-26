package com.gymvip.mobile.features.training

import kotlinx.serialization.Serializable

@Serializable
data class PublishedBranchDto(
    val id: String,
    val organizationId: String,
    val code: String = "",
    val name: String,
    val city: String = "Lima",
    val district: String = "",
    val address: String = "",
    val phone: String? = null,
    val scheduleSummary: String? = null,
    val servicesSummary: String? = null,
    val capacity: Int = 100
)

@Serializable
data class PublishedGymDto(
    val id: String,
    val code: String = "",
    val name: String,
    val description: String? = null,
    val planTier: String = "ENTERPRISE",
    val minPricePen: Double? = null,
    val maxPricePen: Double? = null,
    val branches: List<PublishedBranchDto> = emptyList()
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String = "INTERMEDIO",
    val technique: String,
    val warnings: String? = null
)

@Serializable
data class WorkoutRoutineItemDto(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String = "",
    val equipment: String = "",
    val sortOrder: Int = 1,
    val setsCount: Int = 3,
    val repsTarget: String = "10-12",
    val restSeconds: Int = 90,
    val trainerNotes: String? = null
)

@Serializable
data class WorkoutRoutineDto(
    val id: String,
    val title: String,
    val goal: String,
    val level: String = "INTERMEDIO",
    val durationMinutes: Int = 45,
    val isPersonal: Boolean = false,
    val notes: String? = null,
    val items: List<WorkoutRoutineItemDto> = emptyList()
)

@Serializable
data class WorkoutSetPayload(
    val exerciseId: String? = null,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double
)

@Serializable
data class WorkoutSessionPayload(
    val routineId: String? = null,
    val routineTitle: String,
    val durationSeconds: Int,
    val notes: String? = null,
    val clientSyncId: String,
    val startedAt: String,
    val completedAt: String,
    val sets: List<WorkoutSetPayload>
)

@Serializable
data class WorkoutSessionSummaryDto(
    val id: String,
    val routineTitle: String,
    val durationSeconds: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val completedAt: String = ""
)

@Serializable
data class PersonalRecordDto(
    val exerciseName: String,
    val maxWeightKg: Double = 0.0,
    val maxReps: Int = 0
)

@Serializable
data class WorkoutProgressDto(
    val totalSessions: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val recentSessions: List<WorkoutSessionSummaryDto> = emptyList(),
    val personalRecords: List<PersonalRecordDto> = emptyList()
)

@Serializable
data class GymClassDto(
    val id: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val title: String,
    val category: String,
    val trainerName: String,
    val startsAt: String = "",
    val durationMinutes: Int = 50,
    val capacity: Int = 20,
    val reservedCount: Int = 0,
    val reservedByMe: Boolean = false
)

@Serializable
data class UserPreferencesDto(
    val showGuides: Boolean = true,
    val experienceMode: String = "GUIDED",
    val preferredBranchId: String? = null
)
