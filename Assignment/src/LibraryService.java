import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LibraryService extends Remote {

  String findItem(String userId, String iteamName) throws RemoteException;

  String returnItem(String userId, String itemID) throws RemoteException;

  String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException;

}
