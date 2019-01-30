import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class McGillRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  protected McGillRemoteServiceImpl() throws RemoteException {
    super();
  }

  @Override
  public String findItem(String userId, String iteamName) throws RemoteException {
    return "Return From McGill Remote Server";
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return null;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return null;
  }
}
