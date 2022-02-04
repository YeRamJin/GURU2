@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.example.guru2

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperColors.fromBitmap
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.guru2.R.id.layout_main
import com.example.guru2.R.id.map
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import noman.googleplaces.*
import noman.googleplaces.PlacesException
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    OnRequestPermissionsResultCallback, PlacesListener, GoogleMap.OnInfoWindowClickListener{
//    , GoogleMap.OnMarkerClickListener {     --> 마커 클릭이벤트 함수 오류발생으로 주석 처리
    private var mMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    var needRequest = false

    // 앱을 실행하기 위해 필요한 퍼미션을 정의합니다.
    var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 외부 저장소
    var mCurrentLocatiion: Location? = null
    var currentPosition: LatLng? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null
    private var mLayout // Snackbar 사용하기 위해서는 View가 필요합니다.
            : View? = null

    //음식점 표시
    var previous_marker: MutableList<Marker>? = null

    //북마크 팝업창을 위해
    private var dilaog01: AlertDialog? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        previous_marker = ArrayList<Marker>()
        val button: ImageButton = findViewById<View>(R.id.imageButton) as ImageButton
        button.setOnClickListener(View.OnClickListener { showPlaceInformation(currentPosition) })

        mLayout = findViewById(layout_main)
        locationRequest = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS.toLong())
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS.toLong())
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment: SupportMapFragment? = supportFragmentManager
            .findFragmentById(map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        //북마크 팝업창
        dilaog01?.requestWindowFeature(Window.FEATURE_NO_TITLE) // 타이틀 제거
        dilaog01?.setContentView(R.layout.dilaog01) //XML 파일과 연결

        //말풍선 클릭시 북마크 팝업창 띄우기
//        findViewById<ImageButton>(R.id.bookmarkBtn).setOnClickListener(View.OnClickListener {
//            fun OnClick(view: View) {
//                showDialog01();
//            }
//        })
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        Log.d(TAG, "onMapReady :")
        mMap = googleMap

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation()

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            startLocationUpdates() // 3. 위치 업데이트 시작
        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    REQUIRED_PERMISSIONS[0]
                )
            ) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(
                    mLayout!!, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("확인") { // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions(
                            this@MainActivity, REQUIRED_PERMISSIONS,
                            PERMISSIONS_REQUEST_CODE
                        )
                    }.show()
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
        mMap?.getUiSettings()?.setMyLocationButtonEnabled(true)
        // 현재 오동작을 해서 주석처리

        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mMap?.setOnMapClickListener(object : GoogleMap.OnMapClickListener {
            override fun onMapClick(latLng: LatLng?) {
                Log.d(TAG, "onMapClick :")
            }
        })

        mMap?.setOnInfoWindowClickListener(this)

        //마커 클릭 이벤트를 위해 생성 -> 함수는 밑에 있음 -> 오류로 주석처리
//        mMap?.setOnMarkerClickListener(this)
    }
        //서울 태그하는 코드
