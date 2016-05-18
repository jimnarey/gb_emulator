/**
 * Created by jamesnarey on 07/05/2016.
 */
public interface GByteInterface {

    int read();

    void write(int value);

    void add(int value);

    void sub(int value);

    void inc();

    void dec();

    boolean isZero();

    boolean getCarryFlag();

    boolean getHalfFlag();

}
