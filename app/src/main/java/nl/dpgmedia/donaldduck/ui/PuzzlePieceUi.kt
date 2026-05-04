package nl.dpgmedia.donaldduck.ui

data class PuzzlePieceUi(
    val id: Int,
    val correctIndex: Int,
    val currentIndex: Int,
    val groupId: Int? = null
)