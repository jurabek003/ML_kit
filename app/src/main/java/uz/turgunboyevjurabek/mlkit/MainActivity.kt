package uz.turgunboyevjurabek.mlkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.opengl.ETC1Util.ETC1Texture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.ContactsContract.RawContacts.Data
import android.renderscript.Element
import android.renderscript.Element.DataType
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import uz.turgunboyevjurabek.mlkit.databinding.ActivityMainBinding
import uz.turgunboyevjurabek.mlkit.ml.LiteModelMovenetSingleposeLightningTfliteFloat164

class MainActivity : AppCompatActivity() {
    val paint=Paint()
    lateinit var imagrProcess: ImageProcessor
    lateinit var model:LiteModelMovenetSingleposeLightningTfliteFloat164
    lateinit var imageView: ImageView
    lateinit var bitmap: Bitmap
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        get_premissions()
        imagrProcess=ImageProcessor.Builder().add(ResizeOp(192,192,ResizeOp.ResizeMethod.BILINEAR)).build()
        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this@MainActivity)
        var textureView=binding.textrueView
        var imageView=binding.imageView

        cameraManager=getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread= HandlerThread("videoThread")
        handlerThread.start()

        handler=Handler(handlerThread.looper)

        paint.setColor(Color.YELLOW)

        textureView.surfaceTextureListener=object :TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int, ) {

                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int, ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap=textureView.bitmap!!
                var tensorImage = TensorImage(org.tensorflow.lite.DataType.UINT8)
                tensorImage.load(bitmap)
                tensorImage=imagrProcess.process(tensorImage)
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 192, 192, 3), org.tensorflow.lite.DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                val mutable=bitmap.copy(Bitmap.Config.ARGB_8888,true)
                val canvas=Canvas(mutable)
                val h=bitmap.height
                val w=bitmap.width

                var x=0

                while (x<=49){
                    if (outputFeature0.get(x+2)>0.045){
                        canvas.drawCircle(outputFeature0.get(x+1)*w,outputFeature0.get(x)*h,5f,paint)
                    }
                    x+=3
                }
                imageView.setImageBitmap(mutable)

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
    @SuppressLint("MissingPermission")
     fun openCamera() {
            val cameraId = cameraManager.cameraIdList.find { cameraId ->
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_FRONT
            }

            cameraId?.let { id ->
                cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        textureView= TextureView(this@MainActivity)
                        var captureRequest=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        textureView = binding.textrueView
                        var surface=Surface(textureView.surfaceTexture)
                        captureRequest.addTarget(surface)

                        camera.createCaptureSession(listOf(surface),object :CameraCaptureSession.StateCallback(){
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(),null,null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {

                            }
                        },handler)

                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        // Kamera bağlantısı kesildiğinde yapılacak işlemler
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        // Kamera hatası durumunda yapılacak işlemler
                    }
                }, handler)
            }
        }
    private fun get_premissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0]!=PackageManager.PERMISSION_GRANTED) get_premissions()
    }
}