package vn.edu.bit240125.foodmanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.concurrent.*;
import vn.edu.bit240125.foodmanager.data.SeedImages;

/** Seed photos remain available offline; custom HTTPS URLs are cached after first load. */
public final class ImageLoader {
  private static final ExecutorService pool = Executors.newFixedThreadPool(3);

  public static void load(Context context, String url, ImageView view) {
    view.setTag(url);
    view.setImageResource(vn.edu.bit240125.foodmanager.R.drawable.ic_food);
    view.setContentDescription(url == null ? "No image" : url);
    if (url == null || url.isEmpty()) return;
    pool.execute(
        () -> {
          try {
            Bitmap bitmap = null;
            for (int i = 0; i < SeedImages.URLS.length; i++)
              if (url.equals(SeedImages.URLS[i])) {
                try (InputStream in = context.getAssets().open("food_" + i + ".jpg")) {
                  bitmap = BitmapFactory.decodeStream(in);
                }
                break;
              }
            if (bitmap == null) {
              StringBuilder key = new StringBuilder();
              for (byte b : MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8")))
                key.append(String.format("%02x", b));
              File file = new File(context.getCacheDir(), "BIT240125_" + key + ".img");
              if (file.exists()) bitmap = BitmapFactory.decodeFile(file.getPath());
              if (bitmap == null) {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);
                c.setInstanceFollowRedirects(false);
                try {
                  if (c.getResponseCode() != 200)
                    throw new IOException("Image HTTP " + c.getResponseCode());
                  try (InputStream in = c.getInputStream();
                      ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] b = new byte[8192];
                    int n, total = 0;
                    while ((n = in.read(b)) != -1) {
                      total += n;
                      if (total > 5_000_000) throw new IOException("Image too large");
                      out.write(b, 0, n);
                    }
                    byte[] data = out.toByteArray();
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                    int sample = 1;
                    while (opts.outWidth / sample > 1400 || opts.outHeight / sample > 1400)
                      sample *= 2;
                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = sample;
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                    if (bitmap != null)
                      try (FileOutputStream f = new FileOutputStream(file)) {
                        f.write(data);
                      }
                  }
                } finally {
                  c.disconnect();
                }
              }
            }
            final Bitmap result = bitmap;
            if (result != null)
              view.post(
                  () -> {
                    if (url.equals(view.getTag())) view.setImageBitmap(result);
                  });
          } catch (Exception ignored) {
            /* Visible placeholder covers empty, failed and offline images. */
          }
        });
  }
}
