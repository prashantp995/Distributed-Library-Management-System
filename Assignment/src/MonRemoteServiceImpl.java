import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class MonRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  HashMap<String, HashMap<String, Integer>> data = new HashMap<>();

  protected MonRemoteServiceImpl() throws RemoteException {
    super();
    HashMap<String, Integer> nameAndQuantity = new HashMap<>();
    nameAndQuantity.put("DSD", 5);
    data.put("MON101", nameAndQuantity);

  }

  @Override
  public String findItem(String userId, String iteamName) throws RemoteException {
    return getData(data);
  }

  private String getData(HashMap<String, HashMap<String, Integer>> data) {
    StringBuilder response = new StringBuilder();
    for (Map.Entry<String, HashMap<String, Integer>> letterEntry : data.entrySet()) {
      String letter = letterEntry.getKey();
      response.append("ItemId " + letter);
      for (Map.Entry<String, Integer> nameEntry : letterEntry.getValue().entrySet()) {
        response.append(" IeamName " + nameEntry.getKey());
        response.append(" Quantity " + nameEntry.getValue() + "\n");
      }
    }
    return response.toString();
  }

}
