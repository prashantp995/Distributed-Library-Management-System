import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.omg.CORBA.ORB;

public class ConcordiaRemoteServiceImpl extends LibraryServicePOA {

  HashMap<String, LibraryModel> data = new HashMap<>();
  HashMap<String, ArrayList<String>> currentBorrowers = new HashMap<>();

  HashSet<String> managerId = new HashSet<>();
  HashSet<String> userIds = new HashSet<>();
  HashSet<String> completelyRemovedItems = new HashSet<String>();//removed items by Manager
  Logger logger = null;
  private ORB orb;


  protected ConcordiaRemoteServiceImpl(Logger logger) {
    super();
    initManagerID();
    initUserID();
    data.put("CON1012", new LibraryModel("DSD", 5));
    data.put("CON1013", new LibraryModel("ALGO", 0));
    this.logger = logger;


  }

  public void setORB(ORB orb_val) {
    orb = orb_val;
  }

  private void initManagerID() {
    managerId.add("CONM1111");
    managerId.add("CONM1112");
  }

  private void initUserID() {
    userIds.add("CONU1111");
    userIds.add("CONU1112");
  }


  @Override
  public String findItem(String userId, String itemName) {
    logger.info(userId + "requested to find item" + itemName);
    String itemDetails;
    if (!isValidUser(userId)) {
      logger.info(userId + "is not valid/authorized to  find Item" + itemName);
      return userId + "is not valid/authorized to  find Item" + itemName;
    } else {
      itemDetails = findItem(itemName, true);
      if (itemDetails.length() == 0) {
        return "No item found in Concordia Server";
      }
    }
    return itemDetails;
  }


  @Override
  public String returnItem(String userId, String itemID) {
    if (!isValidUser(userId)) {
      logger.info(userId + "is not present/authorised");
      return userId + "is not present/authorised";
    }
    String response = null;
    if (data.containsKey(itemID) || completelyRemovedItems.contains(itemID)) {
      response = performReturnItemOperation(userId, itemID, false);
      logger.info("Return Item Response is " + response);

    } else {
      response = performReturnItemOperation(userId, itemID, true);
      if (response.equalsIgnoreCase(LibConstants.SUCCESS)) {
        System.out.println("External Server Approved Return Item");
        removeFromCurrentBorrowers(userId, itemID);
      }
    }
    return response;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) {
    logger.info(userId + " has asked to borrow item " + itemID);
    if (!isValidUser(userId)) {
      logger.info(userId + "is not present/authorised");
      return userId + "is not present/authorised";
    }
    if (currentBorrowers.containsKey(userId)) {
      synchronized (currentBorrowers) {
        logger.info("Current Borrower" + userId + "Borrowed Item " + currentBorrowers.get(userId)
            .toString());
        for (String borrowedItem : currentBorrowers.get(userId)) {
          if (!borrowedItem.startsWith("CON") && !itemID.startsWith("CON")) {
            if (borrowedItem.startsWith("MON") && itemID.startsWith("MON")) {
              return LibConstants.FAIL
                  + "Can not borrow more than one item from each of  external library";
            }
            if (borrowedItem.startsWith("MCG") && itemID.startsWith("MCG")) {
              return LibConstants.FAIL
                  + "Can not borrow more than one item from each of  external library";
            }

          }

        }
      }

    }
    StringBuilder response = new StringBuilder();
    if (itemID.startsWith("CON") && !data.containsKey(itemID)) {
      logger.info(userId + " Can not borrow , Item id is unknown to Library  " + itemID);
      return LibConstants.FAIL + " Can not borrow , Item id is unknown to Library";
    }
    if (data.containsKey(itemID) && data.get(itemID).getCurrentBorrowerList().contains(userId)) {
      logger.info("user already borrowed item ");
      return LibConstants.FAIL + " user already borrowed item";
    }
    String result = performBorrowItemOperation(itemID, userId,
        numberOfDays);//this will fail if itemID is not in this server
    //if fails then we need to connect to Respective External Server to Borrow Item
    if (result.equals(LibConstants.FAIL)) {
      logger.info(
          "Calling ExternalUDP Servers To Borrow " + itemID + " For " + userId);
      String res = borrowItemFromExternalServer(userId, itemID, numberOfDays);
      response.append(res);
      if (res.equals(LibConstants.WAIT_LIST_POSSIBLE)) {
        return res;
      } else if (res.equalsIgnoreCase(LibConstants.SUCCESS)) {
        addOrUpdateInCurrentBorrowers(userId, itemID);//external server approved borrow item
        return LibConstants.SUCCESS;
      }


    } else if (result.equalsIgnoreCase(LibConstants.WAIT_LIST_POSSIBLE)) {
      return result;
    }

    return result;
  }

