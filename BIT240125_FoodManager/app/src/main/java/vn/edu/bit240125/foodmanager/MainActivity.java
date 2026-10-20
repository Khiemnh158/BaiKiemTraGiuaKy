package vn.edu.bit240125.foodmanager;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import org.json.*;
import vn.edu.bit240125.foodmanager.data.*;
import vn.edu.bit240125.foodmanager.model.*;
import vn.edu.bit240125.foodmanager.sync.SyncEngine;

/** Nguyễn Đức Khiêm - BIT240125. Four native screens sharing a local repository. */
public class MainActivity extends Activity {
  public FoodRepository repo;

  public String syncState() {
    return sync == null ? "local" : sync.state;
  }

  public final java.util.List<AlertDialog> dialogs = new java.util.ArrayList<>();

  public AlertDialog activeDialog() {
    for (int i = dialogs.size() - 1; i >= 0; i--)
      if (dialogs.get(i).isShowing()) return dialogs.get(i);
    return null;
  }

  private AlertDialog display(AlertDialog d) {
    dialogs.add(d);
    d.setOnDismissListener(v -> dialogs.remove(d));
    d.show();
    return d;
  }

  private SyncEngine sync;
  private SharedPreferences prefs;
  private boolean vi = false;
  private final int PINK = Color.rgb(178, 48, 103),
      INK = Color.rgb(58, 39, 50),
      MUTED = Color.rgb(125, 100, 113),
      BG = Color.rgb(255, 246, 250),
      LINE = Color.rgb(239, 218, 229),
      ORANGE = Color.rgb(180, 92, 38);
  private LinearLayout root, body, rows;
  private TextView status, count, filterSummary;
  public String screen = "list", selectedId = null;
  private String query = "", filterCategory = "";
  private Long filterMin = null, filterMax = null;
  private boolean filterSelling = false;
  private int sort = 0;
  private Dish draft;
  private boolean creating = false;
  private EditText nameInput, priceInput, timeInput, descriptionInput;
  private Spinner categoryInput;
  private Switch availableInput;
  private List<DishCategory> formCategories;
  private LinearLayout imageRows;
  private boolean suppressSearch = false;

  public String t(String key) {
    return Texts.get(key, vi);
  }

  private int dp(float value) {
    return (int) (getResources().getDisplayMetrics().density * value + .5f);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(PINK);
    getWindow().setNavigationBarColor(BG);
    prefs = getSharedPreferences("BIT240125_Settings", 0);
    vi = prefs.getBoolean("BIT240125_Vietnamese", false);
    query = prefs.getString("BIT240125_Query", "");
    filterCategory = prefs.getString("BIT240125_FilterCategory", "");
    filterSelling = prefs.getBoolean("BIT240125_FilterSelling", false);
    sort = prefs.getInt("BIT240125_Sort", 0);
    if (prefs.contains("BIT240125_FilterMin")) filterMin = prefs.getLong("BIT240125_FilterMin", 0);
    if (prefs.contains("BIT240125_FilterMax")) filterMax = prefs.getLong("BIT240125_FilterMax", 0);
    repo = new FoodRepository(this);
    repo.getWritableDatabase();
    sync =
        new SyncEngine(
            this,
            repo,
            () -> {
              if (status != null) status.setText(t(sync == null ? "local" : sync.state));
              if (screen.equals("list") && rows != null) renderRows();
            });
    if (state != null) {
      selectedId = state.getString("selectedId");
      screen = state.getString("screen", "list");
      creating = state.getBoolean("creating");
      if (state.containsKey("draft"))
        try {
          draft = readDraft(new JSONObject(state.getString("draft")));
        } catch (Exception ignored) {
        }
    } else if (getIntent().hasExtra("dishId")) {
      selectedId = getIntent().getStringExtra("dishId");
      screen = "detail";
    }
    showCurrent();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    if (screen.equals("form")) captureDraft();
    out.putString("screen", screen);
    out.putString("selectedId", selectedId);
    out.putBoolean("creating", creating);
    if (draft != null) out.putString("draft", draftJson().toString());
    super.onSaveInstanceState(out);
  }

  @Override
  public void onDestroy() {
    if (sync != null) sync.close();
    super.onDestroy();
  }

