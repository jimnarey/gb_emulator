/**
 * Created by jamesnarey on 13/03/2016.
 */
public class Registers {

    // !! Consider writing 0's the the non-flag bits in F each cycle
    // !! or otherwise prevent them from being written with a 1

    public BByte A = new BByte();
    public FlagRegister F = new FlagRegister();
    public BByte B = new BByte();
    public BByte C = new BByte();
    public BByte D = new BByte();
    public BByte E = new BByte();
    public BByte H = new BByte();
    public BByte L = new BByte();

    public BShort SP = new BShort();
    public BShort PC = new BShort();
    public BShort AF = new BShort();
    public BShort BC = new BShort();
    public BShort DE = new BShort();
    public BShort HL = new BShort();

    public Registers() {

        PC.populate();
        SP.populate();

        AF.setUnit(0, F);
        AF.setUnit(1, A);

        BC.setUnit(0, C);
        BC.setUnit(1, D);

        DE.setUnit(0, E);
        DE.setUnit(1, D);

        HL.setUnit(0, L);
        HL.setUnit(1, H);


    }


}
