import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LibraryService extends Remote {

  public String findItem(String userId,String iteamName) throws RemoteException;

}
