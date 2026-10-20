package vn.edu.bit240125.foodmanager.model;

import java.util.ArrayList;
import java.util.List;

public class Dish {
  public String id, name = "", description = "", dishCategoryId = "";
  public long price;
  public int preparationTime;
  public boolean isAvailable = true;
  public List<DishImage> images = new ArrayList<>();

  public boolean isQuick() {
    return preparationTime <= 15;
  }

  public String thumbnail() {
    for (DishImage i : images) if (i.isThumbnail) return i.imageUrl;
    return images.isEmpty() ? null : images.get(0).imageUrl;
  }
}
