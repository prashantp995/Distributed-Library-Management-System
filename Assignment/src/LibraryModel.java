import java.util.ArrayList;
import java.util.List;

public class LibraryModel {

  String itemId;
  String itemName;
  List<String> waitingList = new ArrayList<String>();
  List<String> currentBorrowerList = new ArrayList<String>();
  int quantity;


  public LibraryModel(String itemId, String itemName, int quantity) {
    this.itemId = itemId;
    this.itemName = itemName;
    this.quantity = quantity;
  }

  public LibraryModel() {

  }

  public LibraryModel(String itemName, int quantity) {
    this.itemName = itemName;
    this.quantity = quantity;

  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public List<String> getWaitingList() {
    return waitingList;
  }

  public void setWaitingList(List<String> waitingList) {
    this.waitingList = waitingList;
  }

  public List<String> getCurrentBorrowerList() {
    return currentBorrowerList;
  }

  public void setCurrentBorrowerList(List<String> currentBorrowerList) {
    this.currentBorrowerList = currentBorrowerList;
  }

  @Override
  public String toString() {
    return "{" +
        ", itemName='" + itemName + '\'' +
        ", quantity=" + quantity +
        '}';
  }
}