  private void showCurrent() {
    switch (screen) {
      case "detail":
        showDetail(selectedId);
        break;
      case "form":
        if (draft != null) showForm();
        else showList();
        break;
      case "categories":
        showCategories();
        break;
      case "settings":
        showSettings();
        break;
      default:
        showList();
    }
  }

  private GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private GradientDrawable border() {
    GradientDrawable d = bg(Color.WHITE, 14);
    d.setStroke(dp(1), LINE);
    return d;
  }

  private LinearLayout col() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  private TextView text(String value, int size, int color, boolean bold) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setFontFeatureSettings("kern");
    if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    return v;
  }

  private void gap(LinearLayout parent, int height) {
    View v = new View(this);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
  }

  private Button button(String label, String tag, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTag(tag);
    b.setAllCaps(false);
    b.setTextSize(13);
    b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    b.setTextColor(primary ? Color.WHITE : PINK);
    b.setBackground(bg(primary ? PINK : Color.rgb(249, 224, 235), 10));
    b.setMinHeight(dp(42));
    b.setMinimumHeight(dp(42));
    b.setPadding(dp(12), dp(4), dp(12), dp(4));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void pair(LinearLayout parent, Button a, Button b) {
    LinearLayout r = row();
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
    r.addView(a, lp);
    View g = new View(this);
    r.addView(g, new LinearLayout.LayoutParams(dp(8), 1));
    r.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
    parent.addView(r);
  }

  private void page(String title, boolean back) {
    root = col();
    root.setBackgroundColor(BG);
    setContentView(root);
    status = null;
    rows = null;
    LinearLayout header = col();
    header.setPadding(dp(18), dp(12), dp(18), dp(12));
    header.setBackgroundColor(PINK);
    LinearLayout r = row();
    TextView heading = text(title, 22, Color.WHITE, true);
    r.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    Button lang =
        button(
            vi ? "EN" : "VI",
            "language",
            false,
            () -> {
              if (screen.equals("form")) captureDraft();
              vi = !vi;
              prefs.edit().putBoolean("BIT240125_Vietnamese", vi).apply();
              showCurrent();
            });
    lang.setContentDescription(t("language"));
    r.addView(lang, new LinearLayout.LayoutParams(dp(54), dp(38)));
    header.addView(r);
    TextView identity = text(t("identity"), 11, Color.rgb(255, 224, 239), false);
    identity.setPadding(0, dp(5), 0, 0);
    header.addView(identity);
    root.addView(header);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    body = col();
    body.setPadding(dp(18), dp(16), dp(18), dp(22));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (back) {
      Button b = button("‹ " + t("back"), "back", false, this::showList);
      body.addView(b, new LinearLayout.LayoutParams(-2, dp(38)));
      gap(body, 14);
    }
  }

  public void showList() {
    screen = "list";
    selectedId = null;
    page(t("app"), false);
    TextView eyebrow = text(t("kitchen"), 10, PINK, true);
    eyebrow.setLetterSpacing(.12f);
    body.addView(eyebrow);
    gap(body, 5);
    body.addView(text(t("catalog"), 26, INK, true));
    gap(body, 3);
    body.addView(text(t("intro"), 12, MUTED, false));
    gap(body, 12);
    EditText search = edit(t("search"), "search", false);
    search.setSingleLine(true);
    search.setText(query);
    body.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!suppressSearch) {
              query = s.toString();
              persistFilters();
              renderRows();
            }
          }

          public void afterTextChanged(Editable e) {}
        });
    gap(body, 9);
    pair(
        body,
        button(t("filters"), "filters", false, this::filtersDialog),
        button(sortLabel(), "sort", false, this::sortDialog));
    gap(body, 8);
    filterSummary = text("", 11, MUTED, false);
    filterSummary.setTag("filter_summary");
    body.addView(filterSummary);
    gap(body, 8);
    pair(
        body,
        button("+ " + t("add"), "add_dish", true, () -> beginForm(null)),
        button(t("categories"), "categories", false, this::showCategories));
    gap(body, 14);
    LinearLayout stat = row();
    count = text("", 12, INK, true);
    count.setTag("result_count");
    stat.addView(count, new LinearLayout.LayoutParams(0, -2, 1));
    Button settings = button(t("settings"), "settings", false, this::showSettings);
    stat.addView(settings, new LinearLayout.LayoutParams(-2, dp(34)));
    body.addView(stat);
    gap(body, 6);
    status = text(t(sync == null ? "local" : sync.state), 10, MUTED, false);
    status.setTag("sync_status");
    body.addView(status);
    gap(body, 10);
    rows = col();
    body.addView(rows);
    renderRows();
    root.setFocusableInTouchMode(true);
    root.requestFocus();
  }

  private String money(long value) {
    return String.format(Locale.US, "%,d", value).replace(',', '.') + " ₫";
  }

  private String sortLabel() {
    return t(
        new String[] {"sort_name", "sort_price_asc", "sort_price_desc", "sort_time"}
            [Math.max(0, Math.min(3, sort))]);
  }

  private void renderRows() {
    if (rows == null || !screen.equals("list")) return;
    rows.removeAllViews();
    List<Dish> list = repo.dishes(query, filterCategory, filterSelling, filterMin, filterMax, sort);
    count.setText(list.size() + " " + t("results"));
    List<String> active = new ArrayList<>();
    if (!filterCategory.isEmpty()) {
      DishCategory cat = repo.category(filterCategory);
      if (cat != null) active.add(cat.name);
    }
    if (filterSelling) active.add(t("available"));
    if (filterMin != null || filterMax != null)
      active.add(
          (filterMin == null ? "0" : money(filterMin))
              + " – "
              + (filterMax == null ? "∞" : money(filterMax)));
    filterSummary.setText(active.isEmpty() ? t("all_categories") : String.join(" · ", active));
    if (list.isEmpty()) {
      LinearLayout empty = col();
      empty.setPadding(dp(16), dp(24), dp(16), dp(24));
      empty.setBackground(border());
      empty.addView(text(t("empty"), 16, INK, true));
      gap(empty, 7);
      empty.addView(text(t("empty_hint"), 12, MUTED, false));
      gap(empty, 12);
      empty.addView(button(t("clear_filters"), "clear_filters", false, this::clearFilters));
      rows.addView(empty);
      return;
    }
    for (Dish d : list) {
      LinearLayout card = row();
      card.setPadding(dp(9), dp(9), dp(10), dp(9));
      card.setBackground(border());
      card.setTag("dish_" + d.id);
      card.setContentDescription(d.name);
      card.setOnClickListener(v -> showDetail(d.id));
      ImageView image = new ImageView(this);
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setBackground(bg(LINE, 10));
      card.addView(image, new LinearLayout.LayoutParams(dp(78), dp(88)));
      ImageLoader.load(this, d.thumbnail(), image);
      LinearLayout info = col();
      info.setPadding(dp(11), 0, 0, 0);
      info.addView(text(d.name, 15, INK, true));
      gap(info, 4);
      DishCategory category = repo.category(d.dishCategoryId);
      info.addView(text(category == null ? "" : category.name, 10, MUTED, false));
      gap(info, 5);
      info.addView(
          text(money(d.price) + "  ·  " + d.preparationTime + " " + t("minutes"), 12, PINK, true));
      gap(info, 4);
      info.addView(
          text(
              t(d.isQuick() ? "quick" : "regular")
                  + " · "
                  + t(d.isAvailable ? "available" : "unavailable"),
              10,
              d.isAvailable ? MUTED : ORANGE,
              false));
      card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
      rows.addView(card, new LinearLayout.LayoutParams(-1, -2));
      gap(rows, 9);
    }
  }

  private void persistFilters() {
    SharedPreferences.Editor e =
        prefs
            .edit()
            .putString("BIT240125_Query", query)
            .putString("BIT240125_FilterCategory", filterCategory)
            .putBoolean("BIT240125_FilterSelling", filterSelling)
            .putInt("BIT240125_Sort", sort);
    if (filterMin == null) e.remove("BIT240125_FilterMin");
    else e.putLong("BIT240125_FilterMin", filterMin);
    if (filterMax == null) e.remove("BIT240125_FilterMax");
    else e.putLong("BIT240125_FilterMax", filterMax);
    e.apply();
  }

  private void clearFilters() {
    query = "";
    filterCategory = "";
    filterSelling = false;
    filterMin = null;
    filterMax = null;
    sort = 0;
    persistFilters();
    showList();
  }

  private void filtersDialog() {
    LinearLayout form = dialogForm();
    List<DishCategory> cats = repo.categories();
    List<String> labels = new ArrayList<>();
    labels.add(t("all_categories"));
    int selected = 0;
    for (DishCategory c : cats) {
      labels.add(c.name);
      if (c.id.equals(filterCategory)) selected = labels.size() - 1;
    }
    label(form, t("category"));
    Spinner category = spinner(labels, "filter_category");
    category.setSelection(selected);
    form.addView(category);
    gap(form, 8);
    Switch available = new Switch(this);
    available.setText(t("selling_only"));
    available.setTag("filter_available");
    available.setTextColor(INK);
    available.setTextSize(14);
    available.setChecked(filterSelling);
    form.addView(available);
    gap(form, 10);
    EditText min = field(form, t("min_price"), "filter_min", true),
        max = field(form, t("max_price"), "filter_max", true);
    if (filterMin != null) min.setText("" + filterMin);
    if (filterMax != null) max.setText("" + filterMax);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(t("filters"))
            .setView(form)
            .setPositiveButton(t("apply"), null)
            .setNeutralButton(t("reset"), null)
            .setNegativeButton(t("cancel"), null)
            .create();
    dialog.setOnShowListener(
        v -> {
          dialog.getButton(-1).setTag("apply_filters");
          dialog
              .getButton(-1)
              .setOnClickListener(
                  b -> {
                    try {
                      Long a = parseRange(min), z = parseRange(max);
                      if ((a != null && a < 0)
                          || (z != null && z < 0)
                          || (a != null && z != null && a > z))
                        throw new ValidationException("range_error");
                      filterMin = a;
                      filterMax = z;
                      filterSelling = available.isChecked();
                      filterCategory =
                          category.getSelectedItemPosition() == 0
                              ? ""
                              : cats.get(category.getSelectedItemPosition() - 1).id;
                      persistFilters();
                      dialog.dismiss();
                      showList();
                    } catch (Exception e) {
                      error("range_error");
                    }
                  });
          dialog
              .getButton(-3)
              .setOnClickListener(
                  b -> {
                    dialog.dismiss();
                    clearFilters();
                  });
        });
    display(dialog);
  }

  private Long parseRange(EditText field) {
    String s = field.getText().toString().trim();
    return s.isEmpty() ? null : Long.parseLong(s);
  }

  private void sortDialog() {
    String[] labels = {t("sort_name"), t("sort_price_asc"), t("sort_price_desc"), t("sort_time")};
    display(
        new AlertDialog.Builder(this)
            .setTitle(t("sort"))
            .setSingleChoiceItems(
                labels,
                sort,
                (d, index) -> {
                  sort = index;
                  persistFilters();
                  d.dismiss();
                  showList();
                })
            .setNegativeButton(t("cancel"), null)
            .create());
  }

  public void showDetail(String id) {
    Dish d = repo.dish(id);
    if (d == null) {
      showList();
      error("not_found");
      return;
    }
    selectedId = id;
    screen = "detail";
    page(t("detail"), true);
    if (!d.images.isEmpty()) {
      ImageView hero = new ImageView(this);
      hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
      hero.setBackground(border());
      body.addView(hero, new LinearLayout.LayoutParams(-1, dp(170)));
      ImageLoader.load(this, d.thumbnail(), hero);
      gap(body, 14);
    }
    body.addView(text(d.name, 25, INK, true));
    gap(body, 6);
    DishCategory category = repo.category(d.dishCategoryId);
    body.addView(text(category == null ? "" : category.name, 12, MUTED, false));
    gap(body, 12);
    LinearLayout summary = col();
    summary.setPadding(dp(14), dp(14), dp(14), dp(14));
    summary.setBackground(border());
    summary.addView(text(money(d.price), 24, PINK, true));
    gap(summary, 5);
    summary.addView(
        text(
            d.preparationTime
                + " "
                + t("minutes")
                + " · "
                + t(d.isQuick() ? "quick" : "regular")
                + " · "
                + t(d.isAvailable ? "available" : "unavailable"),
            13,
            d.isAvailable ? MUTED : ORANGE,
            true));
    body.addView(summary);
    gap(body, 16);
    body.addView(text(t("description"), 15, INK, true));
    gap(body, 6);
    body.addView(
        text(d.description.isEmpty() ? t("no_description") : d.description, 14, MUTED, false));
    gap(body, 16);
    body.addView(
        text(t("images") + " · " + d.images.size() + " " + t("photos_count"), 15, INK, true));
    gap(body, 9);
    if (d.images.isEmpty()) body.addView(text(t("no_images"), 12, MUTED, false));
    for (int index = 0; index < d.images.size(); index++) {
      int pos = index;
      DishImage im = d.images.get(index);
      LinearLayout r = row();
      r.setPadding(dp(8), dp(8), dp(8), dp(8));
      r.setBackground(border());
      ImageView picture = new ImageView(this);
      picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
      r.addView(picture, new LinearLayout.LayoutParams(dp(88), dp(66)));
      ImageLoader.load(this, im.imageUrl, picture);
      LinearLayout desc = col();
      desc.setPadding(dp(12), 0, 0, 0);
      desc.addView(text("#" + (index + 1), 11, MUTED, false));
      Button cover =
          button(
              t(im.isThumbnail ? "cover" : "make_cover"),
              "detail_cover_" + index,
              im.isThumbnail,
              () -> {
                for (int j = 0; j < d.images.size(); j++) d.images.get(j).isThumbnail = j == pos;
                try {
                  repo.saveDish(d, false);
                  sync.request();
                  showDetail(d.id);
                } catch (Exception e) {
                  handle(e);
                }
              });
      desc.addView(cover, new LinearLayout.LayoutParams(-1, dp(37)));
      r.addView(desc, new LinearLayout.LayoutParams(0, -2, 1));
      body.addView(r);
      gap(body, 7);
    }
    gap(body, 12);
    pair(
        body,
        button(t("edit"), "edit_dish", true, () -> beginForm(d.id)),
        button(t("delete"), "delete_dish", false, () -> confirmDishDelete(d.id)));
  }

  private void confirmDishDelete(String id) {
    display(
        new AlertDialog.Builder(this)
            .setTitle(t("delete"))
            .setMessage(t("confirm_delete"))
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(
                t("delete"),
                (dialog, which) -> {
                  try {
                    repo.deleteDish(id);
                    sync.request();
                    showList();
                    toast("deleted");
                  } catch (Exception e) {
                    handle(e);
                  }
                })
            .create());
  }

  public void beginForm(String id) {
    creating = id == null;
    draft = creating ? new Dish() : repo.dish(id);
    if (draft == null) {
      error("not_found");
      return;
    }
    if (creating) {
      List<DishCategory> cats = repo.categories();
      if (cats.isEmpty()) {
        showCategories();
        error("no_categories");
        return;
      }
      draft.dishCategoryId = cats.get(0).id;
    }
    screen = "form";
    showForm();
  }

  private EditText edit(String hint, String tag, boolean numeric) {
    EditText e = new EditText(this);
    e.setTag(tag);
    e.setTextSize(14);
    e.setTextColor(INK);
    e.setHintTextColor(MUTED);
    e.setHint(hint);
    e.setPadding(dp(12), dp(9), dp(12), dp(9));
    e.setBackground(border());
    if (numeric) e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    return e;
  }

  private void label(LinearLayout form, String title) {
    TextView l = text(title, 12, INK, true);
    l.setPadding(0, dp(7), 0, dp(6));
    form.addView(l);
  }

  private EditText field(LinearLayout form, String title, String tag, boolean numeric) {
    label(form, title);
    EditText e = edit(title, tag, numeric);
    e.setSingleLine(true);
    form.addView(e, new LinearLayout.LayoutParams(-1, dp(47)));
    gap(form, 6);
    return e;
  }

  private Spinner spinner(List<String> values, String tag) {
    Spinner s = new Spinner(this);
    s.setTag(tag);
    ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(a);
    s.setBackground(border());
    s.setPadding(dp(8), 0, dp(8), 0);
    s.setMinimumHeight(dp(46));
    return s;
  }

  private void showForm() {
    screen = "form";
    page(t(creating ? "add" : "edit"), true);
    nameInput = field(body, t("name"), "dish_name", false);
    nameInput.setText(draft.name);
    priceInput = field(body, t("price"), "dish_price", true);
    priceInput.setText(draft.price > 0 ? "" + draft.price : "");
    timeInput = field(body, t("time"), "dish_time", true);
    timeInput.setText(draft.preparationTime > 0 ? "" + draft.preparationTime : "");
    label(body, t("category"));
    formCategories = repo.categories();
    List<String> labels = new ArrayList<>();
    int selection = 0;
    for (DishCategory c : formCategories) {
      labels.add(c.name);
      if (c.id.equals(draft.dishCategoryId)) selection = labels.size() - 1;
    }
    categoryInput = spinner(labels, "dish_category");
    categoryInput.setSelection(selection);
    body.addView(categoryInput, new LinearLayout.LayoutParams(-1, dp(47)));
    gap(body, 12);
    availableInput = new Switch(this);
    availableInput.setTag("dish_available");
    availableInput.setText(t("available"));
    availableInput.setTextColor(INK);
    availableInput.setChecked(draft.isAvailable);
    body.addView(availableInput);
    gap(body, 5);
    descriptionInput = field(body, t("description"), "dish_description", false);
    descriptionInput.setSingleLine(false);
    descriptionInput.setMinLines(2);
    descriptionInput.setText(draft.description);
    descriptionInput.getLayoutParams().height = dp(80);
    gap(body, 12);
    body.addView(text(t("images"), 16, INK, true));
    gap(body, 5);
    body.addView(text(t("photo_hint"), 11, MUTED, false));
    gap(body, 9);
    imageRows = col();
    body.addView(imageRows);
    renderDraftImages();
    gap(body, 8);
    body.addView(
        button("+ " + t("add_image"), "add_image", false, this::addImageDialog),
        new LinearLayout.LayoutParams(-1, dp(43)));
    gap(body, 18);
    body.addView(
        button(t("save"), "save_dish", true, this::saveDraft),
        new LinearLayout.LayoutParams(-1, dp(48)));
    gap(body, 8);
    root.setFocusableInTouchMode(true);
    root.requestFocus();
  }

  private void captureDraft() {
    if (nameInput == null || draft == null) return;
    draft.name = nameInput.getText().toString();
    draft.description = descriptionInput.getText().toString();
    try {
      draft.price = Long.parseLong(priceInput.getText().toString().trim());
    } catch (Exception e) {
      draft.price = 0;
    }
    try {
      draft.preparationTime = Integer.parseInt(timeInput.getText().toString().trim());
    } catch (Exception e) {
      draft.preparationTime = 0;
    }
    draft.isAvailable = availableInput.isChecked();
    int index = categoryInput.getSelectedItemPosition();
    draft.dishCategoryId =
        index >= 0 && index < formCategories.size() ? formCategories.get(index).id : "";
  }

  private void renderDraftImages() {
    imageRows.removeAllViews();
    if (draft.images.isEmpty()) {
      imageRows.addView(text(t("no_images"), 12, MUTED, false));
      return;
    }
    for (int i = 0; i < draft.images.size(); i++) {
      int index = i;
      DishImage image = draft.images.get(i);
      LinearLayout r = col();
      r.setPadding(dp(8), dp(8), dp(8), dp(8));
      r.setBackground(border());
      TextView url = text(image.imageUrl, 10, MUTED, false);
      url.setMaxLines(2);
      url.setEllipsize(TextUtils.TruncateAt.END);
      r.addView(url);
      gap(r, 6);
      pair(
          r,
          button(
              t(image.isThumbnail ? "cover" : "make_cover"),
              "form_cover_" + index,
              image.isThumbnail,
              () -> {
                for (int j = 0; j < draft.images.size(); j++)
                  draft.images.get(j).isThumbnail = j == index;
                renderDraftImages();
              }),
          button(
              t("remove"),
              "remove_image_" + index,
              false,
              () -> {
                draft.images.remove(index);
                renderDraftImages();
              }));
      imageRows.addView(r);
      gap(imageRows, 7);
    }
  }

  private void addImageDialog() {
    LinearLayout form = dialogForm();
    EditText url = field(form, t("url"), "image_url", false);
    url.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(t("add_image"))
            .setView(form)
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(t("add_url"), null)
            .create();
    dialog.setOnShowListener(
        v -> {
          dialog.getButton(-1).setTag("confirm_add_image");
          dialog
              .getButton(-1)
              .setOnClickListener(
                  b -> {
                    String value = url.getText().toString().trim();
                    if (!FoodRepository.validUrl(value)) {
                      error("url_error");
                      return;
                    }
                    for (DishImage im : draft.images)
                      if (im.imageUrl.equals(value)) {
                        error("duplicate_image");
                        return;
                      }
                    draft.images.add(new DishImage(value, draft.images.isEmpty()));
                    dialog.dismiss();
                    renderDraftImages();
                  });
        });
    display(dialog);
  }

  private void saveDraft() {
    captureDraft();
    try {
      String id = repo.saveDish(draft, creating);
      hideKeyboard();
      sync.request();
      draft = null;
      showDetail(id);
      toast("saved");
    } catch (Exception e) {
      handle(e);
    }
  }

  private void hideKeyboard() {
    View focus = getCurrentFocus();
    if (focus != null)
      ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(focus.getWindowToken(), 0);
  }

  private LinearLayout dialogForm() {
    LinearLayout l = col();
    l.setPadding(dp(22), dp(8), dp(22), dp(14));
    return l;
  }

  public void showCategories() {
    screen = "categories";
    page(t("manage_categories"), true);
    body.addView(text(t("categories"), 25, INK, true));
    gap(body, 12);
    body.addView(
        button("+ " + t("add_category"), "add_category", true, () -> categoryDialog(null)),
        new LinearLayout.LayoutParams(-1, dp(45)));
    gap(body, 15);
    List<DishCategory> cats = repo.categories();
    if (cats.isEmpty()) body.addView(text(t("no_categories"), 14, MUTED, false));
    for (DishCategory c : cats) {
      LinearLayout card = col();
      card.setPadding(dp(13), dp(12), dp(13), dp(12));
      card.setBackground(border());
      card.addView(text(c.name, 18, INK, true));
      gap(card, 4);
      card.addView(text(c.description, 12, MUTED, false));
      gap(card, 11);
      pair(
          card,
          button(t("edit_category"), "edit_category_" + c.id, false, () -> categoryDialog(c)),
          button(
              t("delete"),
              "delete_category_" + c.id,
              false,
              () ->
                  display(
                      new AlertDialog.Builder(this)
                          .setTitle(t("delete"))
                          .setMessage(t("confirm_category"))
                          .setNegativeButton(t("cancel"), null)
                          .setPositiveButton(
                              t("delete"),
                              (di, w) -> {
                                try {
                                  repo.deleteCategory(c.id);
                                  sync.request();
                                  showCategories();
                                } catch (Exception e) {
                                  handle(e);
                                }
                              })
                          .create())));
      body.addView(card);
      gap(body, 10);
    }
  }

  private void categoryDialog(DishCategory old) {
    LinearLayout form = dialogForm();
    EditText name = field(form, t("category_name"), "category_name", false),
        description = field(form, t("category_description"), "category_description", false);
    if (old != null) {
      name.setText(old.name);
      description.setText(old.description);
    }
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(t(old == null ? "add_category" : "edit_category"))
            .setView(form)
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(t("save_category"), null)
            .create();
    dialog.setOnShowListener(
        v -> {
          dialog.getButton(-1).setTag("save_category");
          dialog
              .getButton(-1)
              .setOnClickListener(
                  b -> {
                    try {
                      repo.saveCategory(
                          new DishCategory(
                              old == null ? null : old.id,
                              name.getText().toString(),
                              description.getText().toString()),
                          old == null);
                      hideKeyboard();
                      dialog.dismiss();
                      sync.request();
                      showCategories();
                    } catch (Exception e) {
                      handle(e);
                    }
                  });
        });
    display(dialog);
  }

  public void showSettings() {
    screen = "settings";
    page(t("settings"), true);
    body.addView(text(t("sync_settings"), 23, INK, true));
    gap(body, 10);
    body.addView(text(t("sync_help"), 14, MUTED, false));
    gap(body, 10);
    body.addView(text(t(sync.configured() ? "configured" : "not_configured"), 12, PINK, true));
    gap(body, 8);
    EditText cats = field(body, t("category_endpoint"), "category_endpoint", false),
        dishes = field(body, t("dish_endpoint"), "dish_endpoint", false);
    cats.setInputType(17);
    dishes.setInputType(17);
    cats.setText(prefs.getString("BIT240125_CategoryEndpoint", ""));
    dishes.setText(prefs.getString("BIT240125_DishEndpoint", ""));
    gap(body, 12);
    body.addView(
        button(
            t("save_settings"),
            "save_settings",
            true,
            () -> {
              String a = cats.getText().toString().trim().replaceAll("/+$", ""),
                  b = dishes.getText().toString().trim().replaceAll("/+$", "");
              if (!(a.isEmpty() && b.isEmpty())
                  && (!mockEndpoint(a) || !mockEndpoint(b) || a.equals(b))) {
                error("endpoint_error");
                return;
              }
              prefs
                  .edit()
                  .putString("BIT240125_CategoryEndpoint", a)
                  .putString("BIT240125_DishEndpoint", b)
                  .apply();
              sync.request();
              hideKeyboard();
              showSettings();
              toast("saved");
            }),
        new LinearLayout.LayoutParams(-1, dp(46)));
    gap(body, 17);
    body.addView(text(t("sync_policy"), 13, MUTED, false));
    gap(body, 12);
    body.addView(text(t("pending_count") + ": " + repo.pending(), 13, INK, true));
    gap(body, 15);
    body.addView(text(t("project"), 13, PINK, true));
    body.addView(text(t("identity"), 12, MUTED, false));
  }

  private boolean mockEndpoint(String value) {
    if (!FoodRepository.validUrl(value)) return false;
    try {
      String host = new java.net.URI(value).getHost();
      return host.equals("mockapi.io") || host.endsWith(".mockapi.io");
    } catch (Exception e) {
      return false;
    }
  }

  private void toast(String key) {
    Toast.makeText(this, t(key), Toast.LENGTH_SHORT).show();
  }

  private void error(String key) {
    display(
        new AlertDialog.Builder(this)
            .setTitle(t("error"))
            .setMessage(t(key))
            .setPositiveButton(t("ok"), null)
            .create());
  }

  private void handle(Exception e) {
    error(e instanceof ValidationException ? ((ValidationException) e).key : "storage_error");
  }

  @Override
  public void onBackPressed() {
    if (!screen.equals("list")) {
      hideKeyboard();
      showList();
    } else super.onBackPressed();
  }

  private JSONObject draftJson() {
    JSONObject o = new JSONObject();
    try {
      o.put("id", draft.id);
      o.put("name", draft.name);
      o.put("price", draft.price);
      o.put("time", draft.preparationTime);
      o.put("available", draft.isAvailable);
      o.put("description", draft.description);
      o.put("category", draft.dishCategoryId);
      JSONArray a = new JSONArray();
      for (DishImage im : draft.images) {
        JSONObject i = new JSONObject();
        i.put("url", im.imageUrl);
        i.put("cover", im.isThumbnail);
        a.put(i);
      }
      o.put("images", a);
    } catch (Exception ignored) {
    }
    return o;
  }

  private Dish readDraft(JSONObject o) throws JSONException {
    Dish d = new Dish();
    d.id = o.optString("id", null);
    d.name = o.getString("name");
    d.price = o.getLong("price");
    d.preparationTime = o.getInt("time");
    d.isAvailable = o.getBoolean("available");
    d.description = o.getString("description");
    d.dishCategoryId = o.getString("category");
    JSONArray a = o.getJSONArray("images");
    for (int j = 0; j < a.length(); j++) {
      JSONObject i = a.getJSONObject(j);
      d.images.add(new DishImage(i.getString("url"), i.getBoolean("cover")));
    }
    return d;
  }
}
