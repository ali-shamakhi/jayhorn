import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String str = Verifier.nondetString();
    Verifier.assume(str.length() > 0);
    assert str.equals("");
  }
}
