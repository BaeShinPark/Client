package com.cse.coari.activity

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.rotationMatrix
import androidx.core.graphics.transform
import com.cse.coari.R
import com.cse.coari.data.AnchorDTO
import com.cse.coari.retrofit.RetrofitBuilder
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.Math.abs
import java.lang.Math.round


open class ArVideoFragment : ArFragment(), SensorEventListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: VideoAnchorNode
    private lateinit var viewRenderable: ViewRenderable
    private lateinit var viewAnchorNode: AnchorNode
//    private lateinit var sensorManager: SensorManager

    private var mAccelerometer: Sensor?= null
    private var mMagnetField: Sensor?=null

    private var uiElement = Node()

    /*Sensor variables*/
    private val mAccValues = FloatArray(3)
    private val mMagnetValues = FloatArray(3)
    private val mR = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var accRunning = false
    private var magnetRunning = false
    val REQUEST_IMAGE_CAPTURE = 1

    /* distance &  */
    var distance: Double = 0.0
    var azimuthDegrees = 0f
    var direction: String? = null
    /* Sensor */
    private val sensorManager by lazy {
        activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var activeAugmentedImage: AugmentedImage? = null

    private lateinit var anchorItems: AnchorDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer()

        RetrofitBuilder.api.getAnchor().enqueue(object : Callback<AnchorDTO>{
            override fun onResponse(call: Call<AnchorDTO>, response: Response<AnchorDTO>) {
                anchorItems = response.body()!!
                Log.e("RETROFIT", anchorItems.toString())
            }

            override fun onFailure(call: Call<AnchorDTO>, t: Throwable) {
                Log.e("RETROFIT", "Call Anchor Data Failed")
            }
        })

    }

    override fun onResume() {
        super.onResume()

        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, mMagnetField, SensorManager.SENSOR_DELAY_NORMAL)

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.planeRenderer.isEnabled = false
        arSceneView.isLightEstimationEnabled = false


        initializeSession()
        createArScene()
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)


        Log.e("SENSOR", sensorManager.toString())

        return view
    }


    override fun getSessionConfiguration(session: Session): Config {

        fun loadAugmentedImageBitmap(imageName: String): Bitmap =
            requireContext().assets.open(imageName).use { return BitmapFactory.decodeStream(it) }

        fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {
            try {
                config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
                    db.addImage(VIDEO_907, loadAugmentedImageBitmap(IMAGE_907))
                    db.addImage(VIDEO_912, loadAugmentedImageBitmap(IMAGE_912))
                    db.addImage(VIDEO_919, loadAugmentedImageBitmap(IMAGE_919))
                }
                return true
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Could not add bitmap to augmented image database", e)
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image bitmap.", e)
            }
            return false
        }

        return super.getSessionConfiguration(session).also {
            it.lightEstimationMode = Config.LightEstimationMode.DISABLED
            it.focusMode = Config.FocusMode.AUTO

            if (!setupAugmentedImageDatabase(it, session)) {
                Toast.makeText(
                    requireContext(),
                    "Could not setup augmented image database",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createArScene() {
        // Create an ExternalTexture for displaying the contents of the video.
        externalTexture = ExternalTexture().also {
            mediaPlayer.setSurface(it.surface)
        }


        ModelRenderable.builder()
            .setSource(requireContext(), R.raw.augmented_video_model)
            .build()
            .thenAccept { renderable ->
                videoRenderable = renderable
                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                renderable.material.setExternalTexture("videoTexture", externalTexture)
            }
            .exceptionally { throwable ->
                Log.e(TAG, "Could not create ModelRenderable", throwable)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(requireContext(), R.layout.activity_detail_ar)
            .build()
            .thenAccept {
                viewRenderable = it
                viewRenderable.isShadowCaster = false
                viewRenderable.isShadowReceiver = false
                viewRenderable.view.findViewById<TextView>(R.id.tv_1).text = anchorItems[2].roomNumber
                viewRenderable.view.findViewById<TextView>(R.id.tv_2).text = anchorItems[2].roomName
                viewRenderable.view.findViewById<TextView>(R.id.tv_3).text = anchorItems[2].roomContent
                viewRenderable.view.findViewById<Button>(R.id.button_navi).setOnClickListener(
                    View.OnClickListener {
                        val alert = AlertDialog.Builder(context)

                        alert.setTitle("내비게이션")
                        alert.setMessage("이동하고자 하는 호실의 번호를 입력해 주십시오.")

                        val input = EditText(context)
                        alert.setView(input)

                        alert.setPositiveButton(
                            "Ok",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                calculateSensor()
                            })

                        alert.setNegativeButton("Cancel",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                // Canceled.
                            })
                        alert.show()

                    })

            }.exceptionally { throwable ->
                Log.e(TAG, "Could not create ModelRenderable", throwable)
                return@exceptionally null
            }

        videoAnchorNode = VideoAnchorNode().apply {
            setParent(arSceneView.scene)
        }

        viewAnchorNode = AnchorNode().apply {
            setParent(arSceneView.scene)
        }
    }

    /**
     * In this case, we want to support the playback of one video at a time.
     * Therefore, if ARCore loses current active image FULL_TRACKING we will pause the video.
     * If the same image gets FULL_TRACKING back, the video will resume.
     * If a new image will become active, then the corresponding video will start from scratch.
     */
    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        // If current active augmented image isn't tracked anymore and video playback is started - pause video playback
        val nonFullTrackingImages = updatedAugmentedImages.filter { it.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING }
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (isArVideoPlaying() && nonFullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                pauseArVideo()
            }
        }

        val fullTrackingImages = updatedAugmentedImages.filter { it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING }
        if (fullTrackingImages.isEmpty()) return

        // If current active augmented image is tracked but video playback is paused - resume video playback
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (fullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                if (!isArVideoPlaying()) {
                    resumeArVideo()
                }
                return
            }
        }

        // Otherwise - make the first tracked image active and start video playback
        fullTrackingImages.firstOrNull()?.let { augmentedImage ->
            try {
                playbackArVideo(augmentedImage)
            } catch (e: Exception) {
                Log.e(TAG, "Could not play video [${augmentedImage.name}]", e)
            }
        }
    }

    private fun isArVideoPlaying() = mediaPlayer.isPlaying

    private fun pauseArVideo() {
        videoAnchorNode.renderable = null
        viewAnchorNode.renderable = null
        mediaPlayer.pause()
    }


    private fun resumeArVideo() {
        mediaPlayer.start()
        fadeInVideo()
    }

    private fun dismissArVideo() {
        videoAnchorNode.anchor?.detach()
        viewAnchorNode.anchor?.detach()
        videoAnchorNode.renderable = null
        viewAnchorNode.renderable = null
        activeAugmentedImage = null
        mediaPlayer.reset()
    }

    private fun playbackArVideo(augmentedImage: AugmentedImage) {
        Log.d(TAG, "playbackVideo = ${augmentedImage.name}")

        requireContext().assets.openFd(augmentedImage.name)
            .use { descriptor ->

                val metadataRetriever = MediaMetadataRetriever()
                metadataRetriever.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )

                val videoWidth = metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
                val videoHeight = metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull()
                    ?: 0f
                val videoRotation = metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toFloatOrNull() ?: 0f


                // Account for video rotation, so that scale logic math works properly
                val imageSize = RectF(0f, 0f, augmentedImage.extentX, augmentedImage.extentZ)
                    .transform(rotationMatrix(videoRotation))

                val videoScaleType = VideoScaleType.CenterCrop

                videoAnchorNode.setVideoProperties(
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    videoRotation = videoRotation,
                    imageWidth = imageSize.width(),
                    imageHeight = imageSize.height(),
                    videoScaleType = videoScaleType
                )


                // Update the material parameters
                videoRenderable.material.setFloat2(
                    MATERIAL_IMAGE_SIZE,
                    imageSize.width(),
                    imageSize.height()
                )

                viewRenderable.material.setFloat2(
                    MATERIAL_IMAGE_SIZE,
                    imageSize.width(),
                    imageSize.height()
                )

                videoRenderable.material.setFloat2(MATERIAL_VIDEO_SIZE, videoWidth, videoHeight)
                videoRenderable.material.setBoolean(MATERIAL_VIDEO_CROP, VIDEO_CROP_ENABLED)

                viewRenderable.material.setFloat2(MATERIAL_VIDEO_SIZE, videoWidth, videoHeight)
                viewRenderable.material.setBoolean(MATERIAL_VIDEO_CROP, VIDEO_CROP_ENABLED)

                mediaPlayer.reset()
                mediaPlayer.setDataSource(descriptor)
            }.also {
                mediaPlayer.isLooping = true
                mediaPlayer.prepare()
                mediaPlayer.start()
            }


        videoAnchorNode.anchor?.detach()
        videoAnchorNode.anchor = augmentedImage.createAnchor(augmentedImage.centerPose)



        viewAnchorNode.anchor?.detach()
