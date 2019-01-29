import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.Logger;

public class UserClient {

  static boolean isConcordiaUser = false;
  static boolean isMcGillUser = false;
  static boolean isMonUser = false;


  public static void main(String args[])
      throws RemoteException, NotBoundException, MalformedURLException {

    boolean valid = false;
    while (!valid) {
      System.out.println("Enter your username: ");
      Scanner scanner = new Scanner(System.in);
      String username = scanner.nextLine();
      if (validateUserName(username)) {
        valid = true;
        determineUniversity(username);
        performValidOperation(username);
      } else {
        System.out.println("Given user Name is not in valid format");
      }
    }

  }

  private static void determineUniversity(String username) {
    if (username.startsWith("CONU")) {
      isConcordiaUser = true;
    } else if (username.startsWith("MCGU")) {
      isMcGillUser = true;
    } else if (username.startsWith("MONU")) {
      isMonUser = true;
    }
  }

  private static void performValidOperation(String username) {
    boolean valid = false;
    while (!valid) {
      System.out.println("Please select option from below");
      System.out.println(" 1  for borrowItem");
      System.out.println(" 2  for findItem");
      System.out.println(" 3  for returnItem");
      Scanner scanner = new Scanner(System.in);
      try {

        int choice;
        if (scanner.hasNextInt()) {
          choice = scanner.nextInt();
          if (choice == 1 || choice == 2 || choice == 3) {
            performOperation(choice, username);
            valid = true;
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
    Logger logger = getLogger(username);

    if (isConcordiaUser) {
      logger.info(
          username + " Requested to Return Item " + itemId);
      System.out.println("Connect to Concordia Server");
      Utilities.closeLoggerHandlers(logger);
    }
  }


  private static void performFindItem(String username) {
    boolean valid = false;
    String itemId = getItemId();
    Logger logger = getLogger(username);
    if (isConcordiaUser) {
      System.out.println("Connect to Concordia Server");
      try {
        String response = getItemFindResponse(username, itemId, 8080, "CON", logger);
        System.out.println(response);
      } catch (RemoteException e) {
        e.printStackTrace();
      } catch (NotBoundException e) {
        e.printStackTrace();
      }
      Utilities.closeLoggerHandlers(logger);
    }
    if (isMcGillUser) {
      logger.info(
          username + " Requested to Find Item " + itemId);
      System.out.println("Connect to Concordia Server");
      try {
        String response = getItemFindResponse(username, itemId, 8081, "MCG", logger);
        System.out.println(response);
      } catch (RemoteException | NotBoundException e) {
        e.printStackTrace();
      }
      Utilities.closeLoggerHandlers(logger);
    }
    if (isMonUser) {
      logger.info(
          username + " Requested to Find Item " + itemId);
      System.out.println("Connect to Concordia Server");
      try {
        String response = getItemFindResponse(username, itemId, 8082, "MON", logger);
        System.out.println(response);
      } catch (RemoteException | NotBoundException e) {
        e.printStackTrace();
      }
      Utilities.closeLoggerHandlers(logger);
    }
  }

  private static String getItemFindResponse(String username, String itemId, int port,
      String registryLookUp, Logger logger)
      throws RemoteException, NotBoundException {
    logger.info(
        username + " Requested to Find Item " + itemId);
    Registry registry = LocateRegistry.getRegistry(port);
    LibraryService obj = (LibraryService) registry.lookup(registryLookUp);
    String response = obj.findItem(username, itemId);
    logger.info("Response Received from the server is ");
    return response;
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

  private static void performBorrowItem(String username) {
    boolean valid = false;
    String itemId;
    int numberOfDays;
    Logger logger = getLogger(username);
    while (!valid) {
      System.out.println("Please Enter  Item Id For example CON1012");
      Scanner scanner = new Scanner(System.in);
      itemId = scanner.nextLine();
      System.out.println("Please Enter number Of days");
      numberOfDays = scanner.nextInt();
      if (validateItemIdAndNumberOfDays(itemId, numberOfDays)) {
        valid = true;
        logger.info(
            username + " Requested to Borrow Item " + itemId + " for " + numberOfDays + " days.");
      } else {
        System.out.println("Please Enter Correct Details as specified");
      }

    }
    if (isConcordiaUser) {
      System.out.println("Connect to Concordia Server");
      Utilities.closeLoggerHandlers(logger);
    }

  }

  private static Logger getLogger(String username) {
    Logger logger = null;
    try {
      logger = Utilities
          .setupLogger(Logger.getLogger(username + "log"), username + ".log");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return logger;
  }

  private static boolean validateItemIdAndNumberOfDays(String itemId, int numberOfDays) {
    return validateItemId(itemId) && numberOfDays > 0;
  }

  private static boolean validateItemId(String itemId) {
    return itemId.length() == 7 && (itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId
        .startsWith("MON"));
  }

  private static boolean validateUserName(String username) {
    return username.length() == 8 &&
        (username.contains("CONU")
            || username.contains("MCGU")
            || username.contains("MONU"));
  }

}
