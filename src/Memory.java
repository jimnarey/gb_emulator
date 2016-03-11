import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jamesnarey on 08/03/2016.
 */
public class Memory {
    
    private Bank[] memoryBanks;
    private HashMap memoryMap;

    public static void main(String args[]) {
        Memory testMemory = new Memory();
        System.out.println(testMemory.memoryBanks[0].name);

    }

    public Memory() {

        this.memoryBanks = new Bank[16];

        this.memoryBanks[0] = new Bank("restartInterruptVectors", "Restart and Interrupt Vectors", 0, 255);
        this.memoryBanks[1] = new Bank("cartridgeHeaderArea", "Cartridge Header Area", 256, 335);
        this.memoryBanks[2] = new Bank("cartridgeRom_1",  "Cartridge ROM - Bank 0 (fixed)", 336, 16383);
        this.memoryBanks[3] = new Bank("cartridgeRom_2", "Cartridge ROM - Switchable Banks 1-xx", 16384, 32767);
        this.memoryBanks[4] = new Bank("characterRam", "Character RAM", 32768, 38911);
        this.memoryBanks[5] = new Bank("bGMapData_1", "BG Map Data 1", 38912, 39935);
        this.memoryBanks[6] = new Bank("bGMapData_2", "BG Map Data 2", 39936, 40959);
        this.memoryBanks[7] = new Bank("cartridgeRam", "Cartridge RAM (If Available)", 40960, 49151);
        this.memoryBanks[8] = new Bank("internalRam_0", "Internal RAM - Bank 0 (fixed)", 49152, 53247);
        this.memoryBanks[9] = new Bank("internalRam_1_7", "Internal RAM - Bank 1-7 (switchable - CGB only)", 53248, 57343);
        this.memoryBanks[10] = new Bank("echoRam", "Echo RAM - Reserved, Do Not Use", 57344, 65023);
        this.memoryBanks[11] = new Bank("oAM", "OAM - Object Attribute Memory", 65024, 65183);
        this.memoryBanks[12] = new Bank("unusableMemory", "Unusable Memory", 65184, 65279);
        this.memoryBanks[13] = new Bank("ioRegisters", "Hardware I/O Registers",65280, 65407);
        this.memoryBanks[14] = new Bank("highRam", "Zero Page - 127 bytes/High Ram", 65408, 65534);
        this.memoryBanks[15] = new Bank("interruptEnable", "Interrupt Enable Flag", 65535, 65535);

        //Consider specifying initial size and load factor
        this.memoryMap = new HashMap();
    }


    class Bank extends ArrayList {

        private String name;
        private String description;
        private int firstByte = 0;
        private int lastByte = 0;
        //private byte[] contents;

        public Bank (String name, String description, int firstByte, int lastByte) {
            this.name = name;
            this.description = description;
            this.firstByte = firstByte;
            this.lastByte = lastByte;
            //this.contents = new byte[this.getSize()];
        }

        public int getFirstByte() {
            return firstByte;
        }

        public int getLastByte() {
            return lastByte;
        }

        public int getSize() {
            //Refelecting that the first and last bytes are part of the bank
            return (lastByte - firstByte) + 1;
        }

    }



}