//        viewAnchorNode.anchor = augmentedImage.createAnchor(augmentedImage.centerPose)

//        val pose = Pose.makeTranslation(0.0f, 0.0f, 0.12f)
        val anchorpose = Pose.makeTranslation(
            augmentedImage.centerPose.tx(),
            augmentedImage.centerPose.ty() + 0.08f,
            augmentedImage.centerPose.tz()
        )
//        viewAnchorNode.localPosition.set(augmentedImage.centerPose.tx()+90.0f, augmentedImage.centerPose.ty(), augmentedImage.centerPose.tz())

        viewAnchorNode.anchor = augmentedImage.createAnchor(anchorpose)

        activeAugmentedImage = augmentedImage

        externalTexture.surfaceTexture.setOnFrameAvailableListener {
            it.setOnFrameAvailableListener(null)
            fadeInVideo()
        }
    }

    private fun fadeInVideo() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400L
            interpolator = LinearInterpolator()
            addUpdateListener { v ->
                videoRenderable.material.setFloat(MATERIAL_VIDEO_ALPHA, v.animatedValue as Float)
                viewRenderable.material.setFloat(MATERIAL_VIDEO_ALPHA, v.animatedValue as Float)
            }
            doOnStart { videoAnchorNode.renderable = videoRenderable
                viewAnchorNode.renderable = viewRenderable
            }
            start()
        }
    }

    private fun calculateSensor() {

        if(accRunning || magnetRunning) {
            SensorManager.getRotationMatrix(mR, null, mAccValues, mMagnetValues)
            azimuthDegrees = ((Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0].toDouble()) + 360).toInt() % 360).toFloat()

            val currentAnchorPose = anchorItems[1].pose.toDouble()  // 919
            val destinationAnchorPost = anchorItems[2].pose.toDouble() // 912

            distance = currentAnchorPose - destinationAnchorPost * 6.2

            if (azimuthDegrees >= 0 && azimuthDegrees < 180) {
                if(distance < 0) direction = "right"
                else direction = "left"

            } else if(azimuthDegrees >= 180 && azimuthDegrees < 360) {
                if(distance < 0) direction = "left"
                else direction = "right"
            }
            distance = kotlin.math.abs(distance)
            distance.toString()
        }

        Log.d("distance", distance.toString())
        Log.d("direction", direction.toString())

        val alert = AlertDialog.Builder(context)

        alert.setMessage("이동하고자 하는 호실은 $direction 으로 약 ${distance}cm 만큼 이동하면 됩니다")

        alert.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, whichButton ->
                // Canceled.
            })
        alert.show()


    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mAccValues, 0, event.values.size)
            if (!accRunning) accRunning = true
        } else if (event.sensor == mMagnetField) {
            System.arraycopy(event.values, 0, mMagnetValues, 0, event.values.size)
            if (!magnetRunning) magnetRunning = true
        }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }



    override fun onPause() {
        super.onPause()
        dismissArVideo()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    companion object {
        private const val TAG = "ArVideoFragment"

        private const val IMAGE_907 = "907.png"
        private const val IMAGE_912 = "912.png"
        private const val IMAGE_919 = "919.png"

        private const val VIDEO_907 = "test_video_1.mp4"
        private const val VIDEO_912 = "912_gimbal.mp4"
        private const val VIDEO_919 = "919_gimbal.mp4"

        private const val VIDEO_CROP_ENABLED = true

        private const val MATERIAL_IMAGE_SIZE = "imageSize"
        private const val MATERIAL_VIDEO_SIZE = "videoSize"
        private const val MATERIAL_VIDEO_CROP = "videoCropEnabled"
        private const val MATERIAL_VIDEO_ALPHA = "videoAlpha"
    }
}