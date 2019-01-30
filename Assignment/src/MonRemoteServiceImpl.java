import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MonRemoteServiceImpl extends UnicastRemoteObject implements LibraryService {

  HashMap<String, HashMap<String, Integer>> data = new HashMap<>();
  HashSet<String> itemIds = new HashSet<>();
  ArrayList<String> bookName = new ArrayList<>();

  protected MonRemoteServiceImpl() throws RemoteException {
    super();
    HashMap<String, Integer> nameAndQuantity;
    Initbooks();
    for (int i = 0; i < bookName.size(); i++) {
      nameAndQuantity = new HashMap<>();
      nameAndQuantity.put(bookName.get(0), i + 1);
      String itemId = "MON100" + i;
      itemIds.add(itemId);
      data.put(itemId, nameAndQuantity);
    }


  }

  private void Initbooks() {
    bookName.add("DSD");
    bookName.add("APP");
    bookName.add("ALGO");
  }


  @Override
  public String findItem(String userId, String iteamName) throws RemoteException {
    return getData(data);
  }

  @Override
  public String returnItem(String userId, String itemID) throws RemoteException {
    return "Return item called from " + userId + " for" + itemID;
  }

  @Override
  public String borrowItem(String userId, String itemID, int numberOfDays) throws RemoteException {
    return null;
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