  @Override
  public String addItem(String userId, String itemID, String itemName, int quantity) {
    logger.info("Add item is called on Concordia server by " + userId + " for " + itemID + " for "
        + quantity + " name " + itemName);
    StringBuilder response = new StringBuilder();
    if (!isValidManager(userId)) {
      logger.info(userId + "is not present/authorised");
      return "Item Add Fails,User is not Present/Authorised";
    }
    if (!data.containsKey(itemID)) {
      logger.info("Item id is not in existing database, Adding as new Item");
      LibraryModel libraryModel = new LibraryModel(itemName, quantity);
      data.put(itemID, libraryModel);
      handleAlreadyRemovedItems(itemID);
      response.append("Item Add Success");
    } else {
      logger.info("Item id   exist in  database, modifying  Item");
      if (validateItemIdAndName(itemID, itemName)) {
        logger.info("Item id and Item name matches");
        LibraryModel libraryModel = data.get(itemID);
        logger.info("previous item details" + libraryModel);
        int previousQuantity = libraryModel.getQuantity();
        libraryModel.setQuantity(previousQuantity + quantity);
        data.put(itemID, libraryModel);
        logger.info("updated item details" + libraryModel);
        if (previousQuantity == 0) {
          logger.info("previous quantity was 0 , now need to process waiting list");
          processWaitingListIfPossible(itemID);
        }
        handleAlreadyRemovedItems(itemID);
        response.append("Item add success, Quantity updated");
      } else {
        response.append("Item Add Fails , Item Id and Name Does not match");
      }
    }
    return response.toString();
  }

