package com.example.cameratestapp

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.cameratestapp.databinding.ActivityMainBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import java.util.*

open class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    enum class CameraMode {
        Front, Back
    }

    enum class MorphologyMode {
        Erosion, Dilation, Opening, Closing, None
    }

    private lateinit var binding: ActivityMainBinding

    private val TAG: String = "CT"

    private lateinit var matInput: Mat
    private lateinit var matResult: Mat
    private var initFlag = true
    private var mCameraMode = CameraMode.Back

    private var mBrightness = 0
    private var mBlurKernelSize = 1
    private var mSharpeningWeight = 0
    private var mGrayScale = false
    private var mCannyEdge = false
    private var mCannyEdgeThreshold1 = 50
    private var mCannyEdgeThreshold2 = 200
    private var mBinarization = false
    private var mBinarizationThreshold = 127
    private var mMorphologyMode = MorphologyMode.Opening
    private var mMorphologySize = 0

    private lateinit var mOpenCvCameraView: CameraBridgeViewBase

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("native-lib")
        }
    }

    private var mLoaderCallback = object: BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> mOpenCvCameraView.enableView()
                else -> onManagerConnected(status)
            }
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE: Int = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mOpenCvCameraView = binding.activitySurfaceView
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.setCameraIndex(getCameraModeValue())

        binding.btnCamera.setOnClickListener{
            mCameraMode = if (mCameraMode == CameraMode.Front) CameraMode.Back else CameraMode.Front
            mOpenCvCameraView.disableView()
            mOpenCvCameraView.setCameraIndex(getCameraModeValue())
            mOpenCvCameraView.enableView()
        }

        binding.seekBlur.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mBlurKernelSize = if (progress == 0) 0 else progress * 2 - 1
                binding.seekBlurTitle.text = "Blur ($mBlurKernelSize)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBrightness.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mBrightness = progress - 127
                binding.seekBrightnessTitle.text = "Brightness ($mBrightness)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekSharpening.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mSharpeningWeight = progress
                binding.seekSharpeningTitle.text = "Sharpening ($mSharpeningWeight)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.checkGrayscale.setOnCheckedChangeListener { _, isChecked ->
            mGrayScale = isChecked
            renewBinarizationView()
        }

        binding.checkCannyedge.setOnCheckedChangeListener { _, isChecked ->
            mCannyEdge = isChecked
            renewBinarizationView()
        }

        binding.seekCannyedgeThreshold1.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mCannyEdgeThreshold1 = progress
                binding.seekCannyedgeThreshold1Title.text = "Threshold 1 ($mCannyEdgeThreshold1)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.seekCannyedgeThreshold2.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mCannyEdgeThreshold2 = progress
                binding.seekCannyedgeThreshold2Title.text = "Threshold 2 ($mCannyEdgeThreshold2)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.checkBinarization.setOnCheckedChangeListener{ _, isChecked ->
            mBinarization = isChecked
            renewBinarizationView()
        }

        binding.seekBinarization.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mBinarizationThreshold = progress
                binding.seekBinarizationTitle.text = "Threshold ($mBinarizationThreshold)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.radiogroupMorphology.setOnCheckedChangeListener{ _, checkedId ->
            mMorphologyMode = when (checkedId) {
                binding.radioMorphologyErosion.id -> MorphologyMode.Erosion
                binding.radioMorphologyDilation.id -> MorphologyMode.Dilation
                binding.radioMorphologyOpening.id -> MorphologyMode.Opening
                binding.radioMorphologyClosing.id -> MorphologyMode.Closing
                else -> MorphologyMode.None
            }
            Log.d(TAG, "mMorphologyMode = $mMorphologyMode")
        }

        binding.seekMorphologySize.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mMorphologySize = progress
                binding.seekMorphologyTitle.text = "Morphology size ($mMorphologySize)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        renewBinarizationView()

        // Example of a call to a native method
        //binding.sampleText.text = stringFromJNI()
    }

    private fun renewBinarizationView() {
        binding.layoutGrayscale.setBackgroundColor(Color.parseColor(if (mGrayScale) "#FFFFFF" else "#EEEEEE"))

        binding.checkCannyedge.isEnabled = mGrayScale
        val grayAndEdge = mGrayScale && mCannyEdge
        binding.layoutCannyedgethreashold.setBackgroundColor(Color.parseColor(if (grayAndEdge) "#FFFFFF" else "#EEEEEE"))
        binding.seekCannyedgeThreshold1.isEnabled = grayAndEdge
        binding.seekCannyedgeThreshold2.isEnabled = grayAndEdge

        binding.checkBinarization.isEnabled = mGrayScale
        val grayAndBin = mGrayScale && mBinarization
        binding.seekBinarization.isEnabled = grayAndBin

        binding.layoutMorphology.setBackgroundColor(Color.parseColor(if (grayAndBin) "#FFFFFF" else "#EEEEEE"))

        binding.radioMorphologyErosion.isEnabled = grayAndBin
        binding.radioMorphologyDilation.isEnabled = grayAndBin
        binding.radioMorphologyOpening.isEnabled = grayAndBin
        binding.radioMorphologyClosing.isEnabled = grayAndBin
        binding.seekMorphologySize.isEnabled = grayAndBin
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "onResume :: OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        //TODO("Not yet implemented")
    }

    override fun onCameraViewStopped() {
        //TODO("Not yet implemented")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame != null) {
            matInput = inputFrame.rgba()
            if (initFlag) {
                initFlag = false
            } else {
                matResult.release()
            }
        }
        var matFiltered: Mat = matInput.clone()
        //old camera case
        //Core.flip(matInput, matFiltered, -1)

        if (mBrightness != 0) {
            var matBrightnessResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
            ModifyBrightness(matFiltered.nativeObjAddr, matBrightnessResult.nativeObjAddr, mBrightness)
            matFiltered.release()
            matFiltered = matBrightnessResult
        }

        if (mBlurKernelSize != 0) {
            var matBlurResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
            Blur(matFiltered.nativeObjAddr, matBlurResult.nativeObjAddr, mBlurKernelSize)
            matFiltered.release()
            matFiltered = matBlurResult
        }

        if (mSharpeningWeight != 0) {
            var matSharpenResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
            Sharpening(matFiltered.nativeObjAddr, matSharpenResult.nativeObjAddr, mSharpeningWeight)
            matFiltered.release()
            matFiltered = matSharpenResult
        }

        if (mGrayScale) {
            var matGrayResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
            ConvertRGBtoGray(matFiltered.nativeObjAddr, matGrayResult.nativeObjAddr)
            matFiltered.release()
            matFiltered = matGrayResult

            if (mCannyEdge) {
                var matEdgeResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
                CannyEdge(matFiltered.nativeObjAddr, matEdgeResult.nativeObjAddr, mCannyEdgeThreshold1, mCannyEdgeThreshold2)
                matFiltered.release()
                matFiltered = matEdgeResult
            }

            if (mBinarization) {
                var matBinarizationResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
                Binarization(matFiltered.nativeObjAddr, matBinarizationResult.nativeObjAddr, mBinarizationThreshold)
                matFiltered.release()
                matFiltered = matBinarizationResult

                if (mMorphologySize != 0) {
                    var matMorphologyResult = Mat(matInput.rows(), matInput.cols(), matInput.type())
                    when (mMorphologyMode) {
                        MorphologyMode.Erosion -> Erosion(matFiltered.nativeObjAddr, matMorphologyResult.nativeObjAddr, mMorphologySize)
                        MorphologyMode.Dilation -> Dilation(matFiltered.nativeObjAddr, matMorphologyResult.nativeObjAddr, mMorphologySize)
                        MorphologyMode.Opening -> Opening(matFiltered.nativeObjAddr, matMorphologyResult.nativeObjAddr, mMorphologySize)
                        MorphologyMode.Closing -> Closing(matFiltered.nativeObjAddr, matMorphologyResult.nativeObjAddr, mMorphologySize)
                    }
                    matFiltered.release()
                    matFiltered = matMorphologyResult
                }
            }
        }

        matInput.release()
        matResult = matFiltered

        return matResult
    }


    private external fun ModifyBrightness(matAddrInput: Long, matAddrResult: Long, brightnessAdded: Int)
    private external fun Blur(matAddrInput: Long, matAddrResult: Long, kernelSize: Int)
    private external fun Sharpening(matAddrInput: Long, matAddrResult: Long, weight: Int)

    private external fun ConvertRGBtoGray(matAddrInput: Long, matAddrResult: Long)
    private external fun Binarization(matAddrInput: Long, matAddrResult: Long, threshold: Int)

    private external fun Erosion(matAddrInput: Long, matAddrResult: Long, iteration: Int)
    private external fun Dilation(matAddrInput: Long, matAddrResult: Long, iteration: Int)
    private external fun Opening(matAddrInput: Long, matAddrResult: Long, iteration: Int)
    private external fun Closing(matAddrInput: Long, matAddrResult: Long, iteration: Int)

    private external fun CannyEdge(matAddrInput: Long, matAddrResult: Long, threshold1: Int, threshold2: Int)

    private fun getCameraModeValue() =
        if (mCameraMode == CameraMode.Front) 1 else 0

    private fun getCameraViewList(): List<CameraBridgeViewBase?>? {
        return Collections.singletonList(mOpenCvCameraView)
    }

    private fun onCameraPermissionGranted() {
        val cameraViews = getCameraViewList() ?: return
        for (cameraBridgeViewBase in cameraViews) {
            cameraBridgeViewBase?.setCameraPermissionGranted()
        }
    }

    override fun onStart() {
        super.onStart()
        var havePermission: Boolean = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(listOf(CAMERA).toTypedArray(), CAMERA_PERMISSION_REQUEST_CODE)
                havePermission = false
            }
        }
        if (havePermission) {
            onCameraPermissionGranted()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted()
        } else {
            showDialogForPermission("need to grant permission to run this app.")
        }
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Alert")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("Yes"
        ) { _, _ -> requestPermissions(arrayOf(CAMERA), CAMERA_PERMISSION_REQUEST_CODE) }
        builder.setNegativeButton("No"
        ) { _, _ -> finish() }
        builder.create().show()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

//    companion object {
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
}