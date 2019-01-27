import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LibraryRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  protected LibraryRemoteServiceImpl() throws RemoteException {
    super();
  }


  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    return "List";
  }
}