//        val SEOUL = LatLng(37.56, 126.97)
//        val markerOptions = MarkerOptions()
//        markerOptions.position(SEOUL)
//        markerOptions.title("서울")
//        markerOptions.snippet("한국의 수도")
//        mMap?.addMarker(markerOptions)
//
//
//        // 기존에 사용하던 다음 2줄은 문제가 있습니다.
//        // CameraUpdateFactory.zoomTo가 오동작하네요.
//        //mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
//        //mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
//        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 10F))

    //현위치 나타내기 위한
    var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val locationList: List<Location> = locationResult.getLocations()
            if (locationList.size > 0) {
                location = locationList[locationList.size - 1]
                //location = locationList.get(0);
                currentPosition = LatLng(location!!.latitude, location!!.longitude)
                val markerTitle = getCurrentAddress(currentPosition)
                val markerSnippet = "위도:" + location!!.latitude.toString() + " 경도:" + location!!.longitude.toString()
                Log.d(TAG, "onLocationResult : $markerSnippet")


                //현재 위치에 마커 생성하고 이동
                setCurrentLocation(location, markerTitle, markerSnippet)
                mCurrentLocatiion = location
            }
        }
    }

    //위치 업데이트
    private fun startLocationUpdates() {
        if (!checkLocationServicesStatus()) {
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting")
            showDialogForLocationServiceSetting()
        } else {
            val hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음")
                return
            }
            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates")
            mFusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            if (checkPermission()) mMap?.setMyLocationEnabled(true)
        }
    }

    //현위치 정보 가져온 경우
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        if (checkPermission()) {
            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates")
            mFusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
            if (mMap != null) mMap?.setMyLocationEnabled(true)
        }
    }

    //현위치 정보 나타나지 않는 경우
    override fun onStop() {
        super.onStop()
        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates")
            mFusedLocationClient?.removeLocationUpdates(locationCallback)
        }
    }

    fun getCurrentAddress(latlng: LatLng?): String {

        //지오코더... GPS를 주소로 변환
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>
        addresses = try {
            geocoder.getFromLocation(
                latlng!!.latitude,
                latlng!!.longitude,
                1)
        } catch (ioException: IOException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            return "지오코더 서비스 사용불가"
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            return "잘못된 GPS 좌표"
        }
        return if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show()
            "주소 미발견 $addresses"
        } else {
            val address = addresses[0]
            address.getAddressLine(0).toString()
        }
    }

    fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    //현재 위치에 마커 생성
    fun setCurrentLocation(location: Location?, markerTitle: String?, markerSnippet: String?) {
        if (currentMarker != null) currentMarker?.remove()
        val currentLatLng = LatLng(location!!.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(currentLatLng)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)
        //현재 위치 버튼이미지
        val bitmapdraw = resources.getDrawable(R.drawable.button03) as BitmapDrawable
        val b: Bitmap = bitmapdraw.bitmap
        val smallMarker: Bitmap = Bitmap.createScaledBitmap(b, 80, 80, false)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker))



        currentMarker = mMap?.addMarker(markerOptions)
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng)
        mMap?.moveCamera(cameraUpdate)
    }

    // 디폴트 위치
    fun setDefaultLocation() {

        //디폴트 위치, Seoul
        val DEFAULT_LOCATION = LatLng(37.56, 126.97)
        val markerTitle = "위치정보 가져올 수 없음"
        val markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요"
        if (currentMarker != null) currentMarker?.remove()
        val markerOptions = MarkerOptions()
        markerOptions.position(DEFAULT_LOCATION)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))


        currentMarker = mMap?.addMarker(markerOptions)
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15F)
        mMap?.moveCamera(cameraUpdate)
    }

    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    private fun checkPermission(): Boolean {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        return if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            true
        } else false
    }

    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    override fun onRequestPermissionsResult(permsRequestCode: Int,
                                            permissions: Array<String>,
                                            grandResults: IntArray) {
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.size == REQUIRED_PERMISSIONS.size) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            var check_result = true


            // 모든 퍼미션을 허용했는지 체크합니다.
            for (result in grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result) {

                // 퍼미션을 허용했다면 위치 업데이트를 시작합니다.
                startLocationUpdates()
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {


                    // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout!!, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인") { finish() }.show()
                } else {

                    // "다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout!!, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인") { finish() }.show()
                }
            }
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private fun showDialogForLocationServiceSetting() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("""
    앱을 사용하기 위해서는 위치 서비스가 필요합니다.
    위치 설정을 수정하실래요?
    """.trimIndent())
        builder.setCancelable(true)
        builder.setPositiveButton("설정") { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        }
        builder.setNegativeButton("취소") { dialog, id -> dialog.cancel() }
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : GPS 활성화 되있음")
                        needRequest = true
                        return
                    }
                }
        }
    }

    companion object {
        private const val TAG = "googlemap_example"
        private const val GPS_ENABLE_REQUEST_CODE = 2001
        private const val UPDATE_INTERVAL_MS = 1000 // 1초
        private const val FASTEST_UPDATE_INTERVAL_MS = 500 // 0.5초

        // onRequestPermissionsResult에서 수신된 결과에서 ActivityCompat.requestPermissions를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    //음식점 표시
    override fun onPlacesFailure(e: PlacesException?) {}

    override fun onPlacesStart() {}

    override fun onPlacesSuccess( places: List<Place>) {
        runOnUiThread {
            for (place in places) {
                val latLng = LatLng(place.getLatitude()
                    , place.getLongitude())
                val markerSnippet = getCurrentAddress(latLng)
                val markerOptions = MarkerOptions()
                markerOptions.position(latLng)
                markerOptions.title(place.getName()) // 마커 말풍선 제목 부분
                markerOptions.snippet(markerSnippet) // 마커 말풍선 설명 부분

                //음식점 마커 이미지
                val bitmapdraw = resources.getDrawable(R.drawable.button05) as BitmapDrawable
                val b: Bitmap = bitmapdraw.bitmap
                val smallMarker: Bitmap = Bitmap.createScaledBitmap(b, 80, 80, false)
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker))

                val item: Marker = mMap!!.addMarker(markerOptions);  //?추가함.
                previous_marker?.add(item)  //변경
            }

            //중복 마커 제거
            val hashSet: HashSet<Marker> = HashSet<Marker>()
            hashSet.addAll(previous_marker!!)
            previous_marker!!.clear()
            previous_marker!!.addAll(hashSet)
        }
    }

    override fun onPlacesFinished() {}

    fun showPlaceInformation(location: LatLng?) {
        mMap!!.clear() //지도 클리어

        if (previous_marker != null)
            previous_marker!!.clear() //지역정보 마커 클리어

        if (location != null) {
            NRPlaces.Builder()
                .listener(this@MainActivity)
                .key("AIzaSyCeF36HIWVqpfFqoD0sMk5uA-465JFQ9z8")
                .latlng(location.latitude, location.longitude) //현재 위치
                .radius(700) //500 미터 내에서 검색
                .type(PlaceType.RESTAURANT) //음식점
                .build()
                .execute()
        }
    }

    //마커 정보 저장하기 위해 전역변수 생성
    var markerID = ""
    var markerLoc = LatLng(0.0,0.0)

    // 말풍선 클릭시
    override fun onInfoWindowClick(marker: Marker) {
        markerID = marker.getId()
        markerLoc = marker.getPosition()
        Toast.makeText(this, "정보창 클릭 Marker ID : $markerID ,,, $markerLoc",
            Toast.LENGTH_SHORT).show()
        val bookdlg = bookmark_activity(this)
        bookdlg.start(marker)
        //showDialog01()  //여기에 매개변수로 마커 추가해서 위도 경도 사용하지 않고 this로 구현 시도해볼것.
    }

    //북마크 팝업창 띄우기
    /*
    fun showDialog01(){
        dilaog01?.show()

        //북마크 추가 버튼 클릭시 마커 색상 변경 - 클릭된 마커 위도 경도 활용
        var bookBtn: ImageButton? = dilaog01?.findViewById<ImageButton>(R.id.bookmarkBtn)
        bookBtn?.setOnClickListener(View.OnClickListener {
            fun onClick(view: View) {
                var bitmapdraw = resources.getDrawable(R.drawable.button04) as BitmapDrawable
                var b: Bitmap = bitmapdraw.bitmap
                val smallMarker: Bitmap = Bitmap.createScaledBitmap(b, 80,80, false)
                val bookmark00 = mMap?.addMarker(
                    MarkerOptions()
                        .position(markerLoc)
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                )
                // 팝업창 닫기
                dilaog01?.dismiss()
                Toast.makeText(this, "북마크에 추가 되었습니다.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }*/

    //마커 클릭이벤트 위해 생성 -> 그러나 실행시 마커의 말풍선이 나타나지 않는 오류 발생으로 주석 처리
//    override fun onMarkerClick(marker: Marker): Boolean {
//        var resName = marker.getTitle()
//        var resPosition = marker.getPosition()
//        Log.d(TAG, "name : $resName   address : $resPosition")
//        Toast.makeText(this, "${resName} + \n + ${resPosition}", Toast.LENGTH_LONG).show()
//
//        return true
//    }


}