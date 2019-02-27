import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;

public class UserClient {

  static boolean isConcordiaUser = false;
  static boolean isMcGillUser = false;
  static boolean isMonUser = false;
  static boolean isConcordiaManager = false;
  static boolean isMcGillManager = false;
  static boolean isMonManager = false;
  static HashMap<Integer, String> serverInfo = new HashMap<Integer, String>();
  static Logger logger = null;

  public static void main(String args[])
      throws IOException, NotBoundException {
    int RMIPortNum = 8087;
    CallbackClientImpl exportedObj = null;
    try {
      exportedObj = new CallbackClientImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.rebind("CLIENT", exportedObj);
    } catch (RemoteException e) {
      e.printStackTrace();
    }

    System.out.println("Client Started " + " Rmi Port Number " + RMIPortNum + " Look Up "
        + LibConstants.CON_REG);
    boolean valid = false;
    while (!valid) {
      System.out.println("Enter your username: ");
      Scanner scanner = new Scanner(System.in);
      String username = scanner.nextLine();
      if (Utilities.validateUserName(username)) {
        valid = true;
        determineUniversity(username);
        logger = Utilities
            .setupLogger(Logger.getLogger("UserLogger"), username + ".log", false);
        performValidOperation(username);

      } else {
        System.out.println("Given user Name is not in valid format");
      }
    }

  }

  private static void determineUniversity(String username) {
    if (username.startsWith("CON")) {
      isConcordiaUser = true;
      if (username.startsWith("CONM")) {
        isConcordiaManager = true;
      }
    } else if (username.startsWith("MCG")) {
      isMcGillUser = true;
      if (username.startsWith("MCGM")) {
        isMcGillManager = true;
      }
    } else if (username.startsWith("MON")) {
      isMonUser = true;
      if (username.startsWith("MONM")) {
        isMonManager = true;
      }
    }
  }

  private static void performValidOperation(String username) {
    boolean valid = false;
    if (isManagerUser()) {
      while (!valid) {
        System.out.println("Please select option from below");
        System.out.println(" 1  for addItem");
        System.out.println(" 2  for removeItem");
        System.out.println(" 3  for ListItemAvailability");
        System.out.println(" 4  MultiThread");
        System.out.println(" 0  Exit");
        Scanner scanner = new Scanner(System.in);
        try {

          int choice;
          if (scanner.hasNextInt()) {
            choice = scanner.nextInt();
            if (choice == 4) {
              performMultiThreading();
            }
            if (choice == 0) {
              valid = true;
              Utilities.closeLoggerHandlers(logger);
              System.exit(0);
              break;
            }
            if (choice == 1 || choice == 2 || choice == 3) {
              performManagerOperation(choice, username);
            } else {
              System.out.println("please enter valid choice");
            }
          } else {
            System.out.println("Please enter number only");

          }

        } catch (java.util.InputMismatchException e) {
          System.out.println("Please enter properInput");
        }
      }

    } else {
      while (!valid) {
        System.out.println("Please select option from below");
        System.out.println(" 1  for borrowItem");
        System.out.println(" 2  for findItem");
        System.out.println(" 3  for returnItem");
        System.out.println(" 0  Exit");
        Scanner scanner = new Scanner(System.in);
        try {

          int choice;
          if (scanner.hasNextInt()) {
            choice = scanner.nextInt();
            if (choice == 0) {
              valid = false;
              Utilities.closeLoggerHandlers(logger);
              System.exit(0);
              break;
            }
            if (choice == 1 || choice == 2 || choice == 3) {
              performOperation(choice, username);
            } else {
              System.out.println("please enter valid choice");
            }
          } else {
            System.out.println("Please enter number only");

          }

        } catch (java.util.InputMismatchException e) {
          System.out.println("Please enter properInput");
        }

      }
    }


  }

  private static void performManagerOperation(int choice, String username) {
    switch (choice) {
      case 1:
        System.out.println("perform Add item");
        logger.info(username + " choose to Add Item");
        performAddItem(username);
        break;
      case 2:
        System.out.println("perform Remove item");
        logger.info(username + " choose to Remove Item");
        performRemoveItem(username);
        break;
      case 3:
        System.out.println("perform List item");
        logger.info(username + " choose to List Item");
        performListItem(username);
        break;
      default:
        logger.info(username + " Entered invalid choice");
        System.out.println("please enter valid choice");
    }
  }

