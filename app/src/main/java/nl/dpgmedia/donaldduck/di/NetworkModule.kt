package nl.dpgmedia.donaldduck.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import nl.dpgmedia.donaldduck.data.remote.FakePuzzleService
import nl.dpgmedia.donaldduck.data.remote.PuzzleService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

private const val BASE_URL = "https://www.donaldduck.nl/"
private const val CHUCKER = "CHUCKER_INTERCEPTOR"
private const val LOGGING = "LOGGING_INTERCEPTOR"

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named(CHUCKER) chuckerInterceptor: Interceptor,
        @Named(LOGGING) loggingInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(chuckerInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Named(CHUCKER)
    fun provideChuckerInterceptor(@ApplicationContext context: Context): Interceptor = ChuckerInterceptor(context)

    @Provides
    @Named(LOGGING)
    fun provideLoggingInterceptor(): Interceptor = HttpLoggingInterceptor()
        .apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun providePuzzleService(retrofit: Retrofit): PuzzleService = FakePuzzleService()
}
