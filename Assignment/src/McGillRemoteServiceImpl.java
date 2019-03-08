import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.omg.CORBA.ORB;

public class McGillRemoteServiceImpl extends LibraryServicePOA {

  HashMap<String, LibraryModel> data = new java.util.HashMap<>();
  HashMap<String, ArrayList<String>> currentBorrowers = new HashMap<>();
  HashSet<String> managerIds = new HashSet<>();
  HashSet<String> userIds = new HashSet<>();
  HashSet<String> completelyRemovedItems = new HashSet<String>();//removed items by Manager
  Logger logger = null;
  String lib = LibConstants.MCG_REG;
  private ORB orb;


  protected McGillRemoteServiceImpl(Logger logger) {
    super();
    initManagerID();
    initUserID();
    data.put("MCG1012", new LibraryModel("DSD", 52));
    data.put("MCG1013", new LibraryModel("ALGO", 0));
    this.logger = logger;


  }

  private void initManagerID() {
    managerIds.add("MCGM1111");
    managerIds.add("MCGM1112");
  }

  private void initUserID() {
    userIds.add("MCGU1111");
    userIds.add("MCGU1112");
  }

  public void setORB(ORB orb_val) {
    orb = orb_val;
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
        return "No item found in McGill Server";
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
      String montrealServerResponse = ServerUtils
          .callUDPServer(udpRequestModel, LibConstants.UDP_MON_PORT, logger);
      String concordiaServerResponse = ServerUtils
          .callUDPServer(udpRequestModel, LibConstants.UDP_CON_PORT, logger);
      if (montrealServerResponse != null && montrealServerResponse.length() > 0) {
        response.append("\n" + montrealServerResponse + "\n");
      }
      if (concordiaServerResponse != null && concordiaServerResponse.length() > 0) {
        response.append(concordiaServerResponse + "\n");
      }

    }

