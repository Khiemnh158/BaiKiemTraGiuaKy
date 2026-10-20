package vn.edu.bit240125.foodmanager.data;

/** EVERY persisted SQL table and column is prefixed with the student's MSSV. */
public final class Contract {
  private Contract() {}

  public static final String P = "BIT240125_";
  public static final String CATEGORY = P + "DishCategory",
      DISH = P + "Dish",
      IMAGE = P + "DishImage",
      OUTBOX = P + "Outbox";
  public static final String ID = P + "Id",
      NAME = P + "Name",
      NAME_KEY = P + "NameKey",
      DESCRIPTION = P + "Description";
  public static final String PRICE = P + "Price",
      TIME = P + "PreparationTime",
      AVAILABLE = P + "IsAvailable",
      CATEGORY_ID = P + "DishCategoryId";
  public static final String DISH_ID = P + "DishId",
      URL = P + "ImageUrl",
      THUMBNAIL = P + "IsThumbnail",
      IMAGES = P + "DishImage";
  public static final String ENTITY = P + "Entity",
      ENTITY_ID = P + "EntityId",
      OP = P + "Operation",
      REVISION = P + "Revision";
}
