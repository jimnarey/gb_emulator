/**
 * Created by jamesnarey on 11/03/2016.
 */
public class Processor {

    protected Registers r;
    protected Memory m;
    protected int clockHz;
    protected int currentOpcodeCycles = 0;
    protected boolean CBFlag = false;
    protected boolean EIFlag = false;
    protected boolean DIFlag = false;

    public Processor(Memory memory) {

        this.r = new Registers();
        this.m = memory;
        this.clockHz = 4194304;
        // Therefore, in seconds:
        // .000,000,238,418579102 duration of each clock cycle
        // .000,000,953,674316408 duration of 4 clock cycles (smallest practical time unit?)

    }


    public void call (boolean condition) {

        if (condition) {
            // Inefficient
            GBShort nextInstAddress = new GBShort();
            // !!!!! Must check this !!!!!
            // Assume it must be PC + 3?
            nextInstAddress.write( r.PC.read() + 3);
            pushShort(nextInstAddress);
            GBShort jumpAddress = new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) );
            r.PC.write( jumpAddress.read() );
        }
        else {
            // Do nothing
        }

    }

    public void daa () {

        int correctionFactor = 0;

        if (r.A.read() > 0x99 || r.F.getC() ) {

            correctionFactor += (6 << 4);
            r.F.setC(true);

        }
        else {
            r.F.setC(false);
        }

        if ( (r.A.read() & 0x0F) > 9 || r.F.getH() ) {
            correctionFactor += 6;

        }

        if ( r.F.getN() ) {
            r.A.sub( correctionFactor );
        }
        else {
            r.A.add( correctionFactor );
        }

    }

    public void jp (boolean condition, GBDataInterface address) {

        if (condition) {
            r.PC.write( address.read() );
        }
        else {
            // Do nothing
        }

    }

    public void jr (boolean condition, int signedValue) {

        if (condition) {
            // This is terribly inefficient
            GBShort jumpAddress = new GBShort();
            jumpAddress.write( r.PC.read() + signedValue );
            r.PC.write( jumpAddress.read() );
        }
        else {
            // Do nothing
        }

    }

    public void ret (boolean condition) {

        if (condition) {
            GBShort returnAddress = new GBShort();
            returnAddress.write( popShort().read() );
            r.PC.write( returnAddress.read() );
        }
        else {
            // Do nothing
        }

    }

    public void rst (int address) {
        pushShort( r.PC );
        r.PC.write( address );

    }

    //Test that if a short is pushed, the same short is returned from a pop
    //Double check these are supposed to be LSB first
    public void pushShort (GBShort addressShort) {
        pushByte(addressShort.unit(0));
        pushByte(addressShort.unit(1));
    }

    public GBShort popShort () {
        GBShort addressShort = new GBShort();
        addressShort.setUnit(1, popByte());
        addressShort.setUnit(0, popByte());
        return addressShort;

    }

    public void pushByte (GBByte addressByte) {
        m.address( r.SP.read() ).write( addressByte.read() );
        r.SP.dec();
    }

    public GBByte popByte () {
        r.SP.inc();
        return m.address( r.SP.read() - 1 );
    }



    public void runInstruction() {
        // This needs a lot of work...
        mainTable(m.address(r.PC.read()).read());

        if (CBFlag) {
            CBFlag = false;
            cBTable(m.address(r.PC.read()).read());
        }
        else if (EIFlag) {
            EIFlag = false;
            //Run the next instruction then turn off interrupts
            // runInstruction(m.address(r.PC.read()).read());
            //...interrupts on

        }
        else if (DIFlag) {
            DIFlag = false;
            //Run the next instruction then turn off interrupts
            // runInstruction(m.address(r.PC.read()).read());
            //...interrupts off
        }

    }

    public void mainTable (int opcode) {

        switch(opcode) {

            case 0x0:
                //ins: NOP -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(1);

                break;

            case 0x1:
                //ins: LD BC,d16 -- length: 3 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.BC.write( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() );

                r.PC.add(3);

                break;

            case 0x2:
                //ins: LD (BC),A -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.BC.read() ).write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x3:
                //ins: INC BC -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.BC.inc();

                r.PC.add(1);

                break;

            case 0x4:
                //ins: INC B -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.B.inc();

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH(r.B.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x5:
                //ins: DEC B -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.B.dec();

                r.F.setZ(r.B.isZero() );
                r.F.setN( true );
                r.F.setH(r.B.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x6:
                //ins: LD B,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x7:
                //ins: RLCA -- length: 1 -- cycles: 4 -- flags: 000C

                currentOpcodeCycles = 4;

                //**manual

                r.A.rotateLeft();

                r.F.setZ( false ); // check this with another source
                r.F.setN( false );
                r.F.setH( false );
                r.F.setH(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8:
                //ins: LD (a16),SP -- length: 3 -- cycles: 20 -- flags: ----

                currentOpcodeCycles = 20;

                m.address( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() ).write( r.SP.read() );

                r.PC.add(3);

                break;

            case 0x9:
                //ins: ADD HL,BC -- length: 1 -- cycles: 8 -- flags: -0HC

                currentOpcodeCycles = 8;

                r.HL.add( r.BC.read() );

                r.F.setN( false );
                r.F.setH(r.HL.getHalfFlag() );
                r.F.setC(r.HL.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x0A:
                //ins: LD A,(BC) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.write( m.address( r.BC.read() ).read() );

                r.PC.add(1);

                break;

            case 0x0B:
                //ins: DEC BC -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.BC.dec();

                r.PC.add(1);

                break;

            case 0x0C:
                //ins: INC C -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.C.inc();

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH(r.C.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x0D:
                //ins: DEC C -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.C.dec();

                r.F.setZ(r.C.isZero() );
                r.F.setN( true );
                r.F.setH(r.C.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x0E:
                //ins: LD C,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x0F:
                //ins: RRCA -- length: 1 -- cycles: 4 -- flags: 000C

                currentOpcodeCycles = 4;

                //**manual

                r.A.rotateRight();

                r.F.setZ( false ); // check this with another source
                r.F.setN( false );
                r.F.setH( false );
                r.F.setH(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x10:
                //ins: STOP 0 -- length: 2 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(2);

                break;

            case 0x11:
                //ins: LD DE,d16 -- length: 3 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.DE.write( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() );

                r.PC.add(3);

                break;

            case 0x12:
                //ins: LD (DE),A -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.DE.read() ).write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x13:
                //ins: INC DE -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.DE.inc();

                r.PC.add(1);

                break;

            case 0x14:
                //ins: INC D -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.D.inc();

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH(r.D.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x15:
                //ins: DEC D -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.D.dec();

                r.F.setZ(r.D.isZero() );
                r.F.setN( true );
                r.F.setH(r.D.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x16:
                //ins: LD D,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x17:
                //ins: RLA -- length: 1 -- cycles: 4 -- flags: 000C

                currentOpcodeCycles = 4;

                //**manual

                r.A.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ( false ); // check this with another source
                r.F.setN( false );
                r.F.setH( false );
                r.F.setH(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x18:
                //ins: JR r8 -- length: 2 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                jr( true, m.address( r.PC.read() + 1 ).readSigned() );

                r.PC.add(2);

                break;

            case 0x19:
                //ins: ADD HL,DE -- length: 1 -- cycles: 8 -- flags: -0HC

                currentOpcodeCycles = 8;

                r.HL.add( r.DE.read() );

                r.F.setN( false );
                r.F.setH(r.HL.getHalfFlag() );
                r.F.setC(r.HL.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x1A:
                //ins: LD A,(DE) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.write( m.address( r.DE.read() ).read() );

                r.PC.add(1);

                break;

            case 0x1B:
                //ins: DEC DE -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.DE.dec();

                r.PC.add(1);

                break;

            case 0x1C:
                //ins: INC E -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.E.inc();

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH(r.E.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x1D:
                //ins: DEC E -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.E.dec();

                r.F.setZ(r.E.isZero() );
                r.F.setN( true );
                r.F.setH(r.E.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x1E:
                //ins: LD E,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x1F:
                //ins: RRA -- length: 1 -- cycles: 4 -- flags: 000C

                currentOpcodeCycles = 4;

                //**manual

                r.A.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ( false ); // check this with another source
                r.F.setN( false );
                r.F.setH( false );
                r.F.setH(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x20:
                //ins: JR NZ,r8 -- length: 2 -- cycles: 12/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;12/8

                jr( r.F.getNotZ(), m.address( r.PC.read() + 1 ).readSigned() );

                r.PC.add(2);

                break;

            case 0x21:
                //ins: LD HL,d16 -- length: 3 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.HL.write( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() );

                r.PC.add(3);

                break;

            case 0x22:
                //ins: LD (HL+),A -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                //**manual

                m.address( r.HL.read() ).write( r.A.read() );
                r.HL.inc();

                r.PC.add(1);

                break;

            case 0x23:
                //ins: INC HL -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.HL.inc();

                r.PC.add(1);

                break;

            case 0x24:
                //ins: INC H -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.H.inc();

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH(r.H.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x25:
                //ins: DEC H -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.H.dec();

                r.F.setZ(r.H.isZero() );
                r.F.setN( true );
                r.F.setH(r.H.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x26:
                //ins: LD H,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x27:
                //ins: DAA -- length: 1 -- cycles: 4 -- flags: Z-0C

                currentOpcodeCycles = 4;

                // Some dispute as to flags. Above not the same as
                // http://www.worldofspectrum.org/faq/reference/z80reference.htm#DAA
                // Note carry flag is set in daa method
                // !!!check!!! half carry is set as part of the add/sub method called in daa

                //**manual

                daa();

                r.F.setZ( r.A.isZero() );


                r.PC.add(1);

                break;

            case 0x28:
                //ins: JR Z,r8 -- length: 2 -- cycles: 12/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;12/8

                jr( r.F.getZ(), m.address( r.PC.read() + 1 ).readSigned() );

                r.PC.add(2);

                break;

            case 0x29:
                //ins: ADD HL,HL -- length: 1 -- cycles: 8 -- flags: -0HC

                currentOpcodeCycles = 8;

                r.HL.add( r.HL.read() );

                r.F.setN( false );
                r.F.setH(r.HL.getHalfFlag() );
                r.F.setC(r.HL.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x2A:
                //ins: LD A,(HL+) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                //**manual

                r.A.write( m.address( r.HL.read() ).read() );
                r.HL.inc();

                r.PC.add(1);

                break;

            case 0x2B:
                //ins: DEC HL -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.HL.dec();

                r.PC.add(1);

                break;

            case 0x2C:
                //ins: INC L -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.L.inc();

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH(r.L.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x2D:
                //ins: DEC L -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.L.dec();

                r.F.setZ(r.L.isZero() );
                r.F.setN( true );
                r.F.setH(r.L.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x2E:
                //ins: LD L,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x2F:
                //ins: CPL -- length: 1 -- cycles: 4 -- flags: -11-

                currentOpcodeCycles = 4;

                //**manual

                r.A.complement();

                r.F.setN( true );
                r.F.setH( true );

                r.PC.add(1);

                break;

            case 0x30:
                //ins: JR NC,r8 -- length: 2 -- cycles: 12/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;12/8

                jr( r.F.getNotC(), m.address( r.PC.read() + 1 ).readSigned() );

                r.PC.add(2);

                break;

            case 0x31:
                //ins: LD SP,d16 -- length: 3 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.SP.write( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() );

                r.PC.add(3);

                break;

            case 0x32:
                //ins: LD (HL-),A -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                //**manual

                m.address( r.HL.read() ).write( r.A.read() );
                r.HL.dec();

                r.PC.add(1);

                break;

            case 0x33:
                //ins: INC SP -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.SP.inc();

                r.PC.add(1);

                break;

            case 0x34:
                //ins: INC (HL) -- length: 1 -- cycles: 12 -- flags: Z0H-

                currentOpcodeCycles = 12;

                m.address( r.HL.read() ).inc();

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH(m.address( r.HL.read() ).getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x35:
                //ins: DEC (HL) -- length: 1 -- cycles: 12 -- flags: Z1H-

                currentOpcodeCycles = 12;

                m.address( r.HL.read() ).dec();

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( true );
                r.F.setH(m.address( r.HL.read() ).getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x36:
                //ins: LD (HL),d8 -- length: 2 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                m.address( r.HL.read() ).write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x37:
                //ins: SCF -- length: 1 -- cycles: 4 -- flags: -001

                currentOpcodeCycles = 4;

                //**manual

                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( true ); // This is the whole instruction

                r.PC.add(1);

                break;

            case 0x38:
                //ins: JR C,r8 -- length: 2 -- cycles: 12/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;12/8

                jr( r.F.getC(), m.address( r.PC.read() + 1 ).readSigned() );

                r.PC.add(2);

                break;

            case 0x39:
                //ins: ADD HL,SP -- length: 1 -- cycles: 8 -- flags: -0HC

                currentOpcodeCycles = 8;

                r.HL.add( r.SP.read() );

                r.F.setN( false );
                r.F.setH(r.HL.getHalfFlag() );
                r.F.setC(r.HL.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x3A:
                //ins: LD A,(HL-) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                //**manual

                r.A.write( m.address( r.HL.read() ).read() );
                r.HL.dec();

                r.PC.add(1);

                break;

            case 0x3B:
                //ins: DEC SP -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.SP.dec();

                r.PC.add(1);

                break;

            case 0x3C:
                //ins: INC A -- length: 1 -- cycles: 4 -- flags: Z0H-

                currentOpcodeCycles = 4;

                r.A.inc();

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x3D:
                //ins: DEC A -- length: 1 -- cycles: 4 -- flags: Z1H-

                currentOpcodeCycles = 4;

                r.A.dec();

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );

                r.PC.add(1);

                break;

            case 0x3E:
                //ins: LD A,d8 -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.write( m.address( r.PC.read() + 1 ).read() );

                r.PC.add(2);

                break;

            case 0x3F:
                //ins: CCF -- length: 1 -- cycles: 4 -- flags: -00C

                currentOpcodeCycles = 4;

                //**manual

                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( !r.F.getC() ); // This is the whole instruction

                r.PC.add(1);

                break;

            case 0x40:
                //ins: LD B,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x41:
                //ins: LD B,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x42:
                //ins: LD B,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x43:
                //ins: LD B,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x44:
                //ins: LD B,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x45:
                //ins: LD B,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x46:
                //ins: LD B,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x47:
                //ins: LD B,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.B.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x48:
                //ins: LD C,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x49:
                //ins: LD C,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x4A:
                //ins: LD C,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x4B:
                //ins: LD C,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x4C:
                //ins: LD C,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x4D:
                //ins: LD C,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x4E:
                //ins: LD C,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x4F:
                //ins: LD C,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.C.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x50:
                //ins: LD D,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x51:
                //ins: LD D,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x52:
                //ins: LD D,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x53:
                //ins: LD D,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x54:
                //ins: LD D,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x55:
                //ins: LD D,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x56:
                //ins: LD D,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x57:
                //ins: LD D,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.D.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x58:
                //ins: LD E,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x59:
                //ins: LD E,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x5A:
                //ins: LD E,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x5B:
                //ins: LD E,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x5C:
                //ins: LD E,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x5D:
                //ins: LD E,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x5E:
                //ins: LD E,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x5F:
                //ins: LD E,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.E.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x60:
                //ins: LD H,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x61:
                //ins: LD H,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x62:
                //ins: LD H,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x63:
                //ins: LD H,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x64:
                //ins: LD H,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x65:
                //ins: LD H,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x66:
                //ins: LD H,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x67:
                //ins: LD H,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.H.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x68:
                //ins: LD L,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x69:
                //ins: LD L,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x6A:
                //ins: LD L,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x6B:
                //ins: LD L,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x6C:
                //ins: LD L,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x6D:
                //ins: LD L,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x6E:
                //ins: LD L,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x6F:
                //ins: LD L,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.L.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x70:
                //ins: LD (HL),B -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x71:
                //ins: LD (HL),C -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x72:
                //ins: LD (HL),D -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x73:
                //ins: LD (HL),E -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x74:
                //ins: LD (HL),H -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x75:
                //ins: LD (HL),L -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x76:
                //ins: HALT -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(1);

                break;

            case 0x77:
                //ins: LD (HL),A -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.HL.read() ).write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x78:
                //ins: LD A,B -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.B.read() );

                r.PC.add(1);

                break;

            case 0x79:
                //ins: LD A,C -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.C.read() );

                r.PC.add(1);

                break;

            case 0x7A:
                //ins: LD A,D -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.D.read() );

                r.PC.add(1);

                break;

            case 0x7B:
                //ins: LD A,E -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.E.read() );

                r.PC.add(1);

                break;

            case 0x7C:
                //ins: LD A,H -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.H.read() );

                r.PC.add(1);

                break;

            case 0x7D:
                //ins: LD A,L -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.L.read() );

                r.PC.add(1);

                break;

            case 0x7E:
                //ins: LD A,(HL) -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.write( m.address( r.HL.read() ).read() );

                r.PC.add(1);

                break;

            case 0x7F:
                //ins: LD A,A -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                r.A.write( r.A.read() );

                r.PC.add(1);

                break;

            case 0x80:
                //ins: ADD A,B -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x81:
                //ins: ADD A,C -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x82:
                //ins: ADD A,D -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x83:
                //ins: ADD A,E -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x84:
                //ins: ADD A,H -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x85:
                //ins: ADD A,L -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x86:
                //ins: ADD A,(HL) -- length: 1 -- cycles: 8 -- flags: Z0HC

                currentOpcodeCycles = 8;

                r.A.add( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x87:
                //ins: ADD A,A -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x88:
                //ins: ADC A,B -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.B.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x89:
                //ins: ADC A,C -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.C.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8A:
                //ins: ADC A,D -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.D.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8B:
                //ins: ADC A,E -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.E.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8C:
                //ins: ADC A,H -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.H.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8D:
                //ins: ADC A,L -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.L.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8E:
                //ins: ADC A,(HL) -- length: 1 -- cycles: 8 -- flags: Z0HC

                currentOpcodeCycles = 8;

                r.A.add( m.address( r.HL.read() ).read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x8F:
                //ins: ADC A,A -- length: 1 -- cycles: 4 -- flags: Z0HC

                currentOpcodeCycles = 4;

                r.A.add( r.A.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x90:
                //ins: SUB B -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x91:
                //ins: SUB C -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x92:
                //ins: SUB D -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x93:
                //ins: SUB E -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x94:
                //ins: SUB H -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x95:
                //ins: SUB L -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x96:
                //ins: SUB (HL) -- length: 1 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.sub( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x97:
                //ins: SUB A -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x98:
                //ins: SBC A,B -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.B.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x99:
                //ins: SBC A,C -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.C.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9A:
                //ins: SBC A,D -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.D.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9B:
                //ins: SBC A,E -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.E.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9C:
                //ins: SBC A,H -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.H.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9D:
                //ins: SBC A,L -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.L.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9E:
                //ins: SBC A,(HL) -- length: 1 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.sub( m.address( r.HL.read() ).read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0x9F:
                //ins: SBC A,A -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.sub( r.A.read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xA0:
                //ins: AND B -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA1:
                //ins: AND C -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA2:
                //ins: AND D -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA3:
                //ins: AND E -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA4:
                //ins: AND H -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA5:
                //ins: AND L -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA6:
                //ins: AND (HL) -- length: 1 -- cycles: 8 -- flags: Z010

                currentOpcodeCycles = 8;

                r.A.and( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA7:
                //ins: AND A -- length: 1 -- cycles: 4 -- flags: Z010

                currentOpcodeCycles = 4;

                r.A.and( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA8:
                //ins: XOR B -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xA9:
                //ins: XOR C -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAA:
                //ins: XOR D -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAB:
                //ins: XOR E -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAC:
                //ins: XOR H -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAD:
                //ins: XOR L -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAE:
                //ins: XOR (HL) -- length: 1 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.xor( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xAF:
                //ins: XOR A -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.xor( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB0:
                //ins: OR B -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB1:
                //ins: OR C -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB2:
                //ins: OR D -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB3:
                //ins: OR E -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB4:
                //ins: OR H -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB5:
                //ins: OR L -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB6:
                //ins: OR (HL) -- length: 1 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.or( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB7:
                //ins: OR A -- length: 1 -- cycles: 4 -- flags: Z000

                currentOpcodeCycles = 4;

                r.A.or( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(1);

                break;

            case 0xB8:
                //ins: CP B -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.B.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xB9:
                //ins: CP C -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.C.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBA:
                //ins: CP D -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.D.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBB:
                //ins: CP E -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.E.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBC:
                //ins: CP H -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.H.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBD:
                //ins: CP L -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.L.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBE:
                //ins: CP (HL) -- length: 1 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.cp( m.address( r.HL.read() ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xBF:
                //ins: CP A -- length: 1 -- cycles: 4 -- flags: Z1HC

                currentOpcodeCycles = 4;

                r.A.cp( r.A.read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(1);

                break;

            case 0xC0:
                //ins: RET NZ -- length: 1 -- cycles: 20/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;20/8

                ret( r.F.getNotZ() );

                r.PC.add(1);

                break;

            case 0xC1:
                //ins: POP BC -- length: 1 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.BC.write( popShort().read() );

                r.PC.add(1);

                break;

            case 0xC2:
                //ins: JP NZ,a16 -- length: 3 -- cycles: 16/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;16/12

                jp( r.F.getNotZ(), new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ) );

                r.PC.add(3);

                break;

            case 0xC3:
                //ins: JP a16 -- length: 3 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                jp( true, new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ) );

                r.PC.add(3);

                break;

            case 0xC4:
                //ins: CALL NZ,a16 -- length: 3 -- cycles: 24/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;24/12

                call( r.F.getNotZ() );

                r.PC.add(3);

                break;

            case 0xC5:
                //ins: PUSH BC -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                pushShort( r.BC );

                r.PC.add(1);

                break;

            case 0xC6:
                //ins: ADD A,d8 -- length: 2 -- cycles: 8 -- flags: Z0HC

                currentOpcodeCycles = 8;

                r.A.add( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xC7:
                //ins: RST 00H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 0 );

                r.PC.add(1);

                break;

            case 0xC8:
                //ins: RET Z -- length: 1 -- cycles: 20/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;20/8

                ret( r.F.getZ() );

                r.PC.add(1);

                break;

            case 0xC9:
                //ins: RET -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                ret( true );

                r.PC.add(1);

                break;

            case 0xCA:
                //ins: JP Z,a16 -- length: 3 -- cycles: 16/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;16/12

                jp( r.F.getZ(), new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ) );

                r.PC.add(3);

                break;

            case 0xCB:
                //ins: PREFIX CB -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(1);

                break;

            case 0xCC:
                //ins: CALL Z,a16 -- length: 3 -- cycles: 24/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;24/12

                call( r.F.getZ() );

                r.PC.add(3);

                break;

            case 0xCD:
                //ins: CALL a16 -- length: 3 -- cycles: 24 -- flags: ----

                currentOpcodeCycles = 24;

                call( true );

                r.PC.add(3);

                break;

            case 0xCE:
                //ins: ADC A,d8 -- length: 2 -- cycles: 8 -- flags: Z0HC

                currentOpcodeCycles = 8;

                r.A.add( m.address( r.PC.read() + 1 ).read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xCF:
                //ins: RST 08H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 8 );

                r.PC.add(1);

                break;

            case 0xD0:
                //ins: RET NC -- length: 1 -- cycles: 20/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;20/8

                ret( r.F.getNotC() );

                r.PC.add(1);

                break;

            case 0xD1:
                //ins: POP DE -- length: 1 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.DE.write( popShort().read() );

                r.PC.add(1);

                break;

            case 0xD2:
                //ins: JP NC,a16 -- length: 3 -- cycles: 16/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;16/12

                jp( r.F.getNotC(), new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ) );

                r.PC.add(3);

                break;

            case 0xD4:
                //ins: CALL NC,a16 -- length: 3 -- cycles: 24/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;24/12

                call( r.F.getNotC() );

                r.PC.add(3);

                break;

            case 0xD5:
                //ins: PUSH DE -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                pushShort( r.DE );

                r.PC.add(1);

                break;

            case 0xD6:
                //ins: SUB d8 -- length: 2 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.sub( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xD7:
                //ins: RST 10H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 16 );

                r.PC.add(1);

                break;

            case 0xD8:
                //ins: RET C -- length: 1 -- cycles: 20/8 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;20/8

                ret( r.F.getC() );

                r.PC.add(1);

                break;

            case 0xD9:
                //ins: RETI -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                //**missing

                r.PC.add(1);

                break;

            case 0xDA:
                //ins: JP C,a16 -- length: 3 -- cycles: 16/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;16/12

                jp( r.F.getC(), new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ) );

                r.PC.add(3);

                break;

            case 0xDC:
                //ins: CALL C,a16 -- length: 3 -- cycles: 24/12 -- flags: ----

                //**currentOpcodeCycles = CONDITIONAL;24/12

                call( r.F.getC() );

                r.PC.add(3);

                break;

            case 0xDE:
                //ins: SBC A,d8 -- length: 2 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.sub( m.address( r.PC.read() + 1 ).read() + (r.F.checkBit(4) ? 1 : 0) );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xDF:
                //ins: RST 18H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 24 );

                r.PC.add(1);

                break;

            case 0xE0:
                //ins: LDH (a8),A -- length: 2 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                //**manual

                m.address( r.PC.read() + 0xFF00 + 1 ).write( r.A.read() ); // check this is correct

                r.PC.add(2);

                break;

            case 0xE1:
                //ins: POP HL -- length: 1 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                r.HL.write( popShort().read() );

                r.PC.add(1);

                break;

            case 0xE2:
                //ins: LD (C),A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                m.address( r.C.read() ).write( r.A.read() );

                r.PC.add(2);

                break;

            case 0xE5:
                //ins: PUSH HL -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                pushShort( r.HL );

                r.PC.add(1);

                break;

            case 0xE6:
                //ins: AND d8 -- length: 2 -- cycles: 8 -- flags: Z010

                currentOpcodeCycles = 8;

                r.A.and( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0xE7:
                //ins: RST 20H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 32 );

                r.PC.add(1);

                break;

            case 0xE8:
                //ins: ADD SP,r8 -- length: 2 -- cycles: 16 -- flags: 00HC

                currentOpcodeCycles = 16;

                r.SP.add( m.address( r.PC.read() + 1 ).readSigned() );

                r.F.setZ( false );
                r.F.setN( false );
                r.F.setH(r.SP.getHalfFlag() );
                r.F.setC(r.SP.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xE9:
                //ins: JP (HL) -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                jp( true, m.address( r.HL.read() ) );

                r.PC.add(1);

                break;

            case 0xEA:
                //ins: LD (a16),A -- length: 3 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() ).write( r.A.read() );

                r.PC.add(3);

                break;

            case 0xEE:
                //ins: XOR d8 -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.xor( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0xEF:
                //ins: RST 28H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 40 );

                r.PC.add(1);

                break;

            case 0xF0:
                //ins: LDH A,(a8) -- length: 2 -- cycles: 12 -- flags: ----

                currentOpcodeCycles = 12;

                //**manual

                r.A.write( m.address( r.PC.read() + 0xFF00 + 1 ).read() ); // check this is correct

                r.PC.add(2);

                break;

            case 0xF1:
                //ins: POP AF -- length: 1 -- cycles: 12 -- flags: ZNHC

                currentOpcodeCycles = 12;

                r.AF.write( popShort().read() );

                r.PC.add(1);

                break;

            case 0xF2:
                //ins: LD A,(C) -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.write( m.address( r.C.read() ).read() );

                r.PC.add(2);

                break;

            case 0xF3:
                //ins: DI -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(1);

                break;

            case 0xF5:
                //ins: PUSH AF -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                pushShort( r.AF );

                r.PC.add(1);

                break;

            case 0xF6:
                //ins: OR d8 -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.or( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0xF7:
                //ins: RST 30H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 48 );

                r.PC.add(1);

                break;

            case 0xF8:
                //ins: LD HL,SP+r8 -- length: 2 -- cycles: 12 -- flags: 00HC

                currentOpcodeCycles = 12;

                //**manual

                int value = m.address( r.PC.read() + 1 ).readSigned();
                GBShort tempShort = new GBShort();
                tempShort.write( r.SP.read() );
                tempShort.add(value);
                r.HL.write( tempShort.read() );

                r.F.setZ( false );
                r.F.setN( false );
                r.F.setH( tempShort.getHalfFlag() );
                r.F.setC( tempShort.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xF9:
                //ins: LD SP,HL -- length: 1 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.SP.write( r.HL.read() );

                r.PC.add(1);

                break;

            case 0xFA:
                //ins: LD A,(a16) -- length: 3 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                r.A.write( m.address( new GBShort( m.address( r.PC.read() + 1 ), m.address( r.PC.read() + 2 ) ).read() ).read() );

                r.PC.add(3);

                break;

            case 0xFB:
                //ins: EI -- length: 1 -- cycles: 4 -- flags: ----

                currentOpcodeCycles = 4;

                //**missing

                r.PC.add(1);

                break;

            case 0xFE:
                //ins: CP d8 -- length: 2 -- cycles: 8 -- flags: Z1HC

                currentOpcodeCycles = 8;

                r.A.cp( m.address( r.PC.read() + 1 ).read() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( true );
                r.F.setH(r.A.getHalfFlag() );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0xFF:
                //ins: RST 38H -- length: 1 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                rst( 56 );

                r.PC.add(1);

                break;


        }
    }

    public void cBTable (int opcode) {

        switch(opcode) {

            case 0x00:
                //ins: RLC B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateLeft();

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x01:
                //ins: RLC C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateLeft();

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x02:
                //ins: RLC D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateLeft();

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x03:
                //ins: RLC E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateLeft();

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x04:
                //ins: RLC H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateLeft();

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x05:
                //ins: RLC L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateLeft();

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x06:
                //ins: RLC (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateLeft();

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x07:
                //ins: RLC A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateLeft();

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x08:
                //ins: RRC B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateRight();

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x09:
                //ins: RRC C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateRight();

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0A:
                //ins: RRC D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateRight();

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0B:
                //ins: RRC E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateRight();

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0C:
                //ins: RRC H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateRight();

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0D:
                //ins: RRC L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateRight();

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0E:
                //ins: RRC (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateRight();

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x0F:
                //ins: RRC A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateRight();

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x10:
                //ins: RL B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x11:
                //ins: RL C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x12:
                //ins: RL D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x13:
                //ins: RL E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x14:
                //ins: RL H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x15:
                //ins: RL L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x16:
                //ins: RL (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x17:
                //ins: RL A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateLeftThroughFlag( r.F.getC() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x18:
                //ins: RR B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x19:
                //ins: RR C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1A:
                //ins: RR D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1B:
                //ins: RR E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1C:
                //ins: RR H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1D:
                //ins: RR L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1E:
                //ins: RR (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x1F:
                //ins: RR A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateRightThroughFlag( r.F.getC() );

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x20:
                //ins: SLA B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateLeftThroughFlag(false);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x21:
                //ins: SLA C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateLeftThroughFlag(false);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x22:
                //ins: SLA D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateLeftThroughFlag(false);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x23:
                //ins: SLA E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateLeftThroughFlag(false);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x24:
                //ins: SLA H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateLeftThroughFlag(false);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x25:
                //ins: SLA L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateLeftThroughFlag(false);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x26:
                //ins: SLA (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateLeftThroughFlag(false);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x27:
                //ins: SLA A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateLeftThroughFlag(false);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x28:
                //ins: SRA B -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.B.rotateRightThroughFlag(r.B.checkBit(7));

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x29:
                //ins: SRA C -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.C.rotateRightThroughFlag(r.C.checkBit(7));

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2A:
                //ins: SRA D -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.D.rotateRightThroughFlag(r.D.checkBit(7));

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2B:
                //ins: SRA E -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.E.rotateRightThroughFlag(r.E.checkBit(7));

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2C:
                //ins: SRA H -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.H.rotateRightThroughFlag(r.H.checkBit(7));

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2D:
                //ins: SRA L -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.L.rotateRightThroughFlag(r.L.checkBit(7));

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2E:
                //ins: SRA (HL) -- length: 2 -- cycles: 16 -- flags: Z000

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateRightThroughFlag(m.address( r.HL.read() ).checkBit(7));

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x2F:
                //ins: SRA A -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.rotateRightThroughFlag(r.A.checkBit(7));

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x30:
                //ins: SWAP B -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.B.swap();

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x31:
                //ins: SWAP C -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.C.swap();

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x32:
                //ins: SWAP D -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.D.swap();

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x33:
                //ins: SWAP E -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.E.swap();

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x34:
                //ins: SWAP H -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.H.swap();

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x35:
                //ins: SWAP L -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.L.swap();

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x36:
                //ins: SWAP (HL) -- length: 2 -- cycles: 16 -- flags: Z000

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).swap();

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x37:
                //ins: SWAP A -- length: 2 -- cycles: 8 -- flags: Z000

                currentOpcodeCycles = 8;

                r.A.swap();

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC( false );

                r.PC.add(2);

                break;

            case 0x38:
                //ins: SRL B -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.B.rotateRightThroughFlag(false);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.B.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x39:
                //ins: SRL C -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.C.rotateRightThroughFlag(false);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.C.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3A:
                //ins: SRL D -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.D.rotateRightThroughFlag(false);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.D.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3B:
                //ins: SRL E -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.E.rotateRightThroughFlag(false);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.E.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3C:
                //ins: SRL H -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.H.rotateRightThroughFlag(false);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.H.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3D:
                //ins: SRL L -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.L.rotateRightThroughFlag(false);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.L.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3E:
                //ins: SRL (HL) -- length: 2 -- cycles: 16 -- flags: Z00C

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).rotateRightThroughFlag(false);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(m.address( r.HL.read() ).getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x3F:
                //ins: SRL A -- length: 2 -- cycles: 8 -- flags: Z00C

                currentOpcodeCycles = 8;

                r.A.rotateRightThroughFlag(false);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( false );
                r.F.setC(r.A.getCarryFlag() );

                r.PC.add(2);

                break;

            case 0x40:
                //ins: BIT 0,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(0);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x41:
                //ins: BIT 0,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(0);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x42:
                //ins: BIT 0,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(0);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x43:
                //ins: BIT 0,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(0);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x44:
                //ins: BIT 0,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(0);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x45:
                //ins: BIT 0,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(0);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x46:
                //ins: BIT 0,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(0);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x47:
                //ins: BIT 0,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(0);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x48:
                //ins: BIT 1,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(1);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x49:
                //ins: BIT 1,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(1);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4A:
                //ins: BIT 1,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(1);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4B:
                //ins: BIT 1,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(1);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4C:
                //ins: BIT 1,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(1);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4D:
                //ins: BIT 1,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(1);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4E:
                //ins: BIT 1,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(1);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x4F:
                //ins: BIT 1,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(1);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x50:
                //ins: BIT 2,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(2);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x51:
                //ins: BIT 2,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(2);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x52:
                //ins: BIT 2,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(2);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x53:
                //ins: BIT 2,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(2);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x54:
                //ins: BIT 2,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(2);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x55:
                //ins: BIT 2,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(2);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x56:
                //ins: BIT 2,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(2);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x57:
                //ins: BIT 2,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(2);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x58:
                //ins: BIT 3,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(3);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x59:
                //ins: BIT 3,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(3);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5A:
                //ins: BIT 3,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(3);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5B:
                //ins: BIT 3,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(3);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5C:
                //ins: BIT 3,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(3);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5D:
                //ins: BIT 3,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(3);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5E:
                //ins: BIT 3,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(3);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x5F:
                //ins: BIT 3,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(3);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x60:
                //ins: BIT 4,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(4);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x61:
                //ins: BIT 4,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(4);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x62:
                //ins: BIT 4,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(4);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x63:
                //ins: BIT 4,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(4);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x64:
                //ins: BIT 4,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(4);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x65:
                //ins: BIT 4,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(4);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x66:
                //ins: BIT 4,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(4);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x67:
                //ins: BIT 4,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(4);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x68:
                //ins: BIT 5,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(5);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x69:
                //ins: BIT 5,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(5);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6A:
                //ins: BIT 5,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(5);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6B:
                //ins: BIT 5,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(5);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6C:
                //ins: BIT 5,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(5);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6D:
                //ins: BIT 5,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(5);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6E:
                //ins: BIT 5,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(5);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x6F:
                //ins: BIT 5,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(5);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x70:
                //ins: BIT 6,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(6);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x71:
                //ins: BIT 6,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(6);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x72:
                //ins: BIT 6,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(6);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x73:
                //ins: BIT 6,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(6);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x74:
                //ins: BIT 6,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(6);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x75:
                //ins: BIT 6,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(6);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x76:
                //ins: BIT 6,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(6);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x77:
                //ins: BIT 6,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(6);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x78:
                //ins: BIT 7,B -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.B.checkBit(7);

                r.F.setZ(r.B.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x79:
                //ins: BIT 7,C -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.C.checkBit(7);

                r.F.setZ(r.C.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7A:
                //ins: BIT 7,D -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.D.checkBit(7);

                r.F.setZ(r.D.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7B:
                //ins: BIT 7,E -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.E.checkBit(7);

                r.F.setZ(r.E.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7C:
                //ins: BIT 7,H -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.H.checkBit(7);

                r.F.setZ(r.H.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7D:
                //ins: BIT 7,L -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.L.checkBit(7);

                r.F.setZ(r.L.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7E:
                //ins: BIT 7,(HL) -- length: 2 -- cycles: 16 -- flags: Z01-

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).checkBit(7);

                r.F.setZ(m.address( r.HL.read() ).isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x7F:
                //ins: BIT 7,A -- length: 2 -- cycles: 8 -- flags: Z01-

                currentOpcodeCycles = 8;

                r.A.checkBit(7);

                r.F.setZ(r.A.isZero() );
                r.F.setN( false );
                r.F.setH( true );

                r.PC.add(2);

                break;

            case 0x80:
                //ins: RES 0,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x81:
                //ins: RES 0,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x82:
                //ins: RES 0,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x83:
                //ins: RES 0,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x84:
                //ins: RES 0,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x85:
                //ins: RES 0,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x86:
                //ins: RES 0,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(0, false);

                r.PC.add(2);

                break;

            case 0x87:
                //ins: RES 0,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(0, false);

                r.PC.add(2);

                break;

            case 0x88:
                //ins: RES 1,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x89:
                //ins: RES 1,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8A:
                //ins: RES 1,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8B:
                //ins: RES 1,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8C:
                //ins: RES 1,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8D:
                //ins: RES 1,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8E:
                //ins: RES 1,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(1, false);

                r.PC.add(2);

                break;

            case 0x8F:
                //ins: RES 1,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(1, false);

                r.PC.add(2);

                break;

            case 0x90:
                //ins: RES 2,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x91:
                //ins: RES 2,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x92:
                //ins: RES 2,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x93:
                //ins: RES 2,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x94:
                //ins: RES 2,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x95:
                //ins: RES 2,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x96:
                //ins: RES 2,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(2, false);

                r.PC.add(2);

                break;

            case 0x97:
                //ins: RES 2,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(2, false);

                r.PC.add(2);

                break;

            case 0x98:
                //ins: RES 3,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x99:
                //ins: RES 3,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9A:
                //ins: RES 3,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9B:
                //ins: RES 3,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9C:
                //ins: RES 3,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9D:
                //ins: RES 3,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9E:
                //ins: RES 3,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(3, false);

                r.PC.add(2);

                break;

            case 0x9F:
                //ins: RES 3,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(3, false);

                r.PC.add(2);

                break;

            case 0xA0:
                //ins: RES 4,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA1:
                //ins: RES 4,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA2:
                //ins: RES 4,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA3:
                //ins: RES 4,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA4:
                //ins: RES 4,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA5:
                //ins: RES 4,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA6:
                //ins: RES 4,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA7:
                //ins: RES 4,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(4, false);

                r.PC.add(2);

                break;

            case 0xA8:
                //ins: RES 5,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xA9:
                //ins: RES 5,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAA:
                //ins: RES 5,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAB:
                //ins: RES 5,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAC:
                //ins: RES 5,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAD:
                //ins: RES 5,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAE:
                //ins: RES 5,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(5, false);

                r.PC.add(2);

                break;

            case 0xAF:
                //ins: RES 5,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(5, false);

                r.PC.add(2);

                break;

            case 0xB0:
                //ins: RES 6,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB1:
                //ins: RES 6,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB2:
                //ins: RES 6,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB3:
                //ins: RES 6,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB4:
                //ins: RES 6,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB5:
                //ins: RES 6,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB6:
                //ins: RES 6,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB7:
                //ins: RES 6,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(6, false);

                r.PC.add(2);

                break;

            case 0xB8:
                //ins: RES 7,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xB9:
                //ins: RES 7,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBA:
                //ins: RES 7,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBB:
                //ins: RES 7,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBC:
                //ins: RES 7,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBD:
                //ins: RES 7,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBE:
                //ins: RES 7,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(7, false);

                r.PC.add(2);

                break;

            case 0xBF:
                //ins: RES 7,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(7, false);

                r.PC.add(2);

                break;

            case 0xC0:
                //ins: SET 0,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC1:
                //ins: SET 0,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC2:
                //ins: SET 0,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC3:
                //ins: SET 0,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC4:
                //ins: SET 0,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC5:
                //ins: SET 0,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC6:
                //ins: SET 0,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC7:
                //ins: SET 0,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(0, true);

                r.PC.add(2);

                break;

            case 0xC8:
                //ins: SET 1,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xC9:
                //ins: SET 1,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCA:
                //ins: SET 1,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCB:
                //ins: SET 1,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCC:
                //ins: SET 1,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCD:
                //ins: SET 1,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCE:
                //ins: SET 1,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(1, true);

                r.PC.add(2);

                break;

            case 0xCF:
                //ins: SET 1,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(1, true);

                r.PC.add(2);

                break;

            case 0xD0:
                //ins: SET 2,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD1:
                //ins: SET 2,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD2:
                //ins: SET 2,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD3:
                //ins: SET 2,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD4:
                //ins: SET 2,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD5:
                //ins: SET 2,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD6:
                //ins: SET 2,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD7:
                //ins: SET 2,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(2, true);

                r.PC.add(2);

                break;

            case 0xD8:
                //ins: SET 3,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xD9:
                //ins: SET 3,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDA:
                //ins: SET 3,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDB:
                //ins: SET 3,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDC:
                //ins: SET 3,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDD:
                //ins: SET 3,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDE:
                //ins: SET 3,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(3, true);

                r.PC.add(2);

                break;

            case 0xDF:
                //ins: SET 3,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(3, true);

                r.PC.add(2);

                break;

            case 0xE0:
                //ins: SET 4,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE1:
                //ins: SET 4,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE2:
                //ins: SET 4,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE3:
                //ins: SET 4,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE4:
                //ins: SET 4,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE5:
                //ins: SET 4,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE6:
                //ins: SET 4,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE7:
                //ins: SET 4,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(4, true);

                r.PC.add(2);

                break;

            case 0xE8:
                //ins: SET 5,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xE9:
                //ins: SET 5,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xEA:
                //ins: SET 5,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xEB:
                //ins: SET 5,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xEC:
                //ins: SET 5,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xED:
                //ins: SET 5,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xEE:
                //ins: SET 5,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(5, true);

                r.PC.add(2);

                break;

            case 0xEF:
                //ins: SET 5,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(5, true);

                r.PC.add(2);

                break;

            case 0xF0:
                //ins: SET 6,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF1:
                //ins: SET 6,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF2:
                //ins: SET 6,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF3:
                //ins: SET 6,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF4:
                //ins: SET 6,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF5:
                //ins: SET 6,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF6:
                //ins: SET 6,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF7:
                //ins: SET 6,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(6, true);

                r.PC.add(2);

                break;

            case 0xF8:
                //ins: SET 7,B -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.B.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xF9:
                //ins: SET 7,C -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.C.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFA:
                //ins: SET 7,D -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.D.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFB:
                //ins: SET 7,E -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.E.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFC:
                //ins: SET 7,H -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.H.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFD:
                //ins: SET 7,L -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.L.setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFE:
                //ins: SET 7,(HL) -- length: 2 -- cycles: 16 -- flags: ----

                currentOpcodeCycles = 16;

                m.address( r.HL.read() ).setBit(7, true);

                r.PC.add(2);

                break;

            case 0xFF:
                //ins: SET 7,A -- length: 2 -- cycles: 8 -- flags: ----

                currentOpcodeCycles = 8;

                r.A.setBit(7, true);

                r.PC.add(2);

                break;


        }
    }







}
