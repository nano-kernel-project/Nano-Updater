package org.nano.updater.network

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.network.util.ProgressListener
import org.nano.updater.network.util.ProgressResponseBody
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.util.Constants
import org.nano.updater.worker.WorkerUtils
import java.io.File
import java.io.InputStream
import java.net.UnknownHostException
import javax.inject.Inject

class DownloadTask(
    private val context: Context,
    private val homeViewModel: HomeViewModel,
    private val isKernelUpdate: Boolean
) :
    AsyncTask<String, Void, UpdateViewModel.DownloadStatus>() {

    @Inject
    lateinit var client: OkHttpClient

    private var call: Call? = null

    init {
        (context.applicationContext as NanoApplication).appComponent.inject(this)
    }

    override fun onPreExecute() {
        // Observe for download state changes
        homeViewModel.getDownloadStatus().observe(context as MainActivity, Observer {
            if (it == UpdateViewModel.DownloadStatus.CANCELLED)
                call?.cancel()
        })
    }

    override fun doInBackground(vararg params: String?): UpdateViewModel.DownloadStatus {
        // Set download status to running
        homeViewModel.setDownloadStatus(UpdateViewModel.DownloadStatus.RUNNING)

        // Check if the file is half way downloaded
        // If it is, then, resume the download
        val updateType = if (isKernelUpdate)
            "kernel"
        else
            "updater"
        val localFile = File(context.getExternalFilesDir(updateType), params[1]!!)
        val localFileSize = localFile.length()

        // Setup a progressListener
        val progressListener = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                WorkerUtils.makeStatusNotification(
                    bytesRead,
                    contentLength,
                    done,
                    context,
                    params[1]!!,
                    isKernelUpdate
                )
            }
        }

        // Start download and check whether the response is success or not
        // Resume a download if possible and that's why we add a header
        call = client.newBuilder().apply {
            addInterceptor { chain: Interceptor.Chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalResponse.body()!!, progressListener))
                    .build()
            }
        }.build().newCall(
            Request.Builder().url(params[0]!!).addHeader(
                "Range",
                "bytes=$localFileSize-"
            ).tag(Constants.TAG_DOWNLOAD_REQUEST).build()
        )
        // Create a download start notification
        WorkerUtils.makeStatusNotification(0, 0, false, context, params[1]!!, isKernelUpdate)
        val response = try {
            // Execute request
            call?.execute()
        } catch (e: Exception) {
            if (e is UnknownHostException)
                (context as MainActivity).apply {
                    runOnUiThread {
                        Snackbar.make(
                            this.binding.fab,
                            getString(R.string.no_network_connection),
                            Snackbar.LENGTH_SHORT
                        )
                            .apply {
                                anchorView = (context as MainActivity).binding.fab
                                show()
                            }
                    }
                }
            return UpdateViewModel.DownloadStatus.CANCELLED
        }

        return if (response!!.isSuccessful) {
            val inputStream: InputStream = response.body()!!.byteStream()

            try {
                if (WorkerUtils.saveStreamToFile(inputStream = inputStream, file = localFile))
                    UpdateViewModel.DownloadStatus.COMPLETED
                else
                    UpdateViewModel.DownloadStatus.CANCELLED
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateViewModel.DownloadStatus.CANCELLED
            }
        } else
            UpdateViewModel.DownloadStatus.CANCELLED
    }

    override fun onPostExecute(result: UpdateViewModel.DownloadStatus?) {
        homeViewModel.setDownloadStatus(result!!)
    }
}
