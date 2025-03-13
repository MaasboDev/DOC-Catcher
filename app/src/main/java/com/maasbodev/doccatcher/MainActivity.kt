package com.maasbodev.doccatcher

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.maasbodev.doccatcher.ui.theme.DOCCatcherTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val options = GmsDocumentScannerOptions.Builder()
			.setScannerMode(SCANNER_MODE_FULL)
			.setGalleryImportAllowed(true)
			.setPageLimit(5) //TODO: Set the page limit on user preferences
			.setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
			.build()
		val scanner = GmsDocumentScanning.getClient(options)

		setContent {
			DOCCatcherTheme {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background,
				) {
					var imageUris by remember {
						mutableStateOf<List<Uri>>(emptyList())
					}
					val scannerLauncher = rememberLauncherForActivityResult(
						contract = ActivityResultContracts.StartIntentSenderForResult(),
						onResult = { activityResult ->
							if (activityResult.resultCode == RESULT_OK) {
								val result = GmsDocumentScanningResult.fromActivityResultIntent(
									activityResult.data
								)
								imageUris = result?.pages?.map { it.imageUri } ?: emptyList()
								result?.pdf?.let { pdf ->
									val fos = FileOutputStream(File(filesDir, "scan.pdf")) //TODO: Request file name from user
									contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
										inputStream.copyTo(fos)
									}
								}
							}
						}
					)
					Column(
						modifier = Modifier
							.fillMaxSize()
							.verticalScroll(rememberScrollState()),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						imageUris.forEach { uri ->
							AsyncImage(
								model = uri,
								contentDescription = null, //TODO: Add content description localization string
								contentScale = ContentScale.FillWidth,
								modifier = Modifier.fillMaxWidth(),
							)
						}
						Button(onClick = {
							scanner.getStartScanIntent(this@MainActivity)
								.addOnSuccessListener { intentSender ->
									scannerLauncher.launch(
										IntentSenderRequest.Builder(intentSender).build()
									)
								}
								.addOnFailureListener { exception ->
									Toast.makeText(
										applicationContext,
										exception.message,
										Toast.LENGTH_LONG
									).show()
								}
						}) {
							Text(text = "Scan PDF")  //TODO: Add button text localization string
						}
					}
				}
			}
		}
	}
}
