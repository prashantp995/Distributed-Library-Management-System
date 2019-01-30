import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class MonRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  HashMap<String, HashMap<String, Integer>> data = new HashMap<>();
  HashMap<String, Integer> nameAndQuantity = new HashMap<>();
  HashSet<String> itemIds = new HashSet<>();
  HashSet<String> managerId = new HashSet<>();
  HashSet<String> userId = new HashSet<>();
  ArrayList<String> bookName = new ArrayList<>();
  Logger logger = null;

  protected MonRemoteServiceImpl() throws RemoteException {
    super();
    initManagerID();
    initUserID();
    try {
      logger = Utilities
          .setupLogger(Logger.getLogger("MontrealServerLog"), "MontrealServerLog.log");
    } catch (IOException e) {
      e.printStackTrace();
    }


  }

  private void initManagerID() {
    managerId.add("MONM1111");
    managerId.add("MONM1112");
  }

  private void initUserID() {
    userId.add("MONU1111");
    userId.add("MONU1112");
  }

  private void Initbooks() {
    bookName.add("DSD");
    bookName.add("APP");
    bookName.add("ALGO");
  }


  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    return "Find item is called on Montreal server by " + userId + " for " + itemName;
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return "Return item is called on Montreal server by " + userId + " for " + itemID;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return "Borrow item is called on Montreal server by " + userId + " for " + itemID + " for "
        + numberOfDays;
  }

  @Override
  public String addItem(String userId, String itemID, String itemName, int quantity)
      throws RemoteException {
    logger.info("Add item is called on Montreal server by " + userId + " for " + itemID + " for "
        + quantity + " name " + itemName);
    StringBuilder response = new StringBuilder();
    if (itemIds.add(itemID)) {
      logger.info("Item id is not in existing database, Adding as new Item");
      bookName.add(itemName);
      nameAndQuantity.put(itemName, quantity);
      data.put(itemID, nameAndQuantity);
      response.append("Item Add Success");
    } else {
      logger.info("Item id is  exist in  database, modifying as new Item");
      if (ifitemIdCorrospondsToName()) {

      }
    }
    return response.toString();
  }

  private boolean ifitemIdCorrospondsToName() {
    return true;
  }

  @Override
  public String removeItem(String managerId, String itemId, String quantity) {
    return null;
  }

  @Override
  public String listItem(String managerId) {
    logger.info(managerId + "Requested to View Data");
    if (!isValidManager(managerId)) {
      logger.info(managerId + "is not registered");
      return "ManagerId is not registered";
    }
    return getData(data);
  }

  private String getData(HashMap<String, HashMap<String, Integer>> data) {
    StringBuilder response = new StringBuilder();
    for (Map.Entry<String, HashMap<String, Integer>> letterEntry : data.entrySet()) {
      String letter = letterEntry.getKey();
      response.append("ItemId " + letter);
      for (Map.Entry<String, Integer> nameEntry : letterEntry.getValue().entrySet()) {
        response.append(" IeamName " + nameEntry.getKey());
        response.append(" Quantity " + nameEntry.getValue() + "\n");
      }
    }
    return response.toString();
  }

  private boolean isValidManager(String managerId) {
    return managerId.contains(managerId);
  }

}
