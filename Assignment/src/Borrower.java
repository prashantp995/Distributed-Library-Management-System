import java.util.Objects;

public class Borrower {

  String iteID;
  int numberOfDays;

  public String getIteID() {
    return iteID;
  }

  public void setIteID(String iteID) {
    this.iteID = iteID;
  }

  public Borrower(String iteID, int numberOfDays) {
    this.iteID = iteID;
    this.numberOfDays = numberOfDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Borrower borrower = (Borrower) o;
    return Objects.equals(iteID, borrower.iteID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iteID);
  }

  @Override
  public String toString() {
    return "Borrower{" +
        "iteID='" + iteID + '\'' +
        ", numberOfDays=" + numberOfDays +
        '}';
  }
}
