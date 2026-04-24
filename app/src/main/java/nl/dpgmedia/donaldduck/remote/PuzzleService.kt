package nl.dpgmedia.donaldduck.remote

import kotlinx.coroutines.delay
import nl.dpgmedia.donaldduck.remote.model.EventItem
import nl.dpgmedia.donaldduck.remote.model.PuzzleItem
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import kotlin.random.Random

internal interface PuzzleService {

    @GET("/puzzle")
    suspend fun getPuzzle(): Response<PuzzleItem>

    @PUT("/events")
    suspend fun putEvent(@Body event: EventItem): Response<Unit>
}

internal class FakePuzzleService : PuzzleService {

    // Taken from:
    // https://data.rijksmuseum.nl/oai?verb=GetRecord&metadataPrefix=edm&identifier=https://id.rijksmuseum.nl/200108146
    // Not the best to be used as a vertical puzzle (has an aspect ratio of 6/5), but I quite enjoy the painting :)
    override suspend fun getPuzzle(): Response<PuzzleItem> {
        delay(delayInMs())
        return Response.success(
            PuzzleItem(
                id = 200108146,
                title = "The Threatened Swan",
                description = "A swan fiercely defends its nest against a dog. In later centuries this scuffle was interpreted as a political " +
                    "allegory: the white swan was thought to symbolize the Dutch statesman Johan de Witt (assassinated in 1672) protecting " +
                    "the country from its enemies. This was the meaning attached to the painting when it became the very first acquisition " +
                    "to enter the Nationale Kunstgalerij (the forerunner of the Rijksmuseum) in 1800.",
                author = "Jan Asselijn",
                thumbnail = "https://iiif.micr.io/hZepb/full/max/0/default.webp",
            ),
        )
    }

    override suspend fun putEvent(event: EventItem): Response<Unit> {
        val isEventSuccessful = Random.nextBoolean()
        delay(delayInMs())

        return if (isEventSuccessful) {
            Response.success(Unit)
        } else {
            Response.error(503, ResponseBody.EMPTY)
        }
    }

    private fun delayInMs() = (500L..3000L).random()
}
