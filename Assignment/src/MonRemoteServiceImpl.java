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
    if (!isValidManager(userId)) {
      logger.info(userId + "is not present/authorised");
      return "Item Add Fails,User is not Present/Authorised";
    }
    if (itemIds.add(itemID)) {
      logger.info("Item id is not in existing database, Adding as new Item");
      bookName.add(itemName);
      HashMap<String, Integer> nameQuantity = new HashMap<>();
      nameQuantity.put(itemName, quantity);
      data.put(itemID, nameQuantity);
      response.append("Item Add Success");
    } else {
      logger.info("Item id   exist in  database, modifying as new Item");
      if (validateItemIdAndName(itemID, itemName)) {
        logger.info("Item id and Item name matches");
        HashMap<String, Integer> nameQuantity = new HashMap<>();
        nameQuantity.put(itemName, quantity);
        data.put(itemID, nameQuantity);
        response.append("Item add success, Quantity updated");
      } else {
        response.append("Item Add Fails , Item Id and Name Does not match");
      }
    }
    return response.toString();
  }

  private boolean validateItemIdAndName(String itemID, String itemName) {
    for (Map.Entry<String, HashMap<String, Integer>> letterEntry : data.entrySet()) {
      String id = letterEntry.getKey();
      if (id.equals(itemID)) {
        for (Map.Entry<String, Integer> nameEntry : letterEntry.getValue().entrySet()) {
          String name = nameEntry.getKey();
          if (name.equals(itemName)) {
            return true;
          }

        }
      }

    }
    return false;
  }

  @Override
  public String removeItem(String managerId, String itemId, int quantity) {
    logger.info(
        "Remove item is called on Montreal server by " + managerId + " for " + itemId + " for "
            + quantity);
    StringBuilder response = new StringBuilder();
    if (!isValidManager(managerId)) {
      logger.info(managerId + "is not Present/Authorised");
      return "Item Remove Fails,User is not Present/Authorised";
    }
    if (quantity < 0) {
      logger.info(managerId + "is not Present/Authorised");
      return "Item Remove Fails,Quantity is not valid";
    }
    if (!data.containsKey(itemId)) {
      response.append("Remove Item Fails , Item id is not present in database");
    } else {
      if (quantity == 0) {
        removeItemCompletely(itemId);
        response.append("Remove Item Success , Item Removed Completely");
      } else {
        updateQuantity(itemId, quantity);
        response.append("Remove Item Success , Updated the Quantity");
      }
    }
    return response.toString();
  }

  private synchronized void updateQuantity(String itemId, int quantity) {
    LibraryModel libraryModel = getData(data, itemId);
    logger.info("updating existing item " + libraryModel);
    HashMap<String, Integer> nameQuantity = new HashMap<>();
    nameQuantity.put(libraryModel.getItemName(), quantity);
    data.put(itemId, nameQuantity);
  }

  private synchronized void removeItemCompletely(String itemId) {
    LibraryModel libraryModel = getData(data, itemId);
    logger.info("removing" + libraryModel);
    data.remove(libraryModel.getItemId());
    itemIds.remove(libraryModel.getItemId());
    bookName.remove(libraryModel.getItemName());
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

  private synchronized LibraryModel getData(HashMap<String, HashMap<String, Integer>> data,
      String itemId) {
    StringBuilder response = new StringBuilder();
    LibraryModel libraryModel = new LibraryModel();
    HashMap<String, Integer> nameQuantity = data.get(itemId);
    libraryModel.setItemId(itemId);
    for (Map.Entry<String, Integer> nameEntry : nameQuantity.entrySet()) {
      libraryModel.setItemName(nameEntry.getKey());
      libraryModel.setQuantity(nameEntry.getValue());
    }

    return libraryModel;
  }

  private boolean isValidManager(String managerId) {
    return managerId.contains(managerId);
  }

}
