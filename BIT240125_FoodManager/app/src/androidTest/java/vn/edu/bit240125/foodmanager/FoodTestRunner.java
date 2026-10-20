package vn.edu.bit240125.foodmanager;

import static vn.edu.bit240125.foodmanager.data.Contract.*;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;
import vn.edu.bit240125.foodmanager.data.*;
import vn.edu.bit240125.foodmanager.model.*;

/**
 * Device integration tests and reproducible demo actions. Never included in the submitted app APK.
 */
public class FoodTestRunner extends Instrumentation {
  private Bundle args;
  private MainActivity app;
  private int passed = 0;
  private StringBuilder report = new StringBuilder();

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    this.args = args;
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      String mode = args.getString("mode", "test");
      if (mode.equals("test")) tests();
      else {
        Intent intent =
            new Intent(getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        app = (MainActivity) startActivitySync(intent);
        waitForIdleSync();
        pause(1700);
        if (mode.equals("demo1")) demo1();
        else if (mode.equals("demo2")) demo2();
        else if (mode.equals("demo3")) demo3();
        else if (mode.equals("syncOnline")) syncOnline();
        else if (mode.equals("waitSync")) waitSync();
        else if (mode.equals("ui")) uiTests();
        else if (mode.equals("reopen")) {
          check(
              "Offline data survives force-stop",
              app.repo.dishes("Demo Khiem", null, false, null, null, 0).size() == 1);
        }
      }
      result.putString("stream", report + "\nPASS: " + passed + " checks; mode=" + mode);
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("stream", report + "\nFAILED: " + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }

  private void check(String label, boolean ok) {
    if (!ok) throw new AssertionError(label);
    passed++;
    report.append("PASS ").append(passed).append(" ").append(label).append('\n');
    Bundle b = new Bundle();
    b.putString("stream", "PASS " + passed + ": " + label + "\n");
    sendStatus(0, b);
  }

  private void expect(String key, Runnable action) {
    try {
      action.run();
      throw new AssertionError("Expected " + key);
    } catch (ValidationException e) {
      check(key, e.key.equals(key));
    }
  }

  private Dish valid(String name, String category) {
    Dish d = new Dish();
    d.name = name;
    d.price = 42000;
    d.preparationTime = 12;
    d.dishCategoryId = category;
    return d;
  }

  private void tests() throws Exception {
    FoodRepository r = new FoodRepository(getTargetContext());
    check("Three categories seeded", r.categories().size() == 3);
    check("Six dishes seeded", r.dishes("", null, false, null, null, 0).size() == 6);
    try (Cursor c = r.getReadableDatabase().rawQuery("PRAGMA foreign_keys", null)) {
      c.moveToFirst();
      check("Foreign keys ON", c.getInt(0) == 1);
    }
    for (String table : new String[] {CATEGORY, DISH, IMAGE, OUTBOX}) {
      check("Table prefix " + table, table.startsWith(P));
      try (Cursor c = r.getReadableDatabase().rawQuery("PRAGMA table_info(" + table + ")", null)) {
        while (c.moveToNext())
          check("Column prefix " + c.getString(1), c.getString(1).startsWith(P));
      }
    }
    String cat = r.categories().get(0).id;
    Dish d = valid("Test local persistence", cat);
    String id = r.saveDish(d, true);
    check("Create UUID", id.length() == 36);
    check("Read saved dish", r.dish(id).price == 42000);
    FoodRepository another = new FoodRepository(getTargetContext());
    check("Disk persistence through new connection", another.dish(id) != null);
    another.close();
    d.name = "Test updated";
    d.price = 46000;
    r.saveDish(d, false);
    check("Edit saved", r.dish(id).name.equals("Test updated"));
    Dish bad = valid("  ", cat);
    expect("name_required", () -> r.saveDish(bad, true));
    bad.name = "Test invalid";
    bad.price = 0;
    expect("price_error", () -> r.saveDish(bad, true));
    bad.price = 1;
    bad.preparationTime = 0;
    expect("time_error", () -> r.saveDish(bad, true));
    bad.preparationTime = 1;
    bad.dishCategoryId = "missing";
    expect("category_error", () -> r.saveDish(bad, true));
    Dish dupe = valid(" test UPDATED ", cat);
    expect("duplicate", () -> r.saveDish(dupe, true));
    Dish same =
        valid(
            "Test updated",
            r.categories().stream().filter(c -> !c.id.equals(cat)).findFirst().get().id);
    r.saveDish(same, true);
    check("Same name in another category allowed", r.dish(same.id) != null);
    expect("range_error", () -> r.dishes("", null, false, 100L, 10L, 0));
    expect("range_error", () -> r.dishes("", null, false, -1L, null, 0));
    check(
        "Partial case-insensitive name search",
        r.dishes("UPDATED", null, false, null, null, 0).size() == 2);
    check("SQL wildcard treated as literal", r.dishes("%_", null, false, null, null, 0).isEmpty());
    check(
        "Combined search category availability price",
        r.dishes("updated", cat, true, 45000L, 47000L, 1).size() == 1);
    d.isAvailable = false;
    r.saveDish(d, false);
    check(
        "Unavailable remains in management",
        r.dishes("updated", cat, false, null, null, 0).size() == 1);
    check(
        "Unavailable excluded by available filter",
        r.dishes("updated", cat, true, null, null, 0).isEmpty());
    List<Dish> asc = r.dishes("", null, false, null, null, 1);
    boolean ordered = true;
    for (int i = 1; i < asc.size(); i++) ordered &= asc.get(i - 1).price <= asc.get(i).price;
    check("Price ascending", ordered);
    List<Dish> desc = r.dishes("", null, false, null, null, 2);
    ordered = true;
    for (int i = 1; i < desc.size(); i++) ordered &= desc.get(i - 1).price >= desc.get(i).price;
    check("Price descending", ordered);
    List<Dish> times = r.dishes("", null, false, null, null, 3);
    ordered = true;
    for (int i = 1; i < times.size(); i++)
      ordered &= times.get(i - 1).preparationTime <= times.get(i).preparationTime;
    check("Preparation time ascending", ordered);
    d.preparationTime = 15;
    check("Quick boundary 15", d.isQuick());
    d.preparationTime = 16;
    check("Regular boundary 16", !d.isQuick());
    d.images.add(new DishImage(SeedImages.URLS[0], true));
    d.images.add(new DishImage(SeedImages.URLS[1], true));
    expect("thumbnail_error", () -> r.saveDish(d, false));
    d.images.get(0).isThumbnail = false;
    r.saveDish(d, false);
    check("Multiple images saved", r.dish(id).images.size() == 2);
    check("Cover selected", r.dish(id).thumbnail().equals(SeedImages.URLS[1]));
    d.images.get(0).imageUrl = "bad-url";
    expect("url_error", () -> r.saveDish(d, false));
    d.images.get(0).imageUrl = SeedImages.URLS[1];
    expect("duplicate_image", () -> r.saveDish(d, false));
    d.images.clear();
    r.saveDish(d, false);
    check("Empty gallery saved", r.dish(id).images.isEmpty());
    expect("category_used", () -> r.deleteCategory(cat));
    check("Missing ID returns null", r.dish("not-an-id") == null);
    expect("not_found", () -> r.deleteDish("not-an-id"));
    DishCategory temp = new DishCategory(null, "Test category", "Temporary");
    r.saveCategory(temp, true);
    temp.name = "Edited category";
    r.saveCategory(temp, false);
    check("Category create edit", r.category(temp.id).name.equals("Edited category"));
    r.deleteCategory(temp.id);
    check("Unused category deletion", r.category(temp.id) == null);
    JSONObject json = r.json("dish", id);
    boolean prefix = true;
    for (Iterator<String> it = json.keys(); it.hasNext(); ) prefix &= it.next().startsWith(P);
    check("MockAPI payload uses prefixed fields", prefix);
    int before = r.pending();
    d.price = 47000;
    r.saveDish(d, false);
    d.price = 48000;
    r.saveDish(d, false);
    check("Outbox coalesces repeated edits", r.pending() == before);
    String[] old = r.pendingItems().stream().filter(a -> a[1].equals(id)).findFirst().get();
    d.price = 49000;
    r.saveDish(d, false);
    r.acknowledge(old);
    check("New revision survives stale sync acknowledgement", r.dirty("dish", id));
    json.put(PRICE, 1);
    r.importRemote("dish", json);
    check("Pending local edit wins over remote", r.dish(id).price == 49000);
    r.deleteDish(id);
    r.deleteDish(same.id);
    check("Delete persisted", r.dish(id) == null);
    check(
        "Seed data restored after integration tests",
        r.dishes("", null, false, null, null, 0).size() == 6);
    check("English default text", Texts.get("add", false).equals("Add dish"));
    check("Vietnamese validation translated", Texts.get("duplicate", true).startsWith("Tên món"));
    r.close();
  }

  private void main(Runnable action) {
    runOnMainSync(action);
    waitForIdleSync();
  }

  private void pause(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private View find(String tag) {
    final View[] out = {null};
    main(
        () -> {
          AlertDialog dialog = app.activeDialog();
          if (dialog != null) out[0] = dialog.getWindow().getDecorView().findViewWithTag(tag);
          if (out[0] == null) out[0] = app.getWindow().getDecorView().findViewWithTag(tag);
        });
    if (out[0] == null) throw new AssertionError("View not found: " + tag);
    return out[0];
  }

  private void click(String tag) {
    View v = find(tag);
    main(() -> v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), false));
    pause(550);
    main(v::performClick);
    pause(950);
  }

