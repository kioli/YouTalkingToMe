package kioli.youtalkingtome

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val requestCode = 1
    private val duration = 3000L
    private val handler: Handler = Handler()

    private lateinit var player: MediaPlayer
    private lateinit var surfaceHolder: SurfaceHolder
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        player = MediaPlayer.create(this, R.raw.youtalkingtome)
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), requestCode)
            return
        }
        startSurfaceView()
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    override fun onRequestPermissionsResult(reqCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(reqCode, permissions, grantResults)
        if (reqCode == requestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSurfaceView()
            hideErrorPage()
            return
        }
        showErrorPage()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        val numCameras = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until numCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Camera.open(i)?.let {
                    it.setPreviewDisplay(surfaceHolder)
                    it.setFaceDetectionListener { faces, camera -> faceDetected(faces) }
                    camera = it
                }
                return
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) {
            return
        }
        camera?.let {
            try {
                it.stopPreview()
            } catch (e: Exception) {
                // ignore: tried to stop a non-existent preview
            }
            try {
                it.setPreviewDisplay(surfaceHolder)
                it.startPreview()
                it.startFaceDetection()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        camera?.let {
            try {
                it.stopFaceDetection()
                it.setFaceDetectionListener(null)
                it.stopPreview()
                it.release()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        camera = null
    }

    private fun faceDetected(faces: Array<Camera.Face>?) {
        faces?.let { array ->
            if (array.isNotEmpty()) {
                camera?.stopFaceDetection()
                handler.postDelayed({
                    looking_view.setBackgroundColor(ContextCompat.getColor(baseContext, android.R.color.white))
                    camera?.startFaceDetection()
                }, duration)
                ContextCompat.getDrawable(baseContext, R.drawable.talkingtome)?.let {
                    looking_view.background = it
                }
                player.start()
            }
        }
    }

    private fun startSurfaceView() {
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
    }

    private fun showErrorPage() {
        error_view.visibility = View.VISIBLE
    }

    private fun hideErrorPage() {
        error_view.visibility = View.GONE
    }
}
