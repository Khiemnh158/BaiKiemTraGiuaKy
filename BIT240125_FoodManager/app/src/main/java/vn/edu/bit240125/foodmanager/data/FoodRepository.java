package vn.edu.bit240125.foodmanager.data;

import static vn.edu.bit240125.foodmanager.data.Contract.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.Normalizer;
import java.util.*;
import org.json.*;
import vn.edu.bit240125.foodmanager.model.*;

/** Local source of truth. Entity changes and outbox updates share ONE transaction. */
public class FoodRepository extends SQLiteOpenHelper {
  public FoodRepository(Context c) {
    super(c, "BIT240125_FoodManager.db", null, 1);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE "
            + CATEGORY
            + " ("
            + ID
            + " TEXT PRIMARY KEY,"
            + NAME
            + " TEXT NOT NULL CHECK(length(trim("
            + NAME
            + "))>0),"
            + DESCRIPTION
            + " TEXT NOT NULL DEFAULT '')");
    db.execSQL(
        "CREATE TABLE "
            + DISH
            + " ("
            + ID
            + " TEXT PRIMARY KEY,"
            + NAME
            + " TEXT NOT NULL CHECK(length(trim("
            + NAME
            + "))>0),"
            + NAME_KEY
            + " TEXT NOT NULL,"
            + PRICE
            + " INTEGER NOT NULL CHECK("
            + PRICE
            + ">0),"
            + TIME
            + " INTEGER NOT NULL CHECK("
            + TIME
            + ">0),"
            + AVAILABLE
            + " INTEGER NOT NULL CHECK("
            + AVAILABLE
            + " IN(0,1)),"
            + DESCRIPTION
            + " TEXT NOT NULL DEFAULT '',"
            + CATEGORY_ID
            + " TEXT NOT NULL REFERENCES "
            + CATEGORY
            + "("
            + ID
            + ") ON DELETE RESTRICT, UNIQUE("
            + CATEGORY_ID
            + ","
            + NAME_KEY
            + "))");
    db.execSQL(
        "CREATE TABLE "
            + IMAGE
            + " ("
            + ID
            + " TEXT PRIMARY KEY,"
            + DISH_ID
            + " TEXT NOT NULL REFERENCES "
            + DISH
            + "("
            + ID
            + ") ON DELETE CASCADE,"
            + URL
            + " TEXT NOT NULL,"
            + THUMBNAIL
            + " INTEGER NOT NULL CHECK("
            + THUMBNAIL
            + " IN(0,1)))");
    db.execSQL(
        "CREATE UNIQUE INDEX BIT240125_OneThumbnail ON "
            + IMAGE
            + "("
            + DISH_ID
            + ") WHERE "
            + THUMBNAIL
            + "=1");
    db.execSQL(
        "CREATE TABLE "
            + OUTBOX
            + " ("
            + ENTITY
            + " TEXT NOT NULL,"
            + ENTITY_ID
            + " TEXT NOT NULL,"
            + OP
            + " TEXT NOT NULL,"
            + REVISION
            + " TEXT NOT NULL,PRIMARY KEY("
            + ENTITY
            + ","
            + ENTITY_ID
            + "))");
    String[][] cats = {
      {"cat-main", "Món chính", "Cơm, mì và những món ăn chính"},
      {"cat-light", "Món nhẹ", "Salad, súp và món khai vị"},
      {"cat-drink", "Đồ uống", "Thức uống và nước trái cây"}
    };
    for (String[] a : cats) {
      ContentValues v = new ContentValues();
      v.put(ID, a[0]);
      v.put(NAME, a[1]);
      v.put(DESCRIPTION, a[2]);
      db.insertOrThrow(CATEGORY, null, v);
      queue(db, "category", a[0], "upsert");
    }
    String[][] seed = {
      {
        "dish-1",
        "Cơm rau củ",
        "65000",
        "20",
        "1",
        "Cơm nóng ăn kèm rau củ tươi và sốt nhẹ.",
        "cat-main"
      },
      {"dish-2", "Mì Ý rau xanh", "55000", "15", "1", "Mì Ý với cà chua và rau thơm.", "cat-main"},
      {"dish-3", "Salad rau củ", "35000", "10", "1", "Rau tươi trộn sốt chanh nhẹ.", "cat-light"},
      {"dish-4", "Súp bí đỏ", "30000", "18", "0", "Súp bí đỏ béo nhẹ, dùng nóng.", "cat-light"},
      {
        "dish-5",
        "Trà trái cây",
        "25000",
        "5",
        "1",
        "Trà hoa quả dùng lạnh với lát chanh.",
        "cat-drink"
      },
      {"dish-6", "Nước cam tươi", "30000", "8", "1", "Cam tươi ép nguyên chất.", "cat-drink"}
    };
    for (int i = 0; i < seed.length; i++) {
      String[] s = seed[i];
      Dish d = new Dish();
      d.id = s[0];
      d.name = s[1];
      d.price = Long.parseLong(s[2]);
      d.preparationTime = Integer.parseInt(s[3]);
      d.isAvailable = s[4].equals("1");
      d.description = s[5];
      d.dishCategoryId = s[6];
      d.images.add(new DishImage(SeedImages.URLS[i], true));
      if (i == 0) d.images.add(new DishImage(SeedImages.URLS[1], false));
      writeDish(db, d);
      queue(db, "dish", d.id, "upsert");
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {
    throw new IllegalStateException("Migration required");
  }

  public static String nameKey(String s) {
    return Normalizer.normalize(s.trim().replaceAll("\\s+", " "), Normalizer.Form.NFC)
        .toLowerCase(Locale.ROOT);
  }

  private String s(Cursor c, String col) {
    return c.getString(c.getColumnIndexOrThrow(col));
  }

  public synchronized List<DishCategory> categories() {
    List<DishCategory> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(CATEGORY, null, null, null, null, null, NAME + " COLLATE NOCASE")) {
      while (c.moveToNext()) result.add(new DishCategory(s(c, ID), s(c, NAME), s(c, DESCRIPTION)));
    }
    return result;
  }

