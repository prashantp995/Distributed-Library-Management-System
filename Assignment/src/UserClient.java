import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class UserClient {

  public static void main(String args[])
      throws RemoteException, NotBoundException, MalformedURLException {
    LibraryService libraryRemoteService = (LibraryService) Naming
        .lookup("rmi://localhost:8080/findItem");
    String list = libraryRemoteService.findItem("1", "2");
    System.out.println(list);
  }
}
