/*
Copyright (c) 2017-2019 Divested Computing Group
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package dev.yousif51811.lumoassist;

import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
import android.Manifest;
import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.ValueCallback;
import android.net.Uri;

import androidx.webkit.URLUtilCompat;

import org.woheller69.freeDroidWarn.FreeDroidWarn;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private WebView chatWebView = null;
    private WebSettings chatWebSettings = null;
    private CookieManager chatCookieManager = null;
    private final Context context = this;
    private String TAG ="lumoAssist";
    private String urlToLoad = "https://lumo.proton.me/";
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILE_CHOOSER_REQUEST_CODE = 1;

    @Override
    protected void onPause() {
        if (chatCookieManager!=null) chatCookieManager.flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create the WebView
        chatWebView = findViewById(R.id.chatWebView);
        registerForContextMenu(chatWebView);

        //Set cookie options
        chatCookieManager = CookieManager.getInstance();
        chatCookieManager.setAcceptCookie(true);
        chatCookieManager.setAcceptThirdPartyCookies(chatWebView, false);

        chatWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                    }
                }
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }

                mUploadMessage = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }

            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request.getResources().length > 0 && request.getResources()[0].equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(request.getResources());
                        } else {
                            // Request the permission from the user
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 123);
                            // A more robust solution would involve storing 'request' and calling request.grant() in onRequestPermissionsResult.
                        }
                    } else {
                        request.deny();
                    }
                } else {
                    request.grant(request.getResources()); // For older Android versions, permissions are granted at install time
                }
            }

        });  //needed to share link

        chatWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String urlStr = request.getUrl().toString();
                if (urlStr.equals("about:blank")) return false;

                boolean allowed = urlStr.startsWith("https://lumo.proton.me/") ||
                        urlStr.startsWith("https://account.proton.me/");

                if (!allowed) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Wait for sidebar to be fully loaded
                view.postDelayed(() -> {
                    injectSidebarButton(view);
                }, 1000); // 1 second delay to ensure DOM is ready
            }

            private void injectSidebarButton(WebView webView) {
                String javascript = readAsset("injectSidebar.js");

                if (!javascript.isEmpty()) {
                    webView.evaluateJavascript(javascript, null);
                }
            }

        });

        chatWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Uri source = Uri.parse(url);
            Log.d(TAG,"DownloadManager: " + url);
            DownloadManager.Request request = new DownloadManager.Request(source);
            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
            request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
            request.addRequestHeader("Referer", url);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
            String filename = URLUtilCompat.getFilenameFromContentDisposition(contentDisposition);
            if (filename == null) filename = URLUtilCompat.guessFileName(url, contentDisposition, mimetype);  // only if getFilenameFromContentDisposition does not work and returns null
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            Toast.makeText(this, getString(R.string.download) + "\n" + filename, Toast.LENGTH_SHORT).show();
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            assert dm != null;
            dm.enqueue(request);
        });

        //Set more options
        chatWebSettings = chatWebView.getSettings();
        //Enable some WebView features
        chatWebSettings.setJavaScriptEnabled(true);
        chatWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        chatWebSettings.setDomStorageEnabled(true);
        //Disable some WebView features
        chatWebSettings.setAllowContentAccess(false);
        chatWebSettings.setAllowFileAccess(false);
        chatWebSettings.setBuiltInZoomControls(false);
        chatWebSettings.setDatabaseEnabled(false);
        chatWebSettings.setDisplayZoomControls(false);
        chatWebSettings.setSaveFormData(false);
        chatWebSettings.setGeolocationEnabled(false);
        chatWebView.addJavascriptInterface(
                new AndroidInterface(),
                "AndroidInterface"
        );
        //Load Lumo
        chatWebView.loadUrl(urlToLoad);
        FreeDroidWarn.showWarningOnUpgrade(this, BuildConfig.VERSION_CODE);
    }

    private String readAsset(String fileName) {
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(fileName);

            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            return new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Could not read asset: " + fileName, e);
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Credit (CC BY-SA 3.0): https://stackoverflow.com/a/6077173
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (chatWebView.canGoBack() && !chatWebView.getUrl().equals("about:blank")) {
                        chatWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (mUploadMessage == null) return;
            Uri[] result = null;
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        result = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Microphone permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        // Handle other permission requests if any (like FILE_CHOOSER_REQUEST_CODE)
        if (requestCode == 100) { // This is the request code for READ_EXTERNAL_STORAGE from onShowFileChooser
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for file access
            } else {
                Toast.makeText(context, "Storage permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class AndroidInterface {

        @JavascriptInterface
        public void openOptions() {
            runOnUiThread(() -> {
                Intent intent = new Intent(
                        MainActivity.this,
                        OptionsActivity.class
                );

                startActivity(intent);

                Toast.makeText(
                        MainActivity.this,
                        "All Options are placeholder only for the time being.",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }
    }


}
