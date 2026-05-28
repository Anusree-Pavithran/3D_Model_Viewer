package com.example.a3dmodelviewer

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.a3dmodelviewer.databinding.ActivityMainBinding
import com.example.a3dmodelviewer.databinding.ViewModelContainerBinding
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode

private data class ModelAsset(val displayName: String, val assetPath: String)

private class ModelCard(
    val binding: ViewModelContainerBinding,
    val sceneView: SceneView,
    var modelNode: ModelNode? = null,
    var isInteractMode: Boolean = false,
    var rotX: Float = 0f,
    var rotY: Float = 0f,
    var modelScale: Float = 1f,
    var containerW: Int = 0,
    var containerH: Int = 0,
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val models = listOf(
        ModelAsset("Computer", "models/computer.glb"),
        ModelAsset("Porsche 911", "models/porsche_911_turbo_996.glb"),
        ModelAsset("Theyyam", "models/theyyam_character.glb"),

    )

    private val activeCards = mutableListOf<ModelCard>()
    private var loadingCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAddModel.setOnClickListener {
            showModelPicker()
        }
    }

    private fun showModelPicker() {
        if (loadingCount > 0) {
            Toast.makeText(this, "Still loading, please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        val names = models.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setTitle("Add 3D Model")
            .setItems(names) { _, index ->
                spawnCard(models[index])
            }
            .show()
    }

    private fun spawnCard(asset: ModelAsset) {
        val itemBinding = ViewModelContainerBinding.inflate(
            LayoutInflater.from(this),
            binding.modelCanvas,
            false
        )

        val initialW = dp(260)
        val initialH = dp(300)
        val offset = activeCards.size * dp(28)

        val lp = FrameLayout.LayoutParams(initialW, initialH).apply {
            leftMargin = 0
            topMargin = 0
        }

        itemBinding.root.layoutParams = lp
        itemBinding.root.translationX = (dp(24) + offset).toFloat()
        itemBinding.root.translationY = (dp(80) + offset).toFloat()
        itemBinding.tvModelName.text = asset.displayName

        val sceneView = itemBinding.sceneView
        sceneView.lifecycle = lifecycle

        /*
         * Important:
         * SceneView is SurfaceView based. SurfaceView has separate GPU surface.
         * This helps selected card's surface move above others.
         */
        sceneView.setZOrderMediaOverlay(true)

        val card = ModelCard(
            binding = itemBinding,
            sceneView = sceneView,
            containerW = initialW,
            containerH = initialH
        )

        activeCards.add(card)
        binding.modelCanvas.addView(itemBinding.root)
        binding.emptyState.isVisible = false

        bringCardToTop(card)

        attachDragAndResize(card)
        attachButtons(card)
        loadModelAsync(card, asset)
    }

    private fun bringCardToTop(card: ModelCard) {
        activeCards.remove(card)
        activeCards.add(card)

        activeCards.forEachIndexed { index, item ->
            item.binding.root.elevation = if (item == card) 32f else 8f
            item.binding.root.translationZ = if (item == card) 32f else index.toFloat()

            if (item == card) {
                item.sceneView.setZOrderMediaOverlay(true)
                item.sceneView.setZOrderOnTop(true)
            } else {
                item.sceneView.setZOrderOnTop(false)
                item.sceneView.setZOrderMediaOverlay(true)
            }
        }

        card.binding.root.bringToFront()
        binding.fabAddModel.bringToFront()
        binding.modelCanvas.invalidate()
        binding.root.invalidate()
    }

    private fun loadModelAsync(card: ModelCard, asset: ModelAsset) {
        loadingCount++
        card.binding.tvModelName.text = "${asset.displayName} …"

        binding.root.post {
            val instance = runCatching {
                card.sceneView.modelLoader.createModelInstance(asset.assetPath)
            }.getOrNull()

            loadingCount--

            if (instance == null) {
                Toast.makeText(
                    this,
                    "Failed to load ${asset.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                removeCard(card)
                return@post
            }

            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.8f
            ).apply {
                position = Position(0f, 0f, 0f)
                scale = Scale(1f, 1f, 1f)
            }

            card.sceneView.addChildNode(node)
            card.sceneView.cameraNode.position = Position(0f, 0f, 6f)

            card.modelNode = node
            card.binding.tvModelName.text = asset.displayName

            card.binding.sceneView.alpha = 0f
            ObjectAnimator.ofFloat(card.binding.sceneView, View.ALPHA, 0f, 1f)
                .apply { duration = 280 }
                .start()

            bringCardToTop(card)
        }
    }

    private fun attachDragAndResize(card: ModelCard) {
        val container = card.binding.root

        container.setOnClickListener {
            bringCardToTop(card)
        }

        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    bringCardToTop(card)

                    if (card.isInteractMode) {
                        card.modelScale = (card.modelScale * detector.scaleFactor)
                            .coerceIn(0.3f, 4.0f)

                        card.modelNode?.scale = Scale(
                            card.modelScale,
                            card.modelScale,
                            card.modelScale
                        )
                    } else {
                        val factor = detector.scaleFactor

                        val newW = (card.containerW * factor).toInt()
                            .coerceIn(dp(160), dp(560))

                        val newH = (card.containerH * factor).toInt()
                            .coerceIn(dp(160), dp(560))

                        card.containerW = newW
                        card.containerH = newH

                        val lp = container.layoutParams as FrameLayout.LayoutParams
                        lp.width = newW
                        lp.height = newH
                        container.layoutParams = lp
                    }

                    return true
                }
            }
        )

        var lastRawX = 0f
        var lastRawY = 0f
        var isDragging = false

        card.binding.toolbar.setOnTouchListener { _, event ->
            if (card.isInteractMode) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bringCardToTop(card)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    isDragging = true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && event.pointerCount == 1) {
                        container.translationX += event.rawX - lastRawX
                        container.translationY += event.rawY - lastRawY

                        lastRawX = event.rawX
                        lastRawY = event.rawY
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                }
            }

            true
        }

        var resizeStartRawX = 0f
        var resizeStartRawY = 0f
        var resizeStartW = 0
        var resizeStartH = 0

        card.binding.resizeHandle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bringCardToTop(card)

                    resizeStartRawX = event.rawX
                    resizeStartRawY = event.rawY
                    resizeStartW = card.containerW
                    resizeStartH = card.containerH
                }

                MotionEvent.ACTION_MOVE -> {
                    val dw = (event.rawX - resizeStartRawX).toInt()
                    val dh = (event.rawY - resizeStartRawY).toInt()

                    val newW = (resizeStartW + dw).coerceIn(dp(160), dp(560))
                    val newH = (resizeStartH + dh).coerceIn(dp(160), dp(560))

                    card.containerW = newW
                    card.containerH = newH

                    val lp = container.layoutParams as FrameLayout.LayoutParams
                    lp.width = newW
                    lp.height = newH
                    container.layoutParams = lp
                }
            }

            true
        }

        var iLastRawX = 0f
        var iLastRawY = 0f

        card.binding.sceneView.setOnTouchListener { _, event ->
            bringCardToTop(card)

            if (!card.isInteractMode) {
                scaleDetector.onTouchEvent(event)
                return@setOnTouchListener false
            }

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    iLastRawX = event.rawX
                    iLastRawY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        val dx = event.rawX - iLastRawX
                        val dy = event.rawY - iLastRawY

                        card.rotY += dx * 0.5f
                        card.rotX += dy * 0.5f

                        card.modelNode?.rotation = Rotation(card.rotX, card.rotY, 0f)

                        iLastRawX = event.rawX
                        iLastRawY = event.rawY
                    }
                }
            }

            true
        }
    }

    private fun attachButtons(card: ModelCard) {
        card.binding.btnInteract.setOnClickListener {
            bringCardToTop(card)

            card.isInteractMode = !card.isInteractMode
            applyInteractModeVisuals(card)
        }

        card.binding.btnClose.setOnClickListener {
            removeCard(card)
        }
    }

    private fun applyInteractModeVisuals(card: ModelCard) {
        val active = card.isInteractMode

        card.binding.interactBorder.isVisible = active

        card.binding.btnInteract.backgroundTintList = getColorStateList(
            if (active) R.color.btn_bg_active else R.color.btn_bg_normal
        )

        card.binding.btnInteract.iconTint = getColorStateList(
            if (active) R.color.btn_icon_active else R.color.btn_icon_normal
        )

        card.binding.tvModeBadge.text = if (active) "3D" else "MOVE"

        card.binding.tvModeBadge.setTextColor(
            getColor(if (active) R.color.btn_icon_active else R.color.text_secondary)
        )

        card.binding.tvModeBadge.setBackgroundResource(
            if (active) R.drawable.bg_mode_badge_active else R.drawable.bg_mode_badge_normal
        )

        card.binding.tvModeHint.text = if (active) {
            "drag=rotate · pinch=zoom"
        } else {
            "drag=move · pinch=resize"
        }

        card.binding.resizeHandle.isVisible = !active
    }

    private fun removeCard(card: ModelCard) {
        card.modelNode?.let {
            card.sceneView.removeChildNode(it)
        }

        card.sceneView.destroy()

        binding.modelCanvas.removeView(card.binding.root)
        activeCards.remove(card)

        if (activeCards.isEmpty()) {
            binding.emptyState.isVisible = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        activeCards.toList().forEach { card ->
            card.modelNode?.let {
                card.sceneView.removeChildNode(it)
            }
            card.sceneView.destroy()
        }

        activeCards.clear()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}