    return response.toString();
  }


  private boolean isValidUser(String userId) {
    return userIds.contains(userId);
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
    if (!isValidUser(userId)) {
      logger.info(userId + "is not present/authorised");
      return userId + "is not present/authorised";
    }
    String x = validateBorrow(userId, itemID);
    if (!x.equalsIgnoreCase(LibConstants.SUCCESS)) {
      return x;
    }
    StringBuilder response = new StringBuilder();
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

  private String validateBorrow(String userId, String itemID) {
    if (currentBorrowers.containsKey(userId)) {
      synchronized (currentBorrowers) {
        logger.info("Current Borrower" + userId + "Borrowed Item " + currentBorrowers.get(userId)
            .toString());
        for (String borrowedItem : currentBorrowers.get(userId)) {
          if (!borrowedItem.startsWith("MCG") && !itemID.startsWith("MCG")) {
            if (borrowedItem.startsWith("MON") && itemID.startsWith("MON")) {
              return LibConstants.FAIL
                  + "Can not borrow more than one item from each of  external library";
            }
            if (borrowedItem.startsWith("CON") && itemID.startsWith("CON")) {
              return LibConstants.FAIL
                  + "Can not borrow more than one item from each of  external library";
            }
          }

        }
      }

    }

    if (itemID.startsWith("MCG") && !data.containsKey(itemID)) {
      return LibConstants.FAIL + "Can not borrow , Item id is unknown to Library";
    }
    return LibConstants.SUCCESS;
  }

  private String handleWaitList(String userId, String itemID, int numberOfDays, String res,
      boolean externalServerCallRequire) {
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

  @Override
  public String addItem(String userId, String itemID, String itemName, int quantity) {
    logger.info("Add item is called on McGill server by " + userId + " for " + itemID + " for "
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
      logger.info("Item id   exist in  database, modifying as new Item");
      if (validateItemIdAndName(itemID, itemName)) {
        logger.info("Item id and Item name matches");
        LibraryModel libraryModel = data.get(itemID);
        int previousQuantity = libraryModel.getQuantity();
        libraryModel.setQuantity(previousQuantity + quantity);
        data.put(itemID, libraryModel);
        if (previousQuantity == 0) {
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
        "Remove item is called on McGill server by " + managerId + " for " + itemId + " for "
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
        response.append("Remove Item Success , Item Removed Completely");
      } else if (data.get(itemId).getQuantity() - quantity < 0) {
        response.append("Please Provide Correct Quantity");
      } else {
        updateQuantity(itemId, quantity);
        response.append("Remove Item Success , Updated the Quantity");
      }
    }
    return response.toString();
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
    return addUserInWaitList(ItemId, userId, numberOfDays, !ItemId.startsWith("MCG"));
  }

  @Override
  public String exchangeItem(String userId, String oldItemId, String newItemID) {
    String oldItemId_Lib = ServerUtils.determineLibOfItem(oldItemId);
    String newItemId_Lib = ServerUtils.determineLibOfItem(newItemID);
    if (newItemId_Lib.equalsIgnoreCase(lib)) {
      String validateBorrowForLocalUser = validateBorrow(userId, newItemID);
      if (!validateBorrowForLocalUser.equalsIgnoreCase(LibConstants.SUCCESS)) {
        return validateBorrowForLocalUser;
      }
    }
    if (oldItemId_Lib != null && newItemId_Lib != null) {
      if (oldItemId_Lib.equals(lib) && newItemId_Lib
          .equals(lib)) {
        return performExchange(userId, oldItemId, newItemID, false, oldItemId_Lib, newItemId_Lib);
      } else {
        return performExchange(userId, oldItemId, newItemID, true, oldItemId_Lib, newItemId_Lib);
      }
    }

    return LibConstants.SUCCESS;
  }

  private String performExchange(String userId, String oldItemId, String newItemID,
      boolean callExternalServer, String oldItemId_Lib, String newItemId_Lib) {
    if (!callExternalServer) {
      if (data.containsKey(oldItemId)) {
        if (!data.get(oldItemId).getCurrentBorrowerList().contains(userId)) {
          logger.info("Exchange item is not valid");
          return oldItemId + " is not borrowed by user " + userId
              + "Can not perform exchange operation";
        } else if (data.get(newItemID).getCurrentBorrowerList().contains(userId)) {
          logger.info("Exchange item is not valid");
          return newItemID + " is already borrowed by user " + userId
              + "Can not perform exchange operation ";
        } else {
          logger.info("Exchange item is valid");
          if (data.containsKey(newItemID) && data.get(newItemID).getQuantity() > 0) {
            logger.info("Old item id and New item id both belongs to Montreal Server");
            boolean isValidBorrow = isItemAvailableToBorrow(newItemID, userId, 0);
            boolean isValidReturn = isValidReturn(userId, data.get(oldItemId));
            synchronized (data) {
              if (isValidBorrow && isValidReturn) {
                returnItem(userId, oldItemId);
                borrowItem(newItemID, userId, 2);
                return LibConstants.SUCCESS;
              }
            }

          } else {
            return newItemID + " is not recognized by library or enough quantity not available";
          }
        }
      }
    } else {
      logger.info("Need to connect to external server ....");
      if (oldItemId_Lib.equals(lib)) {
        logger.info(newItemID + "belongs to external server");
        boolean isValidReturn = isValidReturn(userId, data.get(oldItemId));
        logger.info("Verifying" + newItemID + " is is available to borrow In " + newItemId_Lib);
        String isValidBorrow = ServerUtils
            .validateBorrowOnExternalServer(userId, newItemID, logger);
        logger.info("Response received from " + newItemId_Lib + " is " + isValidBorrow);
        if (isValidReturn && isValidBorrow.equalsIgnoreCase("true")) {
          logger.info("validation successful");
          returnItem(userId, oldItemId);
          borrowItem(userId, newItemID, 2);
          return LibConstants.SUCCESS;
        } else {
          return LibConstants.FAIL;
        }

      } else if (newItemId_Lib.equals(lib)) {
        logger.info(oldItemId + "Belongs to external server");
        boolean isValidBorrow = isItemAvailableToBorrow(newItemID, userId, 0);
        String isValidReturn = ServerUtils
            .validateReturnOnExternalServer(userId, oldItemId, logger);
        if (isValidBorrow && isValidReturn.equalsIgnoreCase("true")) {
          returnItem(userId, oldItemId);
          borrowItem(userId, newItemID, 2);
          return LibConstants.SUCCESS;
        } else {
          return LibConstants.FAIL;
        }

      } else if (!oldItemId_Lib.equals(lib) && !newItemId_Lib
          .equals(lib)) {
        logger.info("both item id belongs to external server");
        String isValidReturn = ServerUtils
            .validateReturnOnExternalServer(userId, oldItemId, logger);
        String isValidBorrow = ServerUtils
            .validateBorrowOnExternalServer(userId, newItemID, logger);
        if (isValidBorrow.equalsIgnoreCase("true") && isValidReturn.equalsIgnoreCase("true")) {
          returnItem(userId, oldItemId);
          borrowItem(userId, newItemID, 2);
          return LibConstants.SUCCESS;
        } else {
          return LibConstants.FAIL;
        }
      }

    }
    return LibConstants.FAIL;
  }

  private String getData(HashMap<String, LibraryModel> data) {
    StringBuilder response = new StringBuilder();
    for (Entry<String, LibraryModel> letterEntry : data.entrySet()) {
      String letter = letterEntry.getKey();
      response.append("ItemId " + letter);
      LibraryModel libraryModel = letterEntry.getValue();
      response.append(" IeamName " + libraryModel.getItemName());
      response.append(" Quantity " + libraryModel.getQuantity() + "\n");
      response.append("Current Borrowers" + libraryModel.getCurrentBorrowerList() + "\n");
      response.append("Waiting List" + libraryModel.getWaitingList() + "\n");
    }
    return response.toString();
  }


  private boolean isValidManager(String managerId) {
    return managerIds.contains(managerId);
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
    if (data.containsKey(itemID) && data.get(itemID).getQuantity() == 0 && !data.get(itemID)
        .getCurrentBorrowerList().contains(userId)) {
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

  public boolean isItemAvailableToBorrow(String itemID, String userId, int numberOfDays) {
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
    int port = ServerUtils.getPortFromItemId(itemID);
    if (port != 0) {
      response = ServerUtils
          .callUDPServer(udpRequestModel, port, logger);
    } else {
      response = "invalid item id";
    }
    return response;


  }

  /**
   * this is to perform return operation ,
   *
   * @param callExternalServer set false , if you return itemid belongs to this server
   */
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
        if (isValidReturn(userId, model)) {
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

  private boolean isValidReturn(String userId, LibraryModel model) {
    if (model == null) {
      return false;
    }
    return model.getCurrentBorrowerList() != null && model.getCurrentBorrowerList()
        .contains(userId);
  }

  public boolean isValidReturn(String userId, String itemId) {
    return isValidReturn(userId, data.get(itemId));
  }

  /**
   * Once item is return by user , remove user from the current borrower list
   *
   * @param userId userId
   * @param itemID itemID
   */
  private synchronized void removeFromCurrentBorrowers(String userId, String itemID) {
    if (currentBorrowers.containsKey(userId)) {
      ArrayList<String> borrowedItems = currentBorrowers.get(userId);
      borrowedItems.remove(itemID);
      currentBorrowers.put(userId, borrowedItems);
    }
  }

  /**
   * if item is removed from the manager  , after that if manager decides to add item again in
   * database we should update completelyRemoved item data .
   */
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

  public String isUsereligibleToGetbook(String firstUserInWaitingList, String itemId,
      boolean callExternalServers) {

    if (callExternalServers && isOtherLibraryUser(firstUserInWaitingList)) {
      UdpRequestModel requestModel = new UdpRequestModel();
      requestModel.setMethodName(LibConstants.USER_BORROWED_ITEMS);
      requestModel.setUserId(firstUserInWaitingList);
      requestModel.setItemId(itemId);
      String response = ServerUtils.callUDPServer(requestModel, LibConstants.UDP_MON_PORT, logger);
      String response2 = ServerUtils.callUDPServer(requestModel, LibConstants.UDP_CON_PORT, logger);
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
              if (borrowedItem.startsWith("MCG")) {
                return LibConstants.FAIL;
              }

            }
          }
          for (String borrowedItem : currentBorrowers.get(firstUserInWaitingList)) {
            if (!borrowedItem.startsWith("MCG") && !itemId.startsWith("MCG")) {
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
}
