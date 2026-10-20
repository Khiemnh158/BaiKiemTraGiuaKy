package vn.edu.bit240125.foodmanager.sync;

/** Resource URLs of this assignment project; these are public MockAPI endpoints, not secrets. */
public final class SyncConfig {
  public static final String BASE = "https://6ad71854f71f154ac487c192.mockapi.io/api/v1/";
  public static final String CATEGORIES = BASE + "BIT240125_DishCategory";
  public static final String DISHES = BASE + "BIT240125_Dish";

  private SyncConfig() {}
}
