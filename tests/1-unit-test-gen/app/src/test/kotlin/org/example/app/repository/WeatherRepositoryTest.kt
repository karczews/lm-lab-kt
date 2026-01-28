package org.example.app.repository

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.example.app.models.City
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WeatherRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: WeatherRepositoryImpl

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        repository = WeatherRepositoryImpl(baseUrl = mockWebServer.url("/").toString())
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchWeatherData returns correct data for successful response`() = runTest {
        val city = City("London", 51.5074, -0.1278)
        val hourlyTime = (0..23).joinToString(",") { "\"2024-01-27T${it.toString().padStart(2, '0')}:00\"" }
        val hourlyHumidity = (0..23).joinToString(",") { (60 + it).toString() }
        val hourlyPressure = (0..23).joinToString(",") { (1012 + it).toString() }

        val jsonResponse = """
            {
                "current_weather": {
                    "temperature": 15.5,
                    "windspeed": 10.5,
                    "weathercode": 0,
                    "time": "2024-01-27T12:00"
                },
                "hourly": {
                    "time": [$hourlyTime],
                    "relativehumidity_2m": [$hourlyHumidity],
                    "surface_pressure": [$hourlyPressure]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.fetchWeatherData(city)

        assertEquals(15.5, result.temperature)
        assertEquals("London", result.city)
        assertTrue(result.humidity in 60.0..83.0)
        assertTrue(result.pressure in 1012.0..1035.0)
        assertEquals(0, result.weatherCode)
    }

    @Test
    fun `fetchWeatherData throws exception on server error`() = runTest {
        val city = City("Paris", 48.8566, 2.3522)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        assertThrows<Exception> {
            repository.fetchWeatherData(city)
        }
    }

    @Test
    fun `fetchWeatherData throws exception on network error`() = runTest {
        val city = City("Tokyo", 35.6762, 139.6503)

        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START)
        )

        assertThrows<Exception> {
            repository.fetchWeatherData(city)
        }
    }

    @Test
    fun `fetchWeatherData throws exception on malformed JSON`() = runTest {
        val city = City("New York", 40.7128, -74.0060)

        mockWebServer.enqueue(
            MockResponse()
                .setBody("{ invalid json }")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        assertThrows<Exception> {
            repository.fetchWeatherData(city)
        }
    }

    @Test
    fun `fetchWeatherData throws exception on server disconnect`() = runTest {
        val city = City("Sydney", -33.8688, 151.2093)

        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )

        assertThrows<Exception> {
            repository.fetchWeatherData(city)
        }
    }

    @Test
    fun `fetchWeatherData handles missing fields in response`() = runTest {
        val city = City("London", 51.5074, -0.1278)
        val hourlyTime = (0..23).joinToString(",") { "\"2024-01-27T${it.toString().padStart(2, '0')}:00\"" }
        val hourlyHumidity = (0..23).joinToString(",") { "65.0" }
        val hourlyPressure = (0..23).joinToString(",") { "1012.0" }

        val jsonResponse = """
            {
                "current_weather": {
                    "temperature": 15.5,
                    "windspeed": 10.5,
                    "weathercode": 0,
                    "time": "2024-01-27T12:00"
                },
                "hourly": {
                    "time": [$hourlyTime],
                    "relativehumidity_2m": [$hourlyHumidity],
                    "surface_pressure": [$hourlyPressure]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.fetchWeatherData(city)
        assertEquals(15.5, result.temperature)
        assertEquals(0, result.weatherCode)
    }

    @Test
    fun `fetchWeatherData with negative temperature`() = runTest {
        val city = City("London", 51.5074, -0.1278)
        val hourlyTime = (0..23).joinToString(",") { "\"2024-01-27T${it.toString().padStart(2, '0')}:00\"" }
        val hourlyHumidity = (0..23).joinToString(",") { "80.0" }
        val hourlyPressure = (0..23).joinToString(",") { "1015.0" }

        val jsonResponse = """
            {
                "current_weather": {
                    "temperature": -5.0,
                    "windspeed": 10.5,
                    "weathercode": 71,
                    "time": "2024-01-27T12:00"
                },
                "hourly": {
                    "time": [$hourlyTime],
                    "relativehumidity_2m": [$hourlyHumidity],
                    "surface_pressure": [$hourlyPressure]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.fetchWeatherData(city)
        assertEquals(-5.0, result.temperature)
        assertEquals(71, result.weatherCode)
    }

    @Test
    fun `getSupportedCities returns correct list`() {
        val cities = repository.getSupportedCities()

        assertEquals(5, cities.size)
        assertEquals("London", cities[0].name)
        assertEquals("New York", cities[1].name)
        assertEquals("Tokyo", cities[2].name)
        assertEquals("Paris", cities[3].name)
        assertEquals("Sydney", cities[4].name)
    }

    @Test
    fun `getSupportedCities city coordinates are correct`() {
        val cities = repository.getSupportedCities()

        val london = cities.find { it.name == "London" }
        assertEquals(51.5074, london?.latitude)
        assertEquals(-0.1278, london?.longitude)

        val newYork = cities.find { it.name == "New York" }
        assertEquals(40.7128, newYork?.latitude)
        assertEquals(-74.0060, newYork?.longitude)
    }

    @Test
    fun `fetchWeatherData with extreme weather conditions`() = runTest {
        val city = City("Tokyo", 35.6762, 139.6503)
        val hourlyTime = (0..23).joinToString(",") { "\"2024-01-27T${it.toString().padStart(2, '0')}:00\"" }
        val hourlyHumidity = (0..23).joinToString(",") { "95.0" }
        val hourlyPressure = (0..23).joinToString(",") { "980.0" }

        val jsonResponse = """
            {
                "current_weather": {
                    "temperature": 45.0,
                    "windspeed": 50.0,
                    "weathercode": 99,
                    "time": "2024-01-27T12:00"
                },
                "hourly": {
                    "time": [$hourlyTime],
                    "relativehumidity_2m": [$hourlyHumidity],
                    "surface_pressure": [$hourlyPressure]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.fetchWeatherData(city)
        assertEquals(45.0, result.temperature)
        assertEquals(95.0, result.humidity)
        assertEquals(980.0, result.pressure)
        assertEquals(99, result.weatherCode)
    }

    @Test
    fun `fetchWeatherData with 404 Not Found`() = runTest {
        val city = City("London", 51.5074, -0.1278)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )

        assertThrows<Exception> {
            repository.fetchWeatherData(city)
        }
    }
}
