import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LibraryService extends Remote {

  String findItem(String userId, String itemName) throws RemoteException;

  String returnItem(String userId, String itemID) throws RemoteException;

  String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException;

  String addItem(String userId, String itemID, String itemName, int quantity)
      throws RemoteException;

  String removeItem(String managerId, String itemId, int quantity)throws RemoteException;

  String listItem(String managerId)throws RemoteException;

}
