package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(userId: String, administrator: Boolean) = repository.load(userId, administrator)
}