  private void type(String tag, String value) {
    View v = find(tag);
    main(
        () -> {
          v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), false);
          ((EditText) v).setText(value);
        });
    pause(900);
  }

  private void select(String tag, int pos) {
    View v = find(tag);
    main(() -> ((Spinner) v).setSelection(pos));
    pause(950);
  }

  private void dialogButton(int which) {
    main(
        () -> {
          AlertDialog d = app.activeDialog();
          if (d == null) throw new AssertionError("No active dialog");
          d.getButton(which).performClick();
        });
    pause(1100);
  }

  private void scrollBottom() {
    main(
        () -> {
          ViewGroup decor = (ViewGroup) app.getWindow().getDecorView();
          scrollAll(decor, true);
        });
    pause(1400);
  }

  private void scrollTop() {
    main(() -> scrollAll((ViewGroup) app.getWindow().getDecorView(), false));
    pause(1400);
  }

  private void scrollAll(ViewGroup group, boolean down) {
    if (group instanceof ScrollView) {
      ((ScrollView) group).fullScroll(down ? View.FOCUS_DOWN : View.FOCUS_UP);
      return;
    }
    for (int i = 0; i < group.getChildCount(); i++)
      if (group.getChildAt(i) instanceof ViewGroup)
        scrollAll((ViewGroup) group.getChildAt(i), down);
  }

  private void demo1() {
    pause(3500);
    click("dish_dish-1");
    pause(1800);
    scrollBottom();
    click("detail_cover_1");
    pause(1600);
    scrollTop();
    pause(1200);
    click("language");
    pause(1800);
    scrollBottom();
    pause(900);
    click("back");
    pause(1200);
    click("language");
    click("add_dish");
    click("save_dish");
    pause(1600);
    dialogButton(-1);
    scrollTop();
    type("dish_name", "Demo Khiem");
    type("dish_price", "42000");
    type("dish_time", "12");
    select("dish_category", 0);
    type("dish_description", "Mon demo BIT240125 - luu SQLite offline.");
    scrollBottom();
    click("add_image");
    type("image_url", SeedImages.URLS[2]);
    click("confirm_add_image");
    click("add_image");
    type("image_url", SeedImages.URLS[1]);
    click("confirm_add_image");
    scrollBottom();
    pause(1000);
    click("save_dish");
    check("UI create saved", app.repo.dishes("Demo Khiem", null, false, null, null, 0).size() == 1);
    pause(2200);
    scrollBottom();
    pause(1200);
    click("detail_cover_1");
    pause(1500);
    scrollTop();
    pause(1500);
  }

  private void demo2() {
    main(app::showList);
    type("search", "Demo");
    pause(1600);
    click("filters");
    select("filter_category", 1);
    click("filter_available");
    type("filter_min", "40000");
    type("filter_max", "50000");
    click("apply_filters");
    pause(2000);
    click("sort");
    main(
        () ->
            app.activeDialog()
                .getListView()
                .performItemClick(app.activeDialog().getListView().getChildAt(2), 2, 2));
    pause(1700);
    String id = app.repo.dishes("Demo Khiem", null, false, null, null, 0).get(0).id;
    click("dish_" + id);
    scrollBottom();
    click("edit_dish");
    type("dish_price", "46000");
    type("dish_time", "20");
    click("dish_available");
    scrollBottom();
    click("save_dish");
    pause(2200);
    scrollTop();
    pause(1000);
    click("back");
    pause(2000);
    check(
        "Filters retained after edit",
        ((TextView) find("result_count")).getText().toString().startsWith("0"));
    click("clear_filters");
    pause(1000);
    click("filters");
    type("filter_min", "90000");
    type("filter_max", "10000");
    click("apply_filters");
    pause(1800);
    dialogButton(-1);
    dialogButton(-2);
    click("categories");
    pause(1000);
    click("delete_category_cat-main");
    dialogButton(-1);
    pause(2000);
    dialogButton(-1);
    click("add_category");
    type("category_name", "Demo category");
    type("category_description", "BIT240125");
    click("save_category");
    scrollBottom();
    pause(1200);
    DishCategory temp =
        app.repo.categories().stream()
            .filter(c -> c.name.equals("Demo category"))
            .findFirst()
            .get();
    click("delete_category_" + temp.id);
    dialogButton(-1);
    pause(1000);
    click("back");
    type("search", "Demo");
    pause(1700);
    click("dish_" + id);
    scrollBottom();
    click("delete_dish");
    pause(1700);
    dialogButton(-2);
    pause(1000);
    click("delete_dish");
    pause(1500);
    dialogButton(-1);
    check("UI delete saved", app.repo.dish(id) == null);
    pause(1700);
    click("clear_filters");
    pause(1300);
  }

  private void demo3() {
    main(app::showList);
    click("language");
    pause(1800);
    click("add_dish");
    type("dish_name", "Mon offline BIT240125 v2");
    type("dish_price", "39000");
    type("dish_time", "15");
    type("dish_description", "Du lieu tao khi khong co Internet.");
    scrollBottom();
    click("save_dish");
    pause(1800);
    scrollTop();
    pause(1800);
    click("language");
    pause(1700);
    scrollBottom();
    click("edit_dish");
    type("dish_price", "40000");
    scrollBottom();
    click("save_dish");
    pause(1600);
    scrollTop();
    click("back");
    type("search", "offline");
    pause(2400);
    click("settings");
    pause(3500);
    scrollBottom();
    pause(1500);
    click("back");
    type("search", "offline");
    pause(1800);
    check(
        "Offline create and edit persisted",
        app.repo.dishes("Mon offline BIT240125 v2", null, false, null, null, 0).get(0).price == 40000);
  }

  private void waitSync() {
    for (int i = 0; i < 120; i++) {
      if (app.repo.pending() == 0 && app.syncState().equals("synced")) {
        check("Automatic synchronization completed", true);
        pause(2500);
        return;
      }
      pause(1000);
    }
    throw new AssertionError(
        "Sync timeout; state=" + app.syncState() + "; pending=" + app.repo.pending());
  }

  private void syncOnline() throws Exception {
    waitSync();
    List<Dish> items = app.repo.dishes("Mon offline BIT240125 v2", null, false, null, null, 0);
    check(
        "Offline record remains local after synchronization",
        items.size() == 1 && items.get(0).price == 40000);
    java.net.HttpURLConnection c =
        (java.net.HttpURLConnection)
            new java.net.URL(vn.edu.bit240125.foodmanager.sync.SyncConfig.DISHES).openConnection();
    c.setConnectTimeout(15000);
    c.setReadTimeout(15000);
    String json;
    try (java.io.InputStream in = c.getInputStream();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      json = out.toString("UTF-8");
    } finally {
      c.disconnect();
    }
    JSONArray a = new JSONArray(json);
    int matches = 0;
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      if (items.get(0).id.equals(o.optString(ID))) {
        matches++;
        check("Remote price equals offline edit", o.getLong(PRICE) == 40000);
      }
    }
    check("Exactly one remote record with stable UUID", matches == 1);
    main(app::showList);
    type("search", "offline");
    pause(3000);
    click("dish_" + items.get(0).id);
    pause(2500);
    scrollTop();
    pause(3000);
  }

  private void uiTests() {
    check("Default English screen", app.t("add").equals("Add dish"));
    click("language");
    check("Vietnamese language switch", app.t("add").equals("Thêm món"));
    click("dish_dish-1");
    check("Language retained in details", app.t("edit").equals("Sửa món"));
    scrollBottom();
    click("edit_dish");
    check("Language retained in edit", app.t("save").equals("Lưu món"));
    click("language");
    check("Switch language in form", app.t("save").equals("Save dish"));
    scrollTop();
    type("dish_name", "Mì Ý rau xanh");
    scrollBottom();
    click("save_dish");
    check("Duplicate validation is visible", app.activeDialog() != null);
    dialogButton(-1);
    click("back");
    click("add_dish");
    scrollBottom();
    click("save_dish");
    check("Empty name validation visible", app.activeDialog() != null);
    dialogButton(-1);
    scrollTop();
    type("dish_name", "UI test");
    type("dish_price", "0");
    type("dish_time", "12");
    scrollBottom();
    click("save_dish");
    check("Price validation visible", app.activeDialog() != null);
    dialogButton(-1);
    scrollTop();
    type("dish_price", "1000");
    type("dish_time", "0");
    scrollBottom();
    click("save_dish");
    check("Time validation visible", app.activeDialog() != null);
    dialogButton(-1);
    click("back");
    click("filters");
    type("filter_min", "100");
    type("filter_max", "1");
    click("apply_filters");
    check("Invalid filter validation visible", app.activeDialog() != null);
    dialogButton(-1);
    dialogButton(-2);
    main(() -> app.showDetail("missing"));
    check("Missing ID handled", app.screen.equals("list") && app.activeDialog() != null);
    dialogButton(-1);
  }
}
