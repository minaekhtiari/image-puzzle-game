@file:OptIn(ExperimentalSerializationApi::class)

package nl.dpgmedia.donaldduck.remote.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.random.Random

@Serializable
@JsonClassDiscriminator("eventType")
sealed class EventItem {

    abstract val eventId: Long
    abstract val puzzleId: Long

    @Serializable
    @SerialName("GAME_FINISHED")
    data class GameFinished(
        override val eventId: Long = Random.nextLong(),
        override val puzzleId: Long,
        val durationInMs: Long,
    ) : EventItem()
}
