package org.example.app.di

import org.example.app.repository.WeatherRepository
import org.example.app.repository.WeatherRepositoryImpl
import org.example.app.service.WeatherService


object WeatherModule {
    
    fun provideWeatherRepository(): WeatherRepository {
        return WeatherRepositoryImpl()
    }
    
    fun provideWeatherService(): WeatherService {
        val repository = provideWeatherRepository()
        return WeatherService(repository)
    }
}