  private static void performAddItem(String username) {
    String itemId = getItemId();
    logger.info(username + " is trying to Add item with item id " + itemId);
    try {
      Scanner scanner = new Scanner(System.in);
      System.out.println("Please Enter Item Name");
      String itemName = scanner.nextLine();
      System.out.println("Please Enter Quantity");
      int quantity = scanner.nextInt();
      getResponseOfAddItem(username, itemId, itemName, quantity);
    } catch (java.util.InputMismatchException e) {
      System.out.println("Please enter properInput");
    } catch (RemoteException | NotBoundException e) {
      e.printStackTrace();
    }


  }


  private static void performRemoveItem(String username) {
    String itemId = getItemId();
    logger.info(username + " is trying to remove " + itemId);
    try {
      Scanner scanner = new Scanner(System.in);
      System.out.println("Please Enter Quantity");
      int quantity = scanner.nextInt();
      String response = getResponseFromRemoveItem(username, itemId, quantity);
    } catch (RemoteException | NotBoundException e) {
      e.printStackTrace();
    } catch (java.util.InputMismatchException e) {
      System.out.println("Please enter properInput");
    }
  }

  private static String getResponseFromRemoveItem(String username, String itemId,
      int quantity)
      throws RemoteException, NotBoundException {
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to Remove Item " + itemId + "Quantity" + quantity);
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.removeItem(username, itemId, quantity);
    System.out.println("Response Received from the server is " + response);
    logger.info("Response Received from the server is " + response);
    return response;
  }