  public synchronized DishCategory category(String id) {
    for (DishCategory c : categories()) if (c.id.equals(id)) return c;
    return null;
  }

  public synchronized Dish dish(String id) {
    try (Cursor c =
        getReadableDatabase().query(DISH, null, ID + "=?", new String[] {id}, null, null, null)) {
      return c.moveToFirst() ? readDish(c) : null;
    }
  }

  private Dish readDish(Cursor c) {
    Dish d = new Dish();
    d.id = s(c, ID);
    d.name = s(c, NAME);
    d.description = s(c, DESCRIPTION);
    d.dishCategoryId = s(c, CATEGORY_ID);
    d.price = c.getLong(c.getColumnIndexOrThrow(PRICE));
    d.preparationTime = c.getInt(c.getColumnIndexOrThrow(TIME));
    d.isAvailable = c.getInt(c.getColumnIndexOrThrow(AVAILABLE)) == 1;
    try (Cursor im =
        getReadableDatabase()
            .query(IMAGE, null, DISH_ID + "=?", new String[] {d.id}, null, null, "rowid")) {
      while (im.moveToNext())
        d.images.add(
            new DishImage(s(im, URL), im.getInt(im.getColumnIndexOrThrow(THUMBNAIL)) == 1));
    }
    return d;
  }

