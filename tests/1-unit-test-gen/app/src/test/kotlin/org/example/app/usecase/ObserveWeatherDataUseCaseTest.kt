package org.example.app.usecase

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.example.app.models.City
import org.example.app.models.RawWeatherData
import org.example.app.models.WeatherData
import org.example.app.repository.WeatherRepository
import org.example.app.utils.WeatherDataMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ObserveWeatherDataUseCaseTest {

    private lateinit var mockRepository: WeatherRepository
    private lateinit var mapper: WeatherDataMapper
    private lateinit var useCase: ObserveWeatherDataUseCase

    @BeforeEach
    fun setup() {
        mockRepository = Mockito.mock(WeatherRepository::class.java)
        mapper = WeatherDataMapper()
        useCase = ObserveWeatherDataUseCase(mockRepository, mapper)
    }

    @Test
    fun `execute emits weather data for all cities successfully`() = runTest {
        val cities = listOf(
            City("London", 51.5074, -0.1278),
            City("Paris", 48.8566, 2.3522)
        )

        val londonData = RawWeatherData(15.0, 65.0, 1012.0, 0, "London")
        val parisData = RawWeatherData(18.0, 70.0, 1015.0, 2, "Paris")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)
        whenever(mockRepository.fetchWeatherData(cities[1])).thenReturn(parisData)

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals("London", firstResult.city)
            assertEquals(15.0, firstResult.temperature)
            assertEquals("Clear sky", firstResult.description)

            val secondResult = awaitItem()
            assertEquals("Paris", secondResult.city)
            assertEquals(18.0, secondResult.temperature)
            assertEquals("Partly cloudy", secondResult.description)

            awaitComplete()
        }

        verify(mockRepository, times(1)).fetchWeatherData(cities[0])
        verify(mockRepository, times(1)).fetchWeatherData(cities[1])
    }

    @Test
    fun `execute uses demo data when fetchWeatherData throws exception`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(any())).thenThrow(RuntimeException("Network error"))

        useCase.execute().test {
            val result = awaitItem()
            assertEquals("London", result.city)
            assertTrue(result.temperature in 10.0..20.0)
            assertTrue(result.humidity in 40.0..80.0)
            assertTrue(result.pressure in 1000.0..1030.0)

            awaitComplete()
        }

        verify(mockRepository, times(1)).fetchWeatherData(cities[0])
    }

    @Test
    fun `execute handles mixed success and failure scenarios`() = runTest {
        val cities = listOf(
            City("London", 51.5074, -0.1278),
            City("Paris", 48.8566, 2.3522),
            City("Tokyo", 35.6762, 139.6503)
        )

        val londonData = RawWeatherData(15.0, 65.0, 1012.0, 0, "London")
        val tokyoData = RawWeatherData(25.0, 60.0, 1018.0, 1, "Tokyo")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)
        whenever(mockRepository.fetchWeatherData(cities[1])).thenThrow(RuntimeException("Server error"))
        whenever(mockRepository.fetchWeatherData(cities[2])).thenReturn(tokyoData)

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals("London", firstResult.city)
            assertEquals(15.0, firstResult.temperature)

            val secondResult = awaitItem()
            assertEquals("Paris", secondResult.city)
            assertTrue(secondResult.temperature in 13.0..23.0)

            val thirdResult = awaitItem()
            assertEquals("Tokyo", thirdResult.city)
            assertEquals(25.0, thirdResult.temperature)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles empty city list`() = runTest {
        whenever(mockRepository.getSupportedCities()).thenReturn(emptyList())

        useCase.execute().test {
            awaitComplete()
        }

        verify(mockRepository, never()).fetchWeatherData(any())
    }

    @Test
    fun `execute handles all cities failing`() = runTest {
        val cities = listOf(
            City("London", 51.5074, -0.1278),
            City("Paris", 48.8566, 2.3522)
        )

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(any())).thenThrow(RuntimeException("Network error"))

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals("London", firstResult.city)
            assertTrue(firstResult.temperature in 10.0..20.0)

            val secondResult = awaitItem()
            assertEquals("Paris", secondResult.city)
            assertTrue(secondResult.temperature in 13.0..23.0)

            awaitComplete()
        }

        verify(mockRepository, times(2)).fetchWeatherData(any())
    }

    @Test
    fun `execute maps weather codes correctly`() = runTest {
        val cities = listOf(
            City("London", 51.5074, -0.1278),
            City("Paris", 48.8566, 2.3522),
            City("Tokyo", 35.6762, 139.6503)
        )

        val londonData = RawWeatherData(15.0, 65.0, 1012.0, 0, "London")
        val parisData = RawWeatherData(18.0, 70.0, 1015.0, 61, "Paris")
        val tokyoData = RawWeatherData(25.0, 60.0, 1018.0, 95, "Tokyo")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)
        whenever(mockRepository.fetchWeatherData(cities[1])).thenReturn(parisData)
        whenever(mockRepository.fetchWeatherData(cities[2])).thenReturn(tokyoData)

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals("Clear sky", firstResult.description)

            val secondResult = awaitItem()
            assertEquals("Rain", secondResult.description)

            val thirdResult = awaitItem()
            assertEquals("Thunderstorm", thirdResult.description)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles single city`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        val londonData = RawWeatherData(15.0, 65.0, 1012.0, 0, "London")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)

        useCase.execute().test {
            val result = awaitItem()
            assertEquals("London", result.city)
            assertEquals(15.0, result.temperature)

            awaitComplete()
        }
    }

    @Test
    fun `execute processes cities in correct order`() = runTest {
        val cities = listOf(
            City("City1", 0.0, 0.0),
            City("City2", 0.0, 0.0),
            City("City3", 0.0, 0.0)
        )

        val data1 = RawWeatherData(10.0, 60.0, 1010.0, 0, "City1")
        val data2 = RawWeatherData(20.0, 70.0, 1020.0, 0, "City2")
        val data3 = RawWeatherData(30.0, 80.0, 1030.0, 0, "City3")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(data1)
        whenever(mockRepository.fetchWeatherData(cities[1])).thenReturn(data2)
        whenever(mockRepository.fetchWeatherData(cities[2])).thenReturn(data3)

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals(10.0, firstResult.temperature)

            val secondResult = awaitItem()
            assertEquals(20.0, secondResult.temperature)

            val thirdResult = awaitItem()
            assertEquals(30.0, thirdResult.temperature)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles extreme temperature values`() = runTest {
        val cities = listOf(
            City("Antarctica", -90.0, 0.0),
            City("Sahara", 25.0, 13.0)
        )

        val antarcticaData = RawWeatherData(-50.0, 80.0, 980.0, 71, "Antarctica")
        val saharaData = RawWeatherData(50.0, 10.0, 1010.0, 0, "Sahara")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(antarcticaData)
        whenever(mockRepository.fetchWeatherData(cities[1])).thenReturn(saharaData)

        useCase.execute().test {
            val firstResult = awaitItem()
            assertEquals(-50.0, firstResult.temperature)
            assertEquals("Snow", firstResult.description)

            val secondResult = awaitItem()
            assertEquals(50.0, secondResult.temperature)
            assertEquals("Clear sky", secondResult.description)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles unknown weather codes`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        val londonData = RawWeatherData(15.0, 65.0, 1012.0, 999, "London")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)

        useCase.execute().test {
            val result = awaitItem()
            assertEquals("Unknown", result.description)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles negative humidity and pressure outliers`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        val londonData = RawWeatherData(15.0, 90.0, 1040.0, 0, "London")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)

        useCase.execute().test {
            val result = awaitItem()
            assertEquals(90.0, result.humidity)
            assertEquals(1040.0, result.pressure)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles server timeout exception`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(any())).thenThrow(RuntimeException("Timeout"))

        useCase.execute().test {
            val result = awaitItem()
            assertEquals("London", result.city)
            assertTrue(result.temperature in 10.0..20.0)

            awaitComplete()
        }
    }

    @Test
    fun `execute handles null values in raw data`() = runTest {
        val cities = listOf(City("London", 51.5074, -0.1278))

        val londonData = RawWeatherData(Double.NaN, Double.NaN, Double.NaN, 0, "London")

        whenever(mockRepository.getSupportedCities()).thenReturn(cities)
        whenever(mockRepository.fetchWeatherData(cities[0])).thenReturn(londonData)

        useCase.execute().test {
            val result = awaitItem()
            assertEquals("London", result.city)
            assertTrue(result.temperature.isNaN())

            awaitComplete()
        }
    }
}
