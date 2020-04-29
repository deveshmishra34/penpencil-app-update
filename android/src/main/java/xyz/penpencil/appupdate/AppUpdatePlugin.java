package xyz.penpencil.appupdate;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.content.Context.DOWNLOAD_SERVICE;

@NativePlugin()
public class AppUpdatePlugin extends Plugin {
    private static final String TAG = "AppUpdatePlugin";
    private static final int BUFFER_SIZE = 4096;
    private long downloadID;
    private SharedPreferences prefs;

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    @PluginMethod()
    public void getAppInfo(PluginCall call) {
        try {
            prefs = getActivity().getApplicationContext()
                    .getSharedPreferences("PenpencilAppUpdateSetting", Context.MODE_PRIVATE);

            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String versionName = pInfo.versionName;
            String name = pInfo.packageName;
            int versionCode = pInfo.versionCode;

            JSObject jsObject = new JSObject();
            jsObject.put("version", versionCode);
            jsObject.put("binaryVersionCode", versionCode);
            jsObject.put("bundleName", name);
            jsObject.put("bundleVersion", versionName);
            jsObject.put("binaryVersionName", versionName);
            jsObject.put("deviceInfo", getDeviceInfo());
            jsObject.put("deviceId", prefs.getString("uuid", UUID.randomUUID().toString()));
            jsObject.put("updateVersion", prefs.getString("updateVersion", ""));
            jsObject.put("updateStatus", prefs.getString("updateStatus", ""));
            jsObject.put("updateUrl", prefs.getString("updateUrl", ""));

            call.resolve(jsObject);
        } catch (Exception e) {
            call.reject("Json Error: getAppInfo", e);
        }

    }

    @PluginMethod()
    public void updatePref(PluginCall call) {
        try {
            JSObject jsObject = call.getData();
            updatePref(prefs, jsObject);
            call.resolve();
        } catch (Exception e) {
            call.reject("Unable to update pref", e);
        }
    }

    @PluginMethod()
    public void copyAndExtractFile(PluginCall call) {
        String fileName = call.getString("fileName");
        String updateVersion = call.getString("updateVersion");

        File sourceFile = new File(getActivity().getApplicationContext().getExternalFilesDir(null), fileName);
        File destPath = new File(getActivity().getApplicationContext().getFilesDir(), "penpencil-updates");
        String updateUrl = destPath.getAbsolutePath() + "/" + updateVersion + "/www";
        if (!destPath.exists()) {
            destPath.mkdir();
        }

        try {
            unzip(sourceFile.getAbsolutePath(), destPath.getAbsolutePath() + "/" + updateVersion);
            JSObject jsObject = new JSObject();
            jsObject.put("updateUrl", updateUrl);
            jsObject.put("updateStatus", "Live");
            updatePref(prefs, jsObject);

//            Notify New Update is ready
            notifyListeners("appUpdateLive", null);

            sourceFile.delete();
            removeExtraDir(destPath, updateVersion);

            call.resolve(jsObject);
        } catch (Exception e) {
            sourceFile.delete();
            call.reject("Error while unzipping the update", e);
        }
    }

    private void removeExtraDir(File files, String updateVersion) {
        try {
            String currentUrl = files.getAbsolutePath() + "/" + updateVersion;
            File[] destPathDirList = files.listFiles();
            if (destPathDirList.length >= 3 ) {
                File file = (String.valueOf(destPathDirList[0].getAbsoluteFile()).equals(currentUrl)) ? destPathDirList[1]: destPathDirList[0];
                if (file.isDirectory()) {
                    deleteRecursive(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    @PluginMethod()
    public void downloadUpdate(PluginCall call) {
        try {
            String fileUrl = call.getString("fileUrl"); // downloadable URL
            String fileName = call.getString("fileName"); // fileName.zip

            getActivity().registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            File file = new File(getActivity().getApplicationContext().getExternalFilesDir(null), fileName);

            if (file.exists()) {
                notifyListeners("appUpdateDownloaded", null);
                Log.d(TAG, "File already downloaded " + file.getTotalSpace());
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl))
                    .setTitle("Update Downloading...")// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                    .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true)// Set if download is allowed on roaming network
                    .setVisibleInDownloadsUi(false);

            DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
            downloadID = downloadManager.enqueue(request);// enqueue puts the download request in the queue.

            call.resolve();
        } catch (Exception e) {
            call.reject("Unable to extract file", e);
        }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {

                Toast.makeText(getActivity().getApplicationContext(), "Update downloaded", Toast.LENGTH_SHORT).show();
                notifyListeners("appUpdateDownloaded", null);

                JSObject jsObject = new JSObject();
                jsObject.put("updateStatus", "Ready");
                updatePref(prefs, jsObject);

                getActivity().unregisterReceiver(onDownloadComplete);
            }
        }
    };

    /**
     * Utility Functions
     */

    private JSObject getDeviceInfo() {

        String platformVersion = String.valueOf(Build.VERSION.RELEASE);
        String manufacturer = String.valueOf(Build.MANUFACTURER);
        String brand = String.valueOf(Build.MANUFACTURER);
        String model = String.valueOf(Build.MODEL);
        String hardware = String.valueOf(Build.HARDWARE);
        String display = String.valueOf(Build.DISPLAY);

        JSObject jsObject = new JSObject();
        jsObject.put("platform", "android");
        jsObject.put("platformVersion", platformVersion);
        jsObject.put("manufacturer", manufacturer);
        jsObject.put("brand", brand);
        jsObject.put("model", model);
        jsObject.put("hardware", hardware);
        jsObject.put("display", display);

        return jsObject;
    }

    private void updatePref(SharedPreferences prefs, JSObject jsObject) {
        String updateVersion = jsObject.getString("updateVersion");
        String updateStatus = jsObject.getString("updateStatus");
        String updateUrl = jsObject.getString("updateUrl");

        SharedPreferences.Editor editor = prefs.edit();

        if (!TextUtils.isEmpty(updateVersion)) {
            editor.putString("updateVersion", updateVersion);
        }

        if (!TextUtils.isEmpty(updateStatus)) {
            editor.putString("updateStatus", updateStatus);
        }

        if (!TextUtils.isEmpty(updateUrl)) {
            editor.putString("updateUrl", updateUrl);
        }

        editor.apply();
    }

    private Boolean copyFile(File sourcePath, File destinationPath) {
        try {
            Log.d(TAG, sourcePath.getAbsolutePath());
            Log.d(TAG, destinationPath.getAbsolutePath());
            FileUtils.copyFile(sourcePath, destinationPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Boolean copyDirectory(File sourcePath, File destinationPath) {
        try {
            Log.d(TAG, sourcePath.getAbsolutePath());
            Log.d(TAG, destinationPath.getAbsolutePath());
            FileUtils.copyDirectory(sourcePath, destinationPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    private void unzip(String zipFilePath, String destDirectory) throws IOException {
        Log.d(TAG, zipFilePath);
        Log.d(TAG, destDirectory);
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