  private static void performListItem(String username) {
    try {
      try {
        String response = getResponseFromListItem(username);
      } catch (NotBoundException e) {
        e.printStackTrace();
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  private static String getResponseFromListItem(String username)
      throws RemoteException, NotBoundException {
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to List Item ");
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.listItem(username);
    System.out.println("Response Received from the server is " + response);
    logger.info("Response Received from the server is " + response);
    return response;
  }

  private static String getResponseOfAddItem(String username, String itemId,
      String itemName, int quantity) throws RemoteException, NotBoundException {
    logger
        .info(username + " asked to Add item " + itemId + " " + itemName + " Quantity " + quantity);
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to Add Item " + itemName);
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.addItem(username, itemId, itemName, quantity);
    logger.info("Response Received from the server is " + response);
    System.out.println("Response Received from the server is " + response);
    return response;
  }

  private static boolean isManagerUser() {
    return isMonManager || isConcordiaManager || isMcGillManager;
  }

  private static void performOperation(int choice, String username) {
    switch (choice) {
      case 1:
        System.out.println("perform borrow item");
        performBorrowItem(username);
        break;
      case 2:
        System.out.println("perform find item");
        performFindItem(username);
        break;
      case 3:
        System.out.println("perform return item");
        performReturnItem(username);
        break;
      default:
        System.out.println("please enter valid choice");
    }
  }

  private static void performReturnItem(String username) {
    boolean valid = false;
    String itemId = getItemId();
    logger.info(
        username + " Requested to Return Item " + itemId);
    try {
      String response = getReturnItemResponse(username, itemId);
      System.out.println(response);
    } catch (RemoteException | NotBoundException e) {
      e.printStackTrace();
    }


  }


  private static void performFindItem(String username) {
    boolean valid = false;
    String itemName = getItemName();
    try {
      String response = getItemFindResponse(username, itemName);
    } catch (RemoteException | NotBoundException e) {
      e.printStackTrace();
    }


  }

  private static String getItemFindResponse(String username, String itemName)
      throws RemoteException, NotBoundException {
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to Find Item " + itemName);
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.findItem(username, itemName);
    logger.info("Response Received from the server is " + response);
    System.out.println("Response Received from the server is " + response);
    return response;
  }

  private static String getReturnItemResponse(String username, String itemId)
      throws RemoteException, NotBoundException {
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to Return Item " + itemId);
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.returnItem(username, itemId);
    logger.info("Response Received from the server is " + response);
    System.out.println("Response Received from the server is " + response);
    return response;
  }

  private static String getBorrowItemResponse(String itemId, int numberOfDays, String username)
      throws RemoteException, NotBoundException {
    String[] serverInfo = getServerInfo();
    logger.info(
        username + " Requested to Borrow Item " + itemId);
    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(serverInfo[1]));
    logger.info("Connecting to " + serverInfo[0] + "  Server");
    LibraryService obj = (LibraryService) registry.lookup(serverInfo[0]);
    String response = obj.borrowItem(username, itemId, numberOfDays);
    logger.info("Response Received from the server is " + response);
    System.out.println("Response Received from the server is " + response);
    if (response.equalsIgnoreCase("Wait List Possible")) {
      Scanner scanner = new Scanner(System.in);
      System.out.println(
          "Item is not available now , WaitList Possible, Do you wish to enroll your self in waitList "
              + "\n" +
              "Please enter 1 for yes" + "\n" +
              "Please enter 0 for No");
      int choice = scanner.nextInt();
      if (choice == 1) {
        logger.info(username + "is requesting to enroll in waitList of " + itemId);
        String waitListResponse = obj.addUserInWaitingList(username, itemId, numberOfDays);
        logger.info("Response regarding to waitList for " + itemId + " : " + username + " is "
            + waitListResponse);
        System.out.println("Response regarding to waitList for " + itemId + " " + username + " is "
            + waitListResponse);
      }
    }
    return response;
  }


  private static void performBorrowItem(String username) {
    boolean valid = false;
    String itemId;
    int numberOfDays;
    while (!valid) {
      Scanner scanner = new Scanner(System.in);
      itemId = getItemId();
      System.out.println("Please Enter number Of days");
      numberOfDays = scanner.nextInt();
      if (Utilities.validateItemIdAndNumberOfDays(itemId, numberOfDays)) {
        valid = true;
        logger.info(
            username + " Requested to Borrow Item " + itemId + " for " + numberOfDays + " days.");
        try {
          getBorrowItemResponse(itemId, numberOfDays, username);
        } catch (RemoteException | NotBoundException e) {
          e.printStackTrace();
        }
      } else {
        System.out.println("Please Enter Correct Details as specified");
      }

    }


  }


  private static String getItemId() {
    boolean valid = false;
    String itemId = null;
    while (!valid) {
      System.out.println("Please Enter  Item Id For example CON1012");
      Scanner scanner = new Scanner(System.in);
      itemId = scanner.nextLine();
      if (validateItemId(itemId)) {
        valid = true;
      } else {
        System.out.println("Please enter Valid Id");
      }
    }
    return itemId;
  }

  private static String getItemName() {
    System.out.println("Please Enter  Item Name");
    Scanner scanner = new Scanner(System.in);
    String itemName = scanner.nextLine();
    return itemName;
  }

  private static String[] getServerInfo() {
    String serverInfo[] = new String[2];
    if (isConcordiaUser) {
      serverInfo[0] = LibConstants.CON_REG;
      serverInfo[1] = String.valueOf(LibConstants.CON_PORT);
    } else if (isMcGillUser) {
      serverInfo[0] = LibConstants.MCG_REG;
      serverInfo[1] = String.valueOf(LibConstants.MCG_PORT);
    } else if (isMonUser) {
      serverInfo[0] = LibConstants.MON_REG;
      serverInfo[1] = String.valueOf(LibConstants.MON_PORT);
    }
    return serverInfo;
  }

  public static boolean validateItemId(String itemId) {
    if (isManagerUser()) {
      return itemId.length() == 7 &&
          ((itemId.startsWith("CON") && isConcordiaManager) || (itemId
              .startsWith("MCG") && isMcGillManager) || (itemId
              .startsWith("MON") && isMonManager));
    }
    return itemId.length() == 7 && (itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId
        .startsWith("MON"));
  }

  public static void performMultiThreading() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          getResponseOfAddItem("CONM1111", "CON1015", "DSD", 1);
        } catch (RemoteException e) {
          e.printStackTrace();
        } catch (NotBoundException e) {
          e.printStackTrace();
        }
      }
    };

    Runnable runnable2 = new Runnable() {
      @Override
      public void run() {
        try {
          //getResponseOfAddItem("CONM1111", "CON1015", "DSD", 1);
          getResponseFromRemoveItem("CONM1111", "CON1015", 5);
          //getReturnItemResponse("CONU1111", "CON1012");
        } catch (RemoteException e) {
          e.printStackTrace();
        } catch (NotBoundException e) {
          e.printStackTrace();
        }
      }
    };
    Runnable runnable3 = new Runnable() {
      @Override
      public void run() {
        try {
          getReturnItemResponse("CONU1111", "CON1015");
        } catch (RemoteException e) {
          e.printStackTrace();
        } catch (NotBoundException e) {
          e.printStackTrace();
        }
      }
    };
    Thread thread = new Thread(runnable);
    Thread thread2 = new Thread(runnable2);
    Thread thread3 = new Thread(runnable3);
    thread.start();
    thread2.start();
    thread3.start();


  }

}
