package org.example.app.utils

import org.example.app.models.RawWeatherData
import org.example.app.models.WeatherData

class WeatherDataMapper {
    
    fun mapToWeatherData(rawData: RawWeatherData): WeatherData {
        return WeatherData(
            temperature = rawData.temperature,
            humidity = rawData.humidity,
            pressure = rawData.pressure,
            description = getWeatherDescription(rawData.weatherCode),
            city = rawData.city
        )
    }
    
    fun mapToWeatherDataList(rawDataList: List<RawWeatherData>): List<WeatherData> {
        return rawDataList.map { mapToWeatherData(it) }
    }
    
    fun generateDemoRawWeatherData(city: String): RawWeatherData {
        val baseTemp = when (city) {
            "London" -> 15.0
            "New York" -> 20.0
            "Tokyo" -> 25.0
            "Paris" -> 18.0
            "Sydney" -> 22.0
            else -> 20.0
        }
        
        val variation = (-5..5).random()
        val temperature = baseTemp + variation
        
        return RawWeatherData(
            temperature = temperature,
            humidity = (40..80).random().toDouble(),
            pressure = (1000..1030).random().toDouble(),
            weatherCode = (0..3).random(),
            city = city
        )
    }
    
    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}