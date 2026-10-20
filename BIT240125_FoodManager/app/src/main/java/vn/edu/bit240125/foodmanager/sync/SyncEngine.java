package vn.edu.bit240125.foodmanager.sync;

import static vn.edu.bit240125.foodmanager.data.Contract.*;

import android.content.*;
import android.net.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import vn.edu.bit240125.foodmanager.data.*;

/** Automatic, single-worker synchronization. No manual sync button. */
public final class SyncEngine {
  private final FoodRepository repo;
  private final SharedPreferences prefs;
  private final ConnectivityManager cm;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final Runnable listener;
  private final ConnectivityManager.NetworkCallback callback;
  public volatile String state = "local";
  public volatile String lastError = "";
  private volatile boolean closed = false;

  public SyncEngine(Context context, FoodRepository repository, Runnable listener) {
    repo = repository;
    this.listener = listener;
    prefs = context.getSharedPreferences("BIT240125_Settings", 0);
    if (!prefs.contains("BIT240125_CategoryEndpoint")) {
      prefs
          .edit()
          .putString("BIT240125_CategoryEndpoint", SyncConfig.CATEGORIES)
          .putString("BIT240125_DishEndpoint", SyncConfig.DISHES)
          .apply();
    }

    cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    callback =
        new ConnectivityManager.NetworkCallback() {
          @Override
          public void onAvailable(Network n) {
            request();
          }

          @Override
          public void onLost(Network n) {
            request();
          }

          @Override
          public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
            request();
          }
        };
    cm.registerDefaultNetworkCallback(callback);
    executor.scheduleWithFixedDelay(this::cycle, 2, 30, TimeUnit.SECONDS);
  }

  public boolean configured() {
    return !prefs.getString("BIT240125_CategoryEndpoint", "").isEmpty()
        && !prefs.getString("BIT240125_DishEndpoint", "").isEmpty();
  }

  public boolean online() {
    NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
    return c != null
        && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
  }

  public void request() {
    if (!closed)
      try {
        executor.execute(this::cycle);
      } catch (RejectedExecutionException ignored) {
      }
  }

  private void status(String value) {
    state = value;
    main.post(listener);
  }

  private void cycle() {
    if (closed) return;
    if (!configured()) {
      status("local");
      return;
    }
    if (!online()) {
      status("offline");
      return;
    }
    status("syncing");
    try {
      String cat = prefs.getString("BIT240125_CategoryEndpoint", "");
      String dish = prefs.getString("BIT240125_DishEndpoint", "");
      JSONArray cats = new JSONArray(http("GET", cat, null)),
          dishes = new JSONArray(http("GET", dish, null));
      Map<String, List<String>> catMap = remoteMap(cats), dishMap = remoteMap(dishes);
      // Pull categories first so dishes can resolve foreign keys. Dirty local entities are skipped.
      for (int i = 0; i < cats.length(); i++) repo.importRemote("category", cats.getJSONObject(i));
      for (String[] item : repo.pendingItems()) {
        if (!online()) throw new IOException("Offline");
        String endpoint = item[0].equals("category") ? cat : dish;
        Map<String, List<String>> map = item[0].equals("category") ? catMap : dishMap;
        List<String> ids = map.getOrDefault(item[1], new ArrayList<>());
        if (item[2].equals("delete")) {
          for (String remoteId : ids) http("DELETE", endpoint + "/" + encode(remoteId), null);
        } else {
          JSONObject payload = repo.json(item[0], item[1]);
          if (payload == null) continue;
          if (ids.isEmpty()) {
            JSONObject created = new JSONObject(http("POST", endpoint, payload));
            String remoteId = created.getString("id");
            map.put(item[1], new ArrayList<>(Arrays.asList(remoteId)));
          } else {
            http("PUT", endpoint + "/" + encode(ids.get(0)), payload);
            for (int i = 1; i < ids.size(); i++)
              http("DELETE", endpoint + "/" + encode(ids.get(i)), null);
          }
        }
        // A concurrent UI edit has a new revision and MUST remain queued.
        repo.acknowledge(item);
      }
      // Fresh GET after pushing avoids importing a stale response over acknowledged local edits.
      cats = new JSONArray(http("GET", cat, null));
      dishes = new JSONArray(http("GET", dish, null));
      for (int i = 0; i < cats.length(); i++) repo.importRemote("category", cats.getJSONObject(i));
      for (int i = 0; i < dishes.length(); i++) repo.importRemote("dish", dishes.getJSONObject(i));
      lastError = "";
      prefs.edit().putLong("BIT240125_LastSync", System.currentTimeMillis()).apply();
      status(repo.pending() == 0 ? "synced" : "pending");
    } catch (Exception e) {
      lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
      status(online() ? "sync_error" : "offline");
    }
  }

  private Map<String, List<String>> remoteMap(JSONArray array) throws JSONException {
    Map<String, List<String>> m = new HashMap<>();
    for (int i = 0; i < array.length(); i++) {
      JSONObject o = array.getJSONObject(i);
      String stable = o.getString(ID), server = o.getString("id");
      m.computeIfAbsent(stable, k -> new ArrayList<>()).add(server);
    }
    return m;
  }

  private String encode(String value) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, "UTF-8");
  }

  private String http(String method, String url, JSONObject body) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setConnectTimeout(10000);
    c.setReadTimeout(10000);
    c.setRequestMethod(method);
    c.setRequestProperty("Accept", "application/json");
    try {
      if (body != null) {
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream o = c.getOutputStream()) {
          o.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
      int code = c.getResponseCode();
      if (method.equals("DELETE") && code == 404) return "{}";
      if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
      try (InputStream in = c.getInputStream();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] b = new byte[8192];
        int n, total = 0;
        while ((n = in.read(b)) != -1) {
          total += n;
          if (total > 8_000_000) throw new IOException("Response too large");
          out.write(b, 0, n);
        }
        return out.toString("UTF-8");
      }
    } finally {
      c.disconnect();
    }
  }

  public void close() {
    closed = true;
    cm.unregisterNetworkCallback(callback);
    executor.shutdownNow();
    main.removeCallbacksAndMessages(null);
  }
}
