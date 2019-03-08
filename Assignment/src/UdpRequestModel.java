import java.io.Serializable;

public class UdpRequestModel implements Serializable {

  String methodName;
  String itemName;
  String itemId;
  int numberOfDays;
  String userId;

  public UdpRequestModel(String findItem, String itemName) {
    this.methodName = findItem;
    this.itemName = itemName;
  }

  public UdpRequestModel() {

  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public int getNumberOfDays() {
    return numberOfDays;
  }

  public void setNumberOfDays(int numberOfDays) {
    this.numberOfDays = numberOfDays;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public UdpRequestModel(String methodToExecuteOnExternalServer, String itemId, int numberOfDays, String userId) {
    this.methodName = methodToExecuteOnExternalServer;
    this.itemId = itemId;
    this.numberOfDays = numberOfDays;
    this.userId = userId;
  }

  public UdpRequestModel(String methodName, String itemName, String userId) {
    this.methodName = methodName;
    if (methodName.equalsIgnoreCase("returnItem")) {
      this.itemId = itemName;
    } else {
      this.itemName = itemName;
    }
    this.userId = userId;
  }


}
