package nl.dpgmedia.donaldduck.remote.model

import kotlinx.serialization.Serializable

@Serializable
internal data class PuzzleItem(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val thumbnail: String,
)
