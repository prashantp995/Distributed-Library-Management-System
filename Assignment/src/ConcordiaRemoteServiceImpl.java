import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class ConcordiaRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  HashMap<String, Integer> map = new HashMap<>();

  protected ConcordiaRemoteServiceImpl() throws RemoteException {
    super();
    map.put("TEst", 4);
  }


  @Override
  public String findItem(String userId, String itemName) throws RemoteException {
    return "Return from the Concordia Remote Server";
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return null;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return null;
  }

  public String provideCount(){
    return String.valueOf(map.get("TEst"));
  }
}
