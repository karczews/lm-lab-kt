package org.example.app.service

import kotlinx.coroutines.flow.Flow
import org.example.app.models.TemperatureAnalysis
import org.example.app.models.WeatherData
import org.example.app.repository.WeatherRepository
import org.example.app.usecase.ObserveWeatherDataUseCase
import org.example.app.usecase.WeatherUseCases
import org.example.app.usecase.WeatherUseCasesImpl

class WeatherService(
    private val weatherRepository: WeatherRepository
) {
    
    private val weatherUseCases: WeatherUseCases = WeatherUseCasesImpl()
    private val observeWeatherDataUseCase = ObserveWeatherDataUseCase(weatherRepository)
    
    // Calculator methods
    fun observeTemperatureAnalysis(temperatures: Flow<Double>): Flow<TemperatureAnalysis> {
        return weatherUseCases.observeTemperatureAnalysis(temperatures)
    }
    
    fun observeWeatherData(): Flow<WeatherData> {
        return observeWeatherDataUseCase.execute()
    }
    
    suspend fun close() {
        weatherRepository.close()
    }
}
