import java.io.IOException;
import java.util.logging.Logger;
import org.junit.Test;

public class MonTest {

  static MonRemoteServiceImpl mon;


  @Test
  public void testFindItem() throws IOException {
    Logger logger = Utilities.setupLogger(Logger.getLogger("testLogger"), "testLogger");
    LibraryService libraryService = new MonRemoteServiceImpl(logger);
    for (int i = 0; i < 1000; i++) {
      String response = libraryService.findItem("MONU1111", "DSD");
      System.out.println("--------------------");
      System.out.println(response);
    }

  }

  @Test
  public void testBorrowItem() throws IOException {
    Logger logger = Utilities.setupLogger(Logger.getLogger("testLogger"), "testLogger");
    LibraryService libraryService = new MonRemoteServiceImpl(logger);
    for (int i = 0; i < 1000; i++) {
      String response = libraryService.borrowItem("MONU11111", "CON1012", 5);
      response = libraryService.returnItem("MONU11111", "CON1012");

    }


  }

}
