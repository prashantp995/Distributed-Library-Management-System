import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MonRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  protected MonRemoteServiceImpl() throws RemoteException {
    super();
  }

  @Override
  public String findItem(String userId, String iteamName) throws RemoteException {
    return "Return Fom the Mon Remote Server";
  }
}
