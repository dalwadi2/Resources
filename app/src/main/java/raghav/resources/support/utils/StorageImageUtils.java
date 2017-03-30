package raghav.resources.support.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import raghav.resources.BuildConfig;

/* Usage:

     1. declare provider in manifest

     <provider
        android:name="android.support.v4.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/image_path" />
        </provider>

     2. write permission and feature in manifest

     <uses-permission android:name="android.permission.CAMERA" />

     <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

     3. xml file for provider resource

     <paths>
        <files-path name="captured_image" path="Images/" />
     </paths>

     4. add in requesting activity

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            switch (requestCode) {
                case PermissionUtil.PermissionCode.WRITE_EXTERNAL_STORAGE:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager
                            .PERMISSION_GRANTED) {
                        StorageImageUtils.openChooserDialog(activity, String.valueOf(fileName));
                    }
                    break;
                case PermissionUtil.PermissionCode.CAMERA:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager
                            .PERMISSION_GRANTED) {
                        StorageImageUtils.startCameraIntent(activity, String.valueOf(fileName));
                    }
                    break;
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

      */

public class StorageImageUtils {

    public static final int REQUEST_GALLERY = 20005;
    public static final int REQUEST_CAMERA = 20006;
    private static final String IMAGE_DIRECTORY = "Images";
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = BuildConfig.APPLICATION_ID + ".fileprovider";
    private static String FILE_EXTENSION = ".jpg";

    public static void openChooserDialog(final Activity activity, final String fileName) {
        if (PermissionUtil.checkPermission(activity, PermissionUtil.Permissions.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle("Choose Image")
                    .setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            if (PermissionUtil.checkPermission(activity, PermissionUtil.Permissions.CAMERA)) {
                                startCameraIntent(activity, fileName);
                            } else {
                                PermissionUtil.getPermission(activity,
                                        PermissionUtil.Permissions.CAMERA,
                                        PermissionUtil.PermissionCode.CAMERA,
                                        PermissionUtil.PermissionMessage.CAMERA,
                                        null);
                            }
                        }
                    })
                    .setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            startGalleryIntent(activity);
                        }
                    })
                    .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();

            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);
            builder.show();
        } else

        {
            Toaster.shortToast("No permission to write");
        }
    }

    public static void startGalleryIntent(Activity activity) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_GALLERY);
    }

    public static void startCameraIntent(Activity activity, String fileName) {
        File path = new File(StorageUtils.createInternalDirectory(), IMAGE_DIRECTORY);

        if (!path.exists()) path.mkdirs();

        File image = new File(path, fileName + FILE_EXTENSION);

        Uri imageUri = FileProvider.getUriForFile(activity, CAPTURE_IMAGE_FILE_PROVIDER, image);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clip =
                    ClipData.newUri(activity.getContentResolver(), "A photo", imageUri);

            intent.setClipData(clip);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            List<ResolveInfo> resInfoList =
                    activity.getPackageManager()
                            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, imageUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }

        activity.startActivityForResult(intent, REQUEST_CAMERA);
    }

    public static File getGalleryImageFile(Intent data, ContentResolver resolver, String fileName) {
        if (data != null) {
            try {
                return saveImageToStorage(
                        MediaStore.Images.Media.getBitmap(resolver, data.getData()),
                        IMAGE_DIRECTORY,
                        fileName);
            } catch (IOException e) {
                AppLog.log(AppLog.D, false, AppLog.TAG, "getGalleryImageFile: " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public static File getCameraImageFile(String fileName) {
        File path = new File(StorageUtils.createInternalDirectory(), IMAGE_DIRECTORY);
        if (!path.exists()) path.mkdirs();
        return new File(path, fileName + FILE_EXTENSION);
    }

    public static File saveImageToStorage(Bitmap finalBitmap, String path, String imageName) {
        String root = StorageUtils.createInternalDirectory();
        File myDir = new File(root + "/" + path);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File file = new File(myDir, imageName + FILE_EXTENSION);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return file;
        } catch (Exception e) {
            AppLog.log(AppLog.D, false, AppLog.TAG, "saveImageToStorage: " + e.getMessage());
            return file;
        }
    }

    public static class SaveImageTask extends AsyncTask<Void, Void, File> {

        private Activity activity;
        private Intent data;
        private int requestCode;
        private String fileName;
        private FileSaveListener listener;

        public SaveImageTask(Activity activity,
                             Intent data,
                             int requestCode,
                             String fileName,
                             FileSaveListener listener) {
            this.activity = activity;
            this.data = data;
            this.requestCode = requestCode;
            this.fileName = fileName;
            this.listener = listener;
        }

        @Override
        protected File doInBackground(Void... pObjects) {
            File file = null;

            if (requestCode == StorageImageUtils.REQUEST_GALLERY) {
                file = StorageImageUtils
                        .getGalleryImageFile(data,
                                activity.getContentResolver()
                                , String.valueOf(fileName));
            } else if (requestCode == StorageImageUtils.REQUEST_CAMERA) {
                file = StorageImageUtils.getCameraImageFile(String.valueOf(fileName));
            }

            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            if (listener != null)
                listener.fileSaved(file);
            super.onPostExecute(file);
        }

        public interface FileSaveListener {
            void fileSaved(File file);
        }
    }
}