  @Override
  public String removeItem(String managerId, String itemId, int quantity) {
    logger.info(
        "Remove item is called on Concordia server by " + managerId + " for " + itemId + " for "
            + quantity);
    StringBuilder response = new StringBuilder();
    if (!isValidManager(managerId)) {
      logger.info(managerId + "is not Present/Authorised");
      return "Item Remove Fails,User is not Present/Authorised";
    }
    if (!data.containsKey(itemId)) {
      response.append("Remove Item Fails , Item id is not present in database");
    } else {
      if (quantity <= 0 || data.get(itemId).getQuantity() - quantity == 0) {
        removeItemCompletely(itemId);
        logger.info("Remove Item Success , " + itemId + " Removed Completely by " + managerId);
        response
            .append("Remove Item Success , " + itemId + " Removed Completely by " + managerId);
      } else if (data.get(itemId).getQuantity() - quantity < 0) {
        response.append("Please Provide Correct Quantity");
      } else {
        updateQuantity(itemId, quantity);
        logger.info("Remove Item Success , " + itemId + " Updated the Quantity by  " + managerId);
        response
            .append("Remove Item Success , " + itemId + " Updated the Quantity by  " + managerId);
      }
    }
    return response.toString();
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

  @Override
  public String addUserInWaitingList(String userId, String ItemId, int numberOfDays) {
    return addUserInWaitList(ItemId, userId, numberOfDays, !ItemId.startsWith("CON"));

  }

  private String getData(HashMap<String, LibraryModel> data) {
    StringBuilder response = new StringBuilder();
    for (Entry<String, LibraryModel> letterEntry : data.entrySet()) {
      String letter = letterEntry.getKey();
      response.append("ItemId " + letter);
      LibraryModel libraryModel = letterEntry.getValue();
      response.append(" IeamName " + libraryModel.getItemName());
      response.append(" Quantity " + libraryModel.getQuantity() + "\n");
      response.append(" WaitingList " + libraryModel.getWaitingList() + "\n");
      response.append(" Current Borrowers" + libraryModel.getCurrentBorrowerList() + "\n");
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

  private synchronized void updateQuantity(String itemId, int quantity) {
    LibraryModel libraryModel = data.get(itemId);
    int previousQuantity = libraryModel.getQuantity();
    libraryModel.setQuantity(previousQuantity - quantity);
    logger.info("updating existing item " + libraryModel);
    data.put(itemId, libraryModel);
  }

  private synchronized void removeItemCompletely(String itemId) {
    LibraryModel libraryModel = data.get(itemId);
    logger.info("removing" + libraryModel);
    if (libraryModel.getCurrentBorrowerList() != null
        && libraryModel.getCurrentBorrowerList().size() > 0) {
      logger
          .info("Manager is trying to remove item completely but item is already assigned to"
              + libraryModel.getCurrentBorrowerList().toString());
      completelyRemovedItems.add(itemId);

    }
    data.remove(itemId);
  }


  public synchronized String performBorrowItemOperation(String itemID, String userId,
      int numberOfDays) {
    if (isItemAvailableToBorrow(itemID, userId, numberOfDays)) {
      LibraryModel libraryModel = data.get(itemID);
      libraryModel.getCurrentBorrowerList().add(userId);
      libraryModel.setQuantity(libraryModel.getQuantity() - 1);
      data.put(itemID, libraryModel);
      addOrUpdateInCurrentBorrowers(userId, itemID);
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

  private boolean isValidManager(String managerId) {
    return managerId.contains(managerId);
  }

  private synchronized void removeFromCurrentBorrowers(String userId, String itemID) {
    if (currentBorrowers.containsKey(userId)) {
      currentBorrowers.get(userId).remove(itemID);
    }
  }

  private String handleWaitList(String userId, String itemID, int numberOfDays, String res,
      boolean externalServerCallRequire) {
    logger.info(
        itemID + " is not available to borrow now, adding wait list is possible ,asking " + userId);
    int clientChoice = Utilities.getResponseFromClient(logger);
    if (clientChoice == 1) {
      res = addUserInWaitList(itemID, userId, numberOfDays, externalServerCallRequire);
    }
    return res;
  }

  private void addOrUpdateInCurrentBorrowers(String userId, String itemID) {
    if (!currentBorrowers.containsKey(userId)) {
      ArrayList<String> itemBorrowed = new ArrayList<>();
      itemBorrowed.add(itemID);
      currentBorrowers.put(userId, itemBorrowed);
    } else {
      currentBorrowers.get(userId).add(itemID);
    }
  }

  private void processWaitingListIfPossible(String itemID) {
    if (data.containsKey(itemID)) {
      logger.info("Now attempting to process wait list");
      synchronized (data) {
        while (data.get(itemID).waitingList.size() > 0) {
          String waitlistUser = data.get(itemID).getWaitingList().get(0);
          if (data.get(itemID).getQuantity() > 0) {
            logger.info("Waiting List Found For The Item Id " + itemID);
            String res = isUsereligibleToGetbook(waitlistUser, itemID,
                true);
            if (res.equalsIgnoreCase(LibConstants.FAIL)) {
              logger.info(
                  waitlistUser + "Already got one thing out of library , can not assign "
                      + itemID);
              data.get(itemID).getWaitingList().remove(waitlistUser);//remove from waiting list
              continue;
            } else {
              data.get(itemID).getCurrentBorrowerList().add(waitlistUser);//add in borrower list
              data.get(itemID).getWaitingList().remove(waitlistUser);//remove from waiting list
              data.get(itemID).setQuantity(data.get(itemID).getQuantity() - 1);
              addOrUpdateInCurrentBorrowers(waitlistUser, itemID);
              logger.info(itemID + "  is assigned to " + waitlistUser);
            }
          }
        }


      }

    }
  }

  public String isUsereligibleToGetbook(String firstUserInWaitingList, String itemId,
      boolean callExternalServers) {

    if (callExternalServers && isOtherLibraryUser(firstUserInWaitingList)) {
      UdpRequestModel requestModel = new UdpRequestModel();
      requestModel.setMethodName(LibConstants.USER_BORROWED_ITEMS);
      requestModel.setUserId(firstUserInWaitingList);
      requestModel.setItemId(itemId);
      String response = ServerUtils.callUDPServer(requestModel, LibConstants.UDP_MON_PORT, logger);
      String response2 = ServerUtils.callUDPServer(requestModel, LibConstants.UDP_MCG_PORT, logger);
      System.out.println("reseponse received" + response + "response received 2" + response2);
      if (response.equalsIgnoreCase(LibConstants.FAIL) || response2
          .equalsIgnoreCase(LibConstants.FAIL)) {
        return LibConstants.FAIL;
      }
    } else {

      if (currentBorrowers.containsKey(firstUserInWaitingList)) {
        synchronized (currentBorrowers) {
          if (isOtherLibraryUser(firstUserInWaitingList)) {
            for (String borrowedItem : currentBorrowers.get(firstUserInWaitingList)) {
              if (borrowedItem.startsWith("CON")) {
                return LibConstants.FAIL;
              }

            }
          }
          for (String borrowedItem : currentBorrowers.get(firstUserInWaitingList)) {
            if (!borrowedItem.startsWith("CON") && !itemId.startsWith("CON")) {
              return LibConstants.FAIL;
            }

          }
        }

      }
    }
    return LibConstants.SUCCESS;
  }

  private boolean isOtherLibraryUser(String firstUserInWaitingList) {
    return !isValidUser(firstUserInWaitingList);
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
      String montrealServerResponse = ServerUtils
          .callUDPServer(udpRequestModel, LibConstants.UDP_MON_PORT, logger);
      if (montrealServerResponse != null && montrealServerResponse.length() > 0) {
        response.append("\n" + montrealServerResponse + "\n");
      }
      String mcgServerResponse = ServerUtils
          .callUDPServer(udpRequestModel, LibConstants.UDP_MCG_PORT, logger);
      if (mcgServerResponse != null && mcgServerResponse.length() > 0) {
        response.append("\n" + mcgServerResponse + "\n");
      }

    }

    return response.toString();
  }


  private boolean isValidUser(String userId) {
    return userIds.contains(userId);
  }

  public String addUserInWaitList(String itemID, String userId, int numberOfDays,
      boolean externalServerCallRequire) {
    String response = null;
    if (externalServerCallRequire) {
      int port = ServerUtils.getPortFromItemId(itemID);
      UdpRequestModel udpRequestModel = new UdpRequestModel(LibConstants.OPR_WAIT_LIST, itemID,
          numberOfDays, userId);

      if (port != 0) {
        response = ServerUtils
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

  public String performReturnItemOperation(String userId, String itemID,
      boolean callExternalServer) {
    String response = null;
    if (callExternalServer) {
      UdpRequestModel udpRequestModel = new UdpRequestModel("returnItem", itemID, userId);
      int port = ServerUtils.getPortFromItemId(itemID);
      if (port != 0) {
        response = ServerUtils.callUDPServer(udpRequestModel, port, logger);
      }
      return response;
    } else {
      if (completelyRemovedItems.contains(itemID)) {
        logger.info(userId + " is trying to return item which is removed from library by manager");
        //we do not update the data in this case , simply accept the return request from user
        return LibConstants.SUCCESS;
      }
      if (data.containsKey(itemID)) {
        LibraryModel model = data.get(itemID);
        if (model.getCurrentBorrowerList() != null && model.getCurrentBorrowerList()
            .contains(userId)) {
          synchronized (data) {
            model.getCurrentBorrowerList().remove(userId);
            int previousQuantity = model.getQuantity();
            model.setQuantity(model.getQuantity() + 1);
            data.put(itemID, model);
            removeFromCurrentBorrowers(userId, itemID);
            if (previousQuantity == 0) {
              logger.info("Previous Quantity for the item " + itemID
                  + " was 0 ,Now processing waitinglist");
              processWaitingListIfPossible(itemID);
            }

            return LibConstants.SUCCESS;
          }
        }
      }
      return LibConstants.FAIL;
    }
  }

  private String borrowItemFromExternalServer(String userId, String itemID, int numberOfDays) {
    UdpRequestModel udpRequestModel = new UdpRequestModel("borrowItem", itemID, numberOfDays,
        userId);
    String response = null;
    int port = ServerUtils.getPortFromItemId(itemID);
    if (port != 0) {
      response = ServerUtils
          .callUDPServer(udpRequestModel, port, logger);
    } else {
      response = "invalid item id";
    }
    return response;


  }

  private void handleAlreadyRemovedItems(String itemID) {
    if (completelyRemovedItems.contains(itemID)) {
      logger
          .info(itemID
              + " was removed completely by manager in past, removing item id from the records of removed item ");
      synchronized (completelyRemovedItems) {
        completelyRemovedItems
            .remove(
                itemID);// remove from the set as Now the Item is added by the Manager , so that users can borrow/return it
      }

    }
  }


}
