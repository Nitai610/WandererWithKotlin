package com.nitai.wanderer

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest // NEW: Import the Aggregate Request tool
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectBridge(private val context: Context) {

    private val client: HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null

    interface HealthCallback {
        fun onSuccess(total: Long)
        fun onFailure(errorMessage: String)
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }

    fun readTodaySteps(callback: HealthCallback) {
        if (client == null) {
            callback.onFailure("Health Connect is not installed.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )
                val response = client.readRecords(request)
                var totalSteps = 0L
                for (record in response.records) {
                    totalSteps += record.count
                }
                CoroutineScope(Dispatchers.Main).launch { callback.onSuccess(totalSteps) }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch { callback.onFailure(e.message ?: "Error") }
            }
        }
    }

    // =========================================================
    // FIX: Updated to use AggregateRequest for Calories
    // =========================================================
    fun readTodayCalories(callback: HealthCallback) {
        if (client == null) {
            callback.onFailure("Health Connect is not installed.")
            return
        }

        // NOTE FOR BAGRUT: Why use AggregateRequest instead of ReadRecordsRequest?
        // "Google Fit calculates Total Calories by adding Active Burned Calories + Resting BMR Calories.
        // Because it's a combined mathematical formula, reading the raw records returns 0.
        // I have to use the Aggregate API to ask Google's servers to calculate the final combined sum for me."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS)

                // We ask Health Connect specifically for the ENERGY_TOTAL aggregate metric
                val request = AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )

                val response = client.aggregate(request)

                // Extract the total from the response. If it's null (e.g. no data yet), default to 0.0
                val totalCalories = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

                CoroutineScope(Dispatchers.Main).launch { callback.onSuccess(totalCalories.toLong()) }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch { callback.onFailure(e.message ?: "Error") }
            }
        }
    }
}