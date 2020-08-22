package com.smarttersstudio.camerademo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*


open class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this)
      return
    }
    val surfaceReadyCallback = object: SurfaceHolder.Callback {

      override fun surfaceCreated(p0: SurfaceHolder) {
        startCameraSession()
      }

      override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
      }

      override fun surfaceDestroyed(p0: SurfaceHolder) {
      }
    }
    surfaceView.holder.addCallback(surfaceReadyCallback)

  }


  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if(!CameraPermissionHelper.hasCameraPermission(this)){
      CameraPermissionHelper.requestCameraPermission(this);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }
  private fun startCameraSession() {
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    if (cameraManager.cameraIdList.isEmpty()) {
      // no cameras
      return
    }

    val firstCamera = cameraManager.cameraIdList[0]
    if (ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    cameraManager.openCamera(firstCamera, object: CameraDevice.StateCallback() {
      override fun onDisconnected(p0: CameraDevice) { }
      override fun onError(p0: CameraDevice, p1: Int) { }

      override fun onOpened(cameraDevice: CameraDevice) {
        // use the camera
        val cameraCharacteristics =    cameraManager.getCameraCharacteristics(cameraDevice.id)

        cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
          streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let { yuvSizes ->
            val previewSize = yuvSizes.last()
// cont.
            val displayRotation = windowManager.defaultDisplay.rotation
            val swappedDimensions = areDimensionsSwapped(displayRotation, cameraCharacteristics)
// swap width and height if needed
            val rotatedPreviewWidth = if (swappedDimensions) previewSize.height else previewSize.width
            val rotatedPreviewHeight = if (swappedDimensions) previewSize.width else previewSize.height
            surfaceView.holder.setFixedSize(rotatedPreviewWidth, rotatedPreviewHeight)
            val previewSurface = surfaceView.holder.surface

            val captureCallback = object : CameraCaptureSession.StateCallback()
            {
              override fun onConfigureFailed(session: CameraCaptureSession) {}

              override fun onConfigured(session: CameraCaptureSession) {
                // session configured
                val previewRequestBuilder =   cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                  .apply {
                    addTarget(previewSurface)
                  }
                session.setRepeatingRequest(
                  previewRequestBuilder.build(),
                  object: CameraCaptureSession.CaptureCallback() {},
                  Handler { true }
                )
              }
            }

            cameraDevice.createCaptureSession(mutableListOf(previewSurface), captureCallback, Handler { true })
          }

        }
      }
    }, Handler { true })

  }
  private fun areDimensionsSwapped(displayRotation: Int, cameraCharacteristics: CameraCharacteristics): Boolean {
    var swappedDimensions = false
    when (displayRotation) {
      Surface.ROTATION_0, Surface.ROTATION_180 -> {
        if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 90 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 270) {
          swappedDimensions = true
        }
      }
      Surface.ROTATION_90, Surface.ROTATION_270 -> {
        if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 0 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 180) {
          swappedDimensions = true
        }
      }
      else -> {
        // invalid display rotation
      }
    }
    return swappedDimensions
  }




}
