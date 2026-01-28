package org.example.app.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.app.models.WeatherData
import org.example.app.models.TemperatureAnalysis

interface WeatherUseCases {
    fun calculateTemperatureDifference(baseTemp: Double, currentTemp: Double): Double
    fun convertCelsiusToFahrenheit(celsius: Double): Double
    fun convertFahrenheitToCelsius(fahrenheit: Double): Double
    fun calculateAverageTemperature(temperatures: List<Double>): Double
    fun calculateTemperatureTrend(temperatures: List<Double>): Double
    fun observeTemperatureConversions(celsiusTemperatures: Flow<Double>): Flow<Double>
    fun observeTemperatureAnalysis(temperatures: Flow<Double>): Flow<TemperatureAnalysis>
}

class WeatherUseCasesImpl : WeatherUseCases {
    
    override fun observeTemperatureConversions(celsiusTemperatures: Flow<Double>): Flow<Double> = flow {
        celsiusTemperatures.collect { celsius ->
            emit(convertCelsiusToFahrenheit(celsius))
        }
    }
    
    override fun observeTemperatureAnalysis(temperatures: Flow<Double>): Flow<TemperatureAnalysis> = flow {
        val tempList = mutableListOf<Double>()
        temperatures.collect { temp ->
            tempList.add(temp)
            if (tempList.size >= 5) {
                val analysis = TemperatureAnalysis(
                    average = calculateAverageTemperature(tempList),
                    min = tempList.minOrNull() ?: 0.0,
                    max = tempList.maxOrNull() ?: 0.0,
                    trend = calculateTemperatureTrend(tempList.takeLast(10)),
                    recommendations = generateRecommendations(tempList)
                )
                emit(analysis)
            }
        }
    }
    
    override fun calculateTemperatureDifference(baseTemp: Double, currentTemp: Double): Double {
        return currentTemp - baseTemp
    }
    
    override fun convertCelsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9.0 / 5.0) + 32.0
    }
    
    override fun convertFahrenheitToCelsius(fahrenheit: Double): Double {
        return (fahrenheit - 32.0) * 5.0 / 9.0
    }
    
    override fun calculateAverageTemperature(temperatures: List<Double>): Double {
        return if (temperatures.isEmpty()) 0.0 else temperatures.average()
    }
    
    override fun calculateTemperatureTrend(temperatures: List<Double>): Double {
        if (temperatures.size < 2) return 0.0
        
        val firstHalf = temperatures.take(temperatures.size / 2).average()
        val secondHalf = temperatures.drop(temperatures.size / 2).average()
        return secondHalf - firstHalf
    }
    
    private fun generateRecommendations(temperatures: List<Double>): List<String> {
        val recommendations = mutableListOf<String>()
        val avgTemp = temperatures.average()
        
        when {
            avgTemp < 10.0 -> recommendations.add("It's cold! Wear warm clothes.")
            avgTemp < 20.0 -> recommendations.add("Cool weather. Bring a jacket.")
            avgTemp < 30.0 -> recommendations.add("Nice temperature. Perfect for outdoor activities!")
            else -> recommendations.add("It's hot! Stay hydrated and wear light clothes.")
        }
        
        val trend = calculateTemperatureTrend(temperatures.takeLast(5))
        when {
            trend > 2.0 -> recommendations.add("Temperature is rising.")
            trend < -2.0 -> recommendations.add("Temperature is dropping.")
            else -> recommendations.add("Temperature is stable.")
        }
        
        return recommendations
    }
}