import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

public class McGillRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  Logger logger = null;

  protected McGillRemoteServiceImpl() throws RemoteException {
    super();
    try {
      logger = Utilities
          .setupLogger(Logger.getLogger("McGillServerLog"), "McGillServer.log");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    return "Find item is called on McGill server by " + userId + " for " + itemName;
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return "Return item is called on McGill server by " + userId + " for " + itemID;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return "Borrow item is called on McGill server by " + userId + " for " + itemID + " for "
        + numberOfDays;
  }

  @Override
  public String addItem(String userId, String itemID, String itemName, int quantity)
      throws RemoteException {
    return null;
  }

  @Override
  public String removeItem(String managerId, String itemId, String quantity) {
    return null;
  }

  @Override
  public String listItem(String managerId) {
    return null;
  }
}
