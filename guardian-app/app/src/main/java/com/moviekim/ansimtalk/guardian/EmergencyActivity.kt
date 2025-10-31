package com.moviekim.ansimtalk.guardian

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviekim.ansimtalk.guardian.ui.dto.EventType
import com.moviekim.ansimtalk.guardian.ui.emergency.EmergencyScreen
import com.moviekim.ansimtalk.guardian.ui.theme.GuardianappTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyActivity : ComponentActivity() {

    private lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(this, Locale.KOREAN)

        // 1. FCM 및 시스템에서 데이터 추출
        val eventTypeString = intent.getStringExtra("eventType")
        val elderlyName = intent.getStringExtra("elderlyName")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        // eventTypeString을 EventType enum으로 변환. 기본값은 EMERGENCY_CALL
        val eventType = try {
            eventTypeString?.let { EventType.valueOf(it) } ?: EventType.EMERGENCY_CALL
        } catch (e: IllegalArgumentException) {
            Log.e("EmergencyActivity", "Invalid event type: $eventTypeString", e)
            EventType.EMERGENCY_CALL
        }

        // 현재 시간 포맷팅 (예: 23:34)
        val reportTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 위도, 경도를 주소로 변환 (지오코딩)
        val address = getAddress(latitude, longitude)

        enableEdgeToEdge()
        setContent {
            GuardianappTheme {
                // 2. EmergencyScreen에 모든 데이터를 전달
                EmergencyScreen(
                    eventType = eventType,
                    elderlyName = elderlyName ?: "어르신",
                    reportTime = reportTime,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    onDismiss = { finish() },
                    onRoute = {
                        // '길찾기' 기능
                        openNavigation(latitude, longitude)
                    },
                    onCall119 = {
                        // '119 신고' 기능
                        dialPhoneNumber("119")
                    },
                    onCallUser = {
                        // '사용자 연락' 기능
                        // TODO: 실제 사용자(어르신)의 전화번호를 DB 등에서 가져와야 함
                        dialPhoneNumber("") // 현재는 번호가 없으므로 비워둠
                    }
                )
            }
        }
    }

    /**
     * 위도와 경도를 주소 문자열로 변환합니다.
     */
    private fun getAddress(latitude: Double, longitude: Double): String {
        return try {
            // 안드로이드 API 33 (Tiramisu) 부터는 지오코딩을 비동기로 처리해야 합니다.
            // 여기서는 설명을 위해 동기 방식으로 처리하지만, 실제 앱에서는 비동기 처리가 권장됩니다.
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0) // "대한민국 서울특별시 강남구 역삼동 123-45"
            } else {
                "주소를 찾을 수 없습니다."
            }
        } catch (e: Exception) {
            Log.e("EmergencyActivity", "Geocoding failed", e)
            "주소 변환 중 오류 발생"
        }
    }

    /**
     * 특정 번호로 전화를 겁니다.
     */
    private fun dialPhoneNumber(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    /**
     * 내비게이션 앱으로 길찾기를 시작합니다.
     */
    private fun openNavigation(latitude: Double, longitude: Double) {
        if (latitude != 0.0 && longitude != 0.0) {
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Google 지도를 특정하여 실행
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // TODO: Google 지도 앱이 없을 경우 대체 처리 (예: 웹 지도 열기)
            }
        }
    }
}
