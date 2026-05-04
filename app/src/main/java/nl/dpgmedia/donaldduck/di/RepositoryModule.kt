package nl.dpgmedia.donaldduck.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.dpgmedia.donaldduck.data.PuzzleRepository
import nl.dpgmedia.donaldduck.data.PuzzleRepositoryImpl
import nl.dpgmedia.donaldduck.domain.CoilPuzzleImageLoader
import nl.dpgmedia.donaldduck.domain.PuzzleImageLoader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPuzzleRepository(
        implementation: PuzzleRepositoryImpl
    ): PuzzleRepository

    @Binds
    @Singleton
    abstract fun bindPuzzleImageLoader(
        implementation: CoilPuzzleImageLoader
    ): PuzzleImageLoader
}