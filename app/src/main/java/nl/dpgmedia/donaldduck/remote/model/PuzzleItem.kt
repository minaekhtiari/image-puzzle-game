package nl.dpgmedia.donaldduck.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PuzzleItem(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val thumbnail: String,
)
