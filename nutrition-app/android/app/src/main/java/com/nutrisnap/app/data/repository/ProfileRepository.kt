package com.nutrisnap.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nutrisnap.app.data.model.NutritionTargets
import com.nutrisnap.app.data.model.UserProfile
import com.nutrisnap.app.domain.usecase.CalorieTargetCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("profile")

/** Persists the onboarding profile and derived targets in DataStore. */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val json: Json,
    private val calculator: CalorieTargetCalculator,
) {
    private val profileKey = stringPreferencesKey("profile")
    private val targetsKey = stringPreferencesKey("targets")

    val profile: Flow<UserProfile?> = ctx.dataStore.data.map { prefs ->
        prefs[profileKey]?.let { json.decodeFromString<UserProfile>(it) }
    }

    val targets: Flow<NutritionTargets?> = ctx.dataStore.data.map { prefs ->
        prefs[targetsKey]?.let { json.decodeFromString<NutritionTargets>(it) }
    }

    /** Saves the profile and recomputes targets in one shot. */
    suspend fun save(profile: UserProfile) {
        val computed = calculator.calculate(profile)
        ctx.dataStore.edit { prefs ->
            prefs[profileKey] = json.encodeToString(profile)
            prefs[targetsKey] = json.encodeToString(computed)
        }
    }

    suspend fun updateTargets(targets: NutritionTargets) {
        ctx.dataStore.edit { it[targetsKey] = json.encodeToString(targets) }
    }
}