  public synchronized List<Dish> dishes(
      String query, String cat, boolean selling, Long min, Long max, int sort) {
    if ((min != null && min < 0)
        || (max != null && max < 0)
        || (min != null && max != null && min > max)) throw new ValidationException("range_error");
    List<String> args = new ArrayList<>();
    String where = "1=1";
    if (cat != null && !cat.isEmpty()) {
      where += " AND " + CATEGORY_ID + "=?";
      args.add(cat);
    }
    if (selling) where += " AND " + AVAILABLE + "=1";
    if (min != null) {
      where += " AND " + PRICE + ">=?";
      args.add("" + min);
    }
    if (max != null) {
      where += " AND " + PRICE + "<=?";
      args.add("" + max);
    }
    String order =
        sort == 1
            ? PRICE + " ASC"
            : sort == 2
                ? PRICE + " DESC"
                : sort == 3 ? TIME + " ASC" : NAME + " COLLATE NOCASE ASC";
    List<Dish> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                DISH,
                null,
                where,
                args.toArray(new String[0]),
                null,
                null,
                order + "," + ID + " ASC")) {
      while (c.moveToNext())
        if (nameKey(s(c, NAME)).contains(nameKey(query == null ? "" : query)))
          result.add(readDish(c));
    }
    return result;
  }

  public synchronized void validate(Dish d) {
    if (d.name.trim().isEmpty()) throw new ValidationException("name_required");
    if (d.price <= 0) throw new ValidationException("price_error");
    if (d.preparationTime <= 0) throw new ValidationException("time_error");
    if (category(d.dishCategoryId) == null) throw new ValidationException("category_error");
    if (DatabaseUtils.longForQuery(
            getReadableDatabase(),
            "SELECT COUNT(*) FROM "
                + DISH
                + " WHERE "
                + CATEGORY_ID
                + "=? AND "
                + NAME_KEY
                + "=? AND "
                + ID
                + "<>?",
            new String[] {d.dishCategoryId, nameKey(d.name), d.id == null ? "" : d.id})
        > 0) throw new ValidationException("duplicate");
    int thumbs = 0;
    Set<String> urls = new HashSet<>();
    for (DishImage i : d.images) {
      if (!validUrl(i.imageUrl)) throw new ValidationException("url_error");
      if (!urls.add(i.imageUrl.trim())) throw new ValidationException("duplicate_image");
      if (i.isThumbnail) thumbs++;
    }
    if (thumbs > 1) throw new ValidationException("thumbnail_error");
  }

  public static boolean validUrl(String value) {
    try {
      java.net.URI u = new java.net.URI(value.trim());
      return "https".equalsIgnoreCase(u.getScheme())
          && u.getHost() != null
          && !u.getHost().isEmpty()
          && u.getUserInfo() == null;
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized String saveDish(Dish d, boolean creating) {
    if (!creating && (d.id == null || dish(d.id) == null))
      throw new ValidationException("not_found");
    validate(d);
    if (creating) d.id = UUID.randomUUID().toString();
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      writeDish(db, d);
      queue(db, "dish", d.id, "upsert");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return d.id;
  }

  private void writeDish(SQLiteDatabase db, Dish d) {
    ContentValues v = new ContentValues();
    v.put(ID, d.id);
    v.put(NAME, d.name.trim());
    v.put(NAME_KEY, nameKey(d.name));
    v.put(PRICE, d.price);
    v.put(TIME, d.preparationTime);
    v.put(AVAILABLE, d.isAvailable ? 1 : 0);
    v.put(DESCRIPTION, d.description.trim());
    v.put(CATEGORY_ID, d.dishCategoryId);
    // UPDATE preserves foreign-key children and relationships; INSERT only for new UUIDs.
    if (db.update(DISH, v, ID + "=?", new String[] {d.id}) == 0) db.insertOrThrow(DISH, null, v);
    db.delete(IMAGE, DISH_ID + "=?", new String[] {d.id});
    for (DishImage i : d.images) {
      ContentValues a = new ContentValues();
      a.put(ID, UUID.randomUUID().toString());
      a.put(DISH_ID, d.id);
      a.put(URL, i.imageUrl.trim());
      a.put(THUMBNAIL, i.isThumbnail ? 1 : 0);
      db.insertOrThrow(IMAGE, null, a);
    }
  }

  public synchronized void deleteDish(String id) {
    if (dish(id) == null) throw new ValidationException("not_found");
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete(DISH, ID + "=?", new String[] {id});
      queue(db, "dish", id, "delete");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public synchronized void saveCategory(DishCategory c, boolean creating) {
    if (c.name.trim().isEmpty()) throw new ValidationException("name_required");
    if (!creating && category(c.id) == null) throw new ValidationException("not_found");
    for (DishCategory old : categories())
      if (!old.id.equals(c.id) && nameKey(old.name).equals(nameKey(c.name)))
        throw new ValidationException("duplicate_category");
    if (creating) c.id = UUID.randomUUID().toString();
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put(ID, c.id);
      v.put(NAME, c.name.trim());
      v.put(DESCRIPTION, c.description.trim());
      if (db.update(CATEGORY, v, ID + "=?", new String[] {c.id}) == 0)
        db.insertOrThrow(CATEGORY, null, v);
      queue(db, "category", c.id, "upsert");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public synchronized void deleteCategory(String id) {
    if (category(id) == null) throw new ValidationException("not_found");
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      if (DatabaseUtils.longForQuery(
              db,
              "SELECT COUNT(*) FROM " + DISH + " WHERE " + CATEGORY_ID + "=?",
              new String[] {id})
          > 0) throw new ValidationException("category_used");
      db.delete(CATEGORY, ID + "=?", new String[] {id});
      queue(db, "category", id, "delete");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private void queue(SQLiteDatabase db, String entity, String id, String op) {
    ContentValues v = new ContentValues();
    v.put(ENTITY, entity);
    v.put(ENTITY_ID, id);
    v.put(OP, op);
    v.put(REVISION, UUID.randomUUID().toString());
    db.insertWithOnConflict(OUTBOX, null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public synchronized int pending() {
    return (int)
        DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + OUTBOX, null);
  }

  public synchronized List<String[]> pendingItems() {
    List<String[]> r = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query(
                OUTBOX,
                null,
                null,
                null,
                null,
                null,
                "CASE WHEN "
                    + OP
                    + "='delete' AND "
                    + ENTITY
                    + "='dish' THEN 0 WHEN "
                    + OP
                    + "='upsert' AND "
                    + ENTITY
                    + "='category' THEN 1 WHEN "
                    + ENTITY
                    + "='dish' THEN 2 ELSE 3 END")) {
      while (c.moveToNext())
        r.add(new String[] {s(c, ENTITY), s(c, ENTITY_ID), s(c, OP), s(c, REVISION)});
    }
    return r;
  }

  public synchronized void acknowledge(String[] item) {
    getWritableDatabase()
        .delete(
            OUTBOX,
            ENTITY + "=? AND " + ENTITY_ID + "=? AND " + REVISION + "=?",
            new String[] {item[0], item[1], item[3]});
  }

  public synchronized boolean dirty(String entity, String id) {
    return DatabaseUtils.longForQuery(
            getReadableDatabase(),
            "SELECT COUNT(*) FROM " + OUTBOX + " WHERE " + ENTITY + "=? AND " + ENTITY_ID + "=?",
            new String[] {entity, id})
        > 0;
  }

  public synchronized JSONObject json(String entity, String id) throws JSONException {
    JSONObject o = new JSONObject();
    o.put(ID, id);
    if (entity.equals("category")) {
      DishCategory c = category(id);
      if (c == null) return null;
      o.put(NAME, c.name);
      o.put(DESCRIPTION, c.description);
    } else {
      Dish d = dish(id);
      if (d == null) return null;
      o.put(NAME, d.name);
      o.put(PRICE, d.price);
      o.put(TIME, d.preparationTime);
      o.put(AVAILABLE, d.isAvailable);
      o.put(DESCRIPTION, d.description);
      o.put(CATEGORY_ID, d.dishCategoryId);
      JSONArray images = new JSONArray();
      for (DishImage i : d.images) {
        JSONObject im = new JSONObject();
        im.put(URL, i.imageUrl);
        im.put(THUMBNAIL, i.isThumbnail);
        images.put(im);
      }
      o.put(IMAGES, images);
    }
    return o;
  }

  /** Pending local changes win; clean remote records are imported transactionally. */
  public synchronized void importRemote(String entity, JSONObject o) throws JSONException {
    String id = o.getString(ID);
    if (id.trim().isEmpty() || dirty(entity, id)) return;
    if (entity.equals("category")) {
      String name = o.getString(NAME).trim();
      if (name.isEmpty()) throw new ValidationException("name_required");
      ContentValues v = new ContentValues();
      v.put(ID, id);
      v.put(NAME, name);
      v.put(DESCRIPTION, o.optString(DESCRIPTION, ""));
      if (getWritableDatabase().update(CATEGORY, v, ID + "=?", new String[] {id}) == 0)
        getWritableDatabase().insertOrThrow(CATEGORY, null, v);
    } else {
      Dish d = new Dish();
      d.id = id;
      d.name = o.getString(NAME);
      d.price = o.getLong(PRICE);
      d.preparationTime = o.getInt(TIME);
      d.isAvailable = o.getBoolean(AVAILABLE);
      d.description = o.optString(DESCRIPTION, "");
      d.dishCategoryId = o.getString(CATEGORY_ID);
      JSONArray a = o.optJSONArray(IMAGES);
      if (a != null)
        for (int i = 0; i < a.length(); i++) {
          JSONObject im = a.getJSONObject(i);
          d.images.add(new DishImage(im.getString(URL), im.optBoolean(THUMBNAIL, false)));
        }
      validate(d);
      SQLiteDatabase db = getWritableDatabase();
      db.beginTransaction();
      try {
        writeDish(db, d);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }
  }
}
