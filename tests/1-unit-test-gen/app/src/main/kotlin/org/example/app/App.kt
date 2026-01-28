package org.example.app

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt
import org.example.app.di.WeatherModule
import org.example.app.service.WeatherService

fun main() = runBlocking {
    val weatherService = WeatherModule.provideWeatherService()
    
    println("=== Weather Calculator Demo ===")
    println()

    demonstrateRealTimeWeatherAnalysis(weatherService)
    
    weatherService.close()
}


suspend fun demonstrateRealTimeWeatherAnalysis(weatherService: WeatherService) {
    println("--- Real-time Weather Data Analysis ---")
    println("Collecting weather data from multiple cities...")
    println()
    
    val weatherFlow = weatherService.observeWeatherData()
    val analysisFlow = weatherService.observeTemperatureAnalysis(weatherFlow.map { it.temperature })
    
    coroutineScope {
        val weatherJob = launch {
            weatherFlow.collect { weather ->
                println("Weather in ${weather.city}:")
                println("  Temperature: ${weather.temperature}°C")
                println("  Description: ${weather.description}")
                println("  Humidity: ${weather.humidity}%")
                println("  Pressure: ${weather.pressure} hPa")
                println()
            }
        }
        
        val analysisJob = launch {
            analysisFlow.collect { analysis ->
                println("Temperature Analysis:")
                println("  Average: ${analysis.average.roundToInt()}°C")
                println("  Range: ${analysis.min.roundToInt()}°C - ${analysis.max.roundToInt()}°C")
                println("  Trend: ${if (analysis.trend > 0) "↗ Rising" else if (analysis.trend < 0) "↘ Falling" else "→ Stable"}")
                println("  Recommendations:")
                analysis.recommendations.forEach { rec ->
                    println("    - $rec")
                }
                println()
            }
        }
        
        weatherJob.join()
        analysisJob.join()
    }
    
    println("Weather data collection complete!")
}
