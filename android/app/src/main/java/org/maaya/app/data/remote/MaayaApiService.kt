package org.maaya.app.data.remote

import org.maaya.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MaayaApiService {

    @POST("/api/incidents")
    suspend fun createIncident(
        @Body request: CreateIncidentRequest
    ): Response<IncidentResponse>

    @POST("/api/incidents/location")
    suspend fun updateLocation(
        @Body request: LocationUpdateRequest
    ): Response<GenericApiResponse>

    @POST("/api/incidents/resolve")
    suspend fun resolveIncident(
        @Body request: ResolveIncidentRequest
    ): Response<GenericApiResponse>

    @POST("/api/saferide/start")
    suspend fun startSafeRide(
        @Body request: SafeRideStartRequest
    ): Response<GenericApiResponse>

    @POST("/api/saferide/checkin")
    suspend fun checkInSafeRide(
        @Body request: SafeRideCheckInRequest
    ): Response<GenericApiResponse>

    @POST("/api/saferide/end")
    suspend fun endSafeRide(
        @Body request: Map<String, String>
    ): Response<GenericApiResponse>
}
