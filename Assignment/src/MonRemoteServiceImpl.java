import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class MonRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  HashMap<String, LibraryModel> data = new HashMap<>();
  HashMap<String, ArrayList<String>> currentBorrowers = new HashMap<>();
  HashSet<String> itemIds = new HashSet<>();
  HashSet<String> managerId = new HashSet<>();
  HashSet<String> userIds = new HashSet<>();
  ArrayList<String> bookName = new ArrayList<>();
  Logger logger = null;


  protected MonRemoteServiceImpl() throws RemoteException {
    super();
    initManagerID();
    initUserID();
    data.put("MON1012", new LibraryModel("DSD", 5));
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
    userIds.add("MONU1111");
    userIds.add("MONU1112");
  }

  private void Initbooks() {
    bookName.add("DSD");
    bookName.add("APP");
    bookName.add("ALGO");
  }


  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    logger.info(userId + "requested to find item" + itemName);
    String itemDetails;
    if (!isValidUser(userId)) {
      logger.info(userId + "is not valid/authorized to  find Item" + itemName);
      return userId + "is not valid/authorized to  find Item" + itemName;
    } else {
      itemDetails = findItem(itemName, true);
      if (itemDetails.length() == 0) {
        return "No item found in Montreal Server";
      }
    }
    return itemDetails;
  }

  public String findItem(String itemName, boolean callExternalServers) {
    StringBuilder response = new StringBuilder();
    for (Entry<String, LibraryModel> letterEntry : data.entrySet()) {
      if (letterEntry.getValue().getItemName().equals(itemName)) {
        response.append(letterEntry.getKey() + " ");
        response.append(letterEntry.getValue().getQuantity());
      }
    }
    if (callExternalServers) {
      UdpRequestModel udpRequestModel = new UdpRequestModel("findItem", itemName);
      String concordiaServerResponse = Utilities
          .callUDPServer(udpRequestModel, LibConstants.UDP_CON_PORT, logger);
      if (concordiaServerResponse != null && concordiaServerResponse.length() > 0) {
        response.append("\n" + concordiaServerResponse + "\n");
      }
      String mcGillServerResponse = Utilities
          .callUDPServer(udpRequestModel, LibConstants.UDP_MCG_PORT, logger);
      if (mcGillServerResponse != null && mcGillServerResponse.length() > 0) {
        response.append(mcGillServerResponse + "\n");
      }
    }

    return response.toString();
  }


  private boolean isValidUser(String userId) {
    return userIds.contains(userId);
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return "Return item is called on Montreal server by " + userId + " for " + itemID;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    if (!isValidUser(userId)) {
      logger.info(userId + "is not present/authorised");
      return userId + "is not present/authorised";
    }
    StringBuilder response = new StringBuilder();
    String result = performBorrowItemOperation(itemID, userId,
        numberOfDays);//this will fail if itemID is not in this server
    //if fails then we need to connect to Respective External Server to Borrow Item
    if (result.equals(LibConstants.FAIL)) {
      logger.info(
          "Calling ExternalUDP Servers");
      String res = borrowItemFromExternalServer(userId, itemID, numberOfDays);
      response.append(res);
      if (res.equals(LibConstants.WAIT_LIST_POSSIBLE)) {
        int clientChoice = Utilities.getResponseFromClient(logger);
        if (clientChoice == 1) {
          res = addUserInWaitList(itemID, userId, numberOfDays, true);
        }
      }

      return res;
    }

    return response.toString();
  }

  public String addUserInWaitList(String itemID, String userId, int numberOfDays,
      boolean externalServerCallRequire) {
    String response = null;
    if (externalServerCallRequire) {
      int port = Utilities.getPortFromItemId(itemID);
      UdpRequestModel udpRequestModel = new UdpRequestModel(LibConstants.OPR_WAIT_LIST, itemID,
          numberOfDays, userId);

      if (port != 0) {
        response = Utilities
            .callUDPServer(udpRequestModel, port, logger);
      } else {
        response = "invalid item id";
      }
    } else {
      synchronized (data) {
        LibraryModel libraryModel = data.get(itemID);
        libraryModel.getWaitingList().add(userId);
        data.put(itemID, libraryModel);
        response = LibConstants.SUCCESS;
      }
    }
    return response;
  }


  public synchronized String performBorrowItemOperation(String itemID, String userId,
      int numberOfDays) {
    if (isItemAvailableToBorrow(itemID, userId, numberOfDays)) {
      LibraryModel libraryModel = data.get(itemID);
      libraryModel.getCurrentBorrowerList().add(userId);
      libraryModel.setQuantity(libraryModel.getQuantity() - 1);
      if (!currentBorrowers.containsKey(userId)) {
        ArrayList<String> itemBorrowed = new ArrayList<>();
        itemBorrowed.add(itemID);
        currentBorrowers.put(userId, itemBorrowed);
      } else {
        currentBorrowers.get(userId).add(itemID);
      }
      return LibConstants.SUCCESS;
    } else if (isUserInWaitList(itemID, userId)) {
      return LibConstants.ALREADY_WAITING_LIST;
    } else if (isWaitListPossible(itemID, userId)) {
      return LibConstants.WAIT_LIST_POSSIBLE;

    }
    return LibConstants.FAIL;
  }

  private boolean isWaitListPossible(String itemID, String userId) {
    if (data.containsKey(itemID) && data.get(itemID).getQuantity() == 0) {
      return true;
    }
    return false;
  }

  private boolean isUserInWaitList(String itemID, String userId) {
    if (data.containsKey(itemID) && data.get(itemID).getWaitingList() != null && data.get(itemID)
        .getWaitingList().contains(userId)) {
      return true;
    }
    return false;
  }

  private boolean isItemAvailableToBorrow(String itemID, String userId, int numberOfDays) {
    if (data.containsKey(itemID)) {
      if (data.get(itemID).getQuantity() > 0) {
        //check if user has already borrowed the item
        return !(currentBorrowers.containsKey(userId) && currentBorrowers.get(userId)
            .contains(itemID));
      }
    }
    return false;
  }

  private String borrowItemFromExternalServer(String userId, String itemID, int numberOfDays) {
    UdpRequestModel udpRequestModel = new UdpRequestModel("borrowItem", itemID, numberOfDays,
        userId);
    String response = null;
    int port = Utilities.getPortFromItemId(itemID);
    if (port != 0) {
      response = Utilities
          .callUDPServer(udpRequestModel, port, logger);
    } else {
      response = "invalid item id";
    }
    return response;


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
      LibraryModel libraryModel = new LibraryModel(itemName, quantity);
      data.put(itemID, libraryModel);
      response.append("Item Add Success");
    } else {
      logger.info("Item id   exist in  database, modifying as new Item");
      if (validateItemIdAndName(itemID, itemName)) {
        logger.info("Item id and Item name matches");
        LibraryModel libraryModel = new LibraryModel(itemName, quantity);
        data.put(itemID, libraryModel);
        response.append("Item add success, Quantity updated");
      } else {
        response.append("Item Add Fails , Item Id and Name Does not match");
      }
    }
    return response.toString();
  }

  private boolean validateItemIdAndName(String itemID, String itemName) {
    for (Entry<String, LibraryModel> letterEntry : data.entrySet()) {
      String id = letterEntry.getKey();
      if (id.equals(itemID)) {
        LibraryModel libraryModel = letterEntry.getValue();
        return libraryModel.getItemName().equals(itemName);

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
    LibraryModel libraryModel = data.get(itemId);
    libraryModel.setQuantity(quantity);
    logger.info("updating existing item " + libraryModel);
    data.put(itemId, libraryModel);
  }

  private synchronized void removeItemCompletely(String itemId) {
    LibraryModel libraryModel = data.get(itemId);
    logger.info("removing" + libraryModel);
    data.remove(itemId);
    itemIds.remove(itemId);
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

  private String getData(HashMap<String, LibraryModel> data) {
    StringBuilder response = new StringBuilder();
    for (Entry<String, LibraryModel> letterEntry : data.entrySet()) {
      String letter = letterEntry.getKey();
      response.append("ItemId " + letter);
      LibraryModel libraryModel = letterEntry.getValue();
      response.append(" IeamName " + libraryModel.getItemName());
      response.append(" Quantity " + libraryModel.getQuantity() + "\n");
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
