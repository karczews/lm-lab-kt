package org.example.app.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.app.models.WeatherData
import org.example.app.models.RawWeatherData
import org.example.app.repository.WeatherRepository
import org.example.app.utils.WeatherDataMapper

class ObserveWeatherDataUseCase(
    private val weatherRepository: WeatherRepository,
    private val weatherDataMapper: WeatherDataMapper = WeatherDataMapper()
) {
    
    fun execute(): Flow<WeatherData> = flow {
        val cities = weatherRepository.getSupportedCities()
        
        cities.forEach { city ->
            try {
                val rawData = weatherRepository.fetchWeatherData(city)
                val weatherData = weatherDataMapper.mapToWeatherData(rawData)
                emit(weatherData)
                if (city != cities.last()) {
                    delay(1000)
                }
            } catch (e: Exception) {
                println("Error fetching weather for ${city.name}: ${e.message}")
                val demoRawData = weatherDataMapper.generateDemoRawWeatherData(city.name)
                val demoWeatherData = weatherDataMapper.mapToWeatherData(demoRawData)
                emit(demoWeatherData)
                if (city != cities.last()) {
                    delay(1000)
                }
            }
        }
    }
}