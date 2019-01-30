import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ConcordiaRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  protected ConcordiaRemoteServiceImpl() throws RemoteException {
    super();
  }


  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    return "Find item is called on concordia server by " + userId + " for " + itemName;
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return "Return item is called on concordia server by " + userId + " for " + itemID;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return "Borrow item is called on concordia server by " + userId + " for " + itemID + " for "
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
