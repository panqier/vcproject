package com.vcproject.greeneye.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vcproject.greeneye.composable.HomeContent
import com.vcproject.greeneye.composable.RealTimeDetectionScreen
import com.vcproject.greeneye.databinding.HomeFragmentBinding

class HomeFragment: Fragment() {
    private lateinit var binding: HomeFragmentBinding
    private lateinit var homeViewModel: HomeViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                homeViewModel.startDetection()
            } else {
                Toast.makeText(context, "Live Detection requires camera permission", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            val isCameraActive by homeViewModel.isCameraActive.collectAsState()
            val results by homeViewModel.detectionResults.collectAsState()
            if (isCameraActive) {
                BackHandler {
                    homeViewModel.stopDetection()
                }

                RealTimeDetectionScreen(
                    detectionResults = results,
                    onFrameAnalyzed = { bitmap ->
                        homeViewModel.processCameraFrame(bitmap)
                    },
                    onBackClick = {
                        homeViewModel.stopDetection()
                    },
                    onCaptureClick = {
                        homeViewModel.capturePhoto()
                    }
                )
            } else {
                HomeContent(
                    displayBitmap = null,
                    onTakePhotoClick = {
                        checkPermissionAndStart()
                    }
                )
            }

        }
    }

    private fun checkPermissionAndStart() {
        val permission = Manifest.permission.CAMERA

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                homeViewModel.startDetection()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

}