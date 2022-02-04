package com.example.guru2

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.Window
import android.widget.ImageButton
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import com.example.guru2.R.drawable.button04
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class bookmark_activity(context: Context) {
    private val dlg = Dialog(context)   //부모 액티비티의 context 가 들어감
    private lateinit var bookmarkBtn : ImageButton

    //마커 정보 저장하기 위해 전역변수 생성
    var markerLoc = LatLng(0.0,0.0)

    fun start(marker: Marker) {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dilaog01)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.setCancelable(true)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히도록

        bookmarkBtn = dlg.findViewById(R.id.bookmarkBtn)
        markerLoc = marker.getPosition()
        val markerOptions = MarkerOptions()
        //북마크 버튼 눌렀을 때
        bookmarkBtn.setOnClickListener {
            fun onClick(view: View) {
                //var bitmapdraw = resources.getDrawable(R.drawable.button04) as BitmapDrawable
                //var b: Bitmap = drawable.button04
                //val smallMarker: Bitmap = Bitmap.createScaledBitmap(button04, 80,80, false)

                markerOptions.position(markerLoc)
                    .icon(BitmapDescriptorFactory.fromResource(button04))
            //                    .title()
            //                    .snippet()



//                val bookmark00 = mMap?.addMarker(
//                    MarkerOptions()
//                        .position(markerLoc)
//                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
//                )
                // 팝업창 닫기
                dlg.dismiss()
//                Toast.makeText(this, "북마크에 추가 되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}