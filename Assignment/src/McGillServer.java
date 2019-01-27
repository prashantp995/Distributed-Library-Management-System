import java.rmi.Naming;

public class McGillServer {

  public static void main(String args[]) {

    String registryURL;
    try {

      int RMIPortNum = 8081;
      Utilities.startRegistry(RMIPortNum);
      LibraryRemoteServiceImpl exportedObj = new LibraryRemoteServiceImpl();
      System.setProperty("java.rmi.server.hostname", "localhost");
      registryURL = "rmi://localhost:" + RMIPortNum + "/findItem";
      Naming.rebind(registryURL, exportedObj);
      System.out.println("Server registered.  Registry currently contains:");
      Utilities.listRegistry(registryURL);
      System.out.println("Server ready.");
    } catch (Exception re) {
      System.out.println("Exception in HelloServer.main: " + re);
    }
  }

}
