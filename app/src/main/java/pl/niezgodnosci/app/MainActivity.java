package pl.niezgodnosci.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Na raporcie/karcie "wstecz" wraca do wpisu, na ekranie wpisu zamyka aplikację
        web.evaluateJavascript("(function(){if(typeof cur!=='undefined'&&cur!=='wpis'){go('wpis',-1);return '1'}return '0'})()",
                value -> { if (value == null || !value.contains("1")) finish(); });
    }

    private Uri saveToDownloads(String name, String mime, byte[] bytes) throws Exception {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, name);
        v.put(MediaStore.Downloads.MIME_TYPE, mime);
        v.put(MediaStore.Downloads.RELATIVE_PATH, "Download");
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        OutputStream o = getContentResolver().openOutputStream(uri);
        o.write(bytes);
        o.close();
        return uri;
    }

    private class Bridge {
        @JavascriptInterface
        public void saveBoth(String csvName, String csv, String pngName, String pngBase64) {
            try {
                final Uri u1 = saveToDownloads(csvName, "text/csv", csv.getBytes(StandardCharsets.UTF_8));
                final Uri u2 = saveToDownloads(pngName, "image/png", Base64.decode(pngBase64, Base64.DEFAULT));
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Zapisano w Pobranych: " + csvName + " + wykres", Toast.LENGTH_LONG).show();
                    ArrayList<Uri> uris = new ArrayList<>();
                    uris.add(u1);
                    uris.add(u2);
                    Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    i.setType("*/*");
                    i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Wyślij / otwórz raport"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd zapisu pliku", Toast.LENGTH_LONG).show());
            }
        }
    }
}
