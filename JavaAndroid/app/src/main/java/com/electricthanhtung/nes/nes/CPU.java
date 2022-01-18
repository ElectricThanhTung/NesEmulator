package com.electricthanhtung.nes.nes;

public class CPU {
    public static byte[] PRGMEM = new byte[32768];
    public static byte NMIF = 0;
    public static byte[] Controller = new byte[2];

    public static int PC = 0;
    public static int A = 0, X = 0, Y = 0, SP = 0;
    private static byte[] Controller_state = new byte[2];

    private static class SR_Struct {
        public int C;
        public int Z;
        public int I;
        public int D;
        public int B;
        public int U;
        public int V;
        public int N;
    };
    private static SR_Struct SR = new SR_Struct();
    private static byte cycles = 0;
    public static byte[] CPU_RAM = new byte[2048];
    private static int prg_offset = 0;

    public static int Read(int addr) {
        if (addr < 0x2000)
            return (int)CPU_RAM[addr & 0x07FF] & 0xFF;
        else if (addr < 0x4000)
            return PPU.ReadReg(addr);
        else if (addr == 0x4014)
            return PPU.ReadReg(addr);
        else if (addr >= 0x4016 && addr <= 0x4017) {
            int data = (Controller_state[addr - 0x4016] & 0x80) != 0 ? 1 : 0;
            Controller_state[addr - 0x4016] <<= 1;
            return data;
        }
        else if (addr >= 0x8000 && addr <= 0xBFFF)
            return (int)PRGMEM[(addr & 0x3FFF) + prg_offset] & 0xFF;
        else if (addr >= 0xC000)
            return (int)PRGMEM[(addr & 0x3FFF) + (PRGMEM.length - 16 * 1024)] & 0xFF;

        //else if (addr >= 0x8000)
        //    return PRGMEM[addr % PRGMEM.Length];
        return 0;
    }

    public static void Write(int addr, int data) {
        if (addr < 0x2000)
            CPU_RAM[addr & 0x07FF] = (byte)data;
        else if ((addr < 0x4000) || (addr == 0x4014))
            PPU.WriteReg(addr, data);
        else if (addr >= 0x4016 && addr <= 0x4017) {
            //Controller_state[addr - 0x4016] = Controller[addr - 0x4016];
            Controller_state[0] = Controller[0];
            Controller_state[1] = Controller[1];
        }
        else if (addr >= 0x8000) {
            if ((PPU.Mapper1 >> 4) == 3)
                PPU.chr_offset = (data & 0x03) * 8192;
            else
                prg_offset = data * 16384;
        }

        //else if (addr > 0x8000)
        //    PRGMEM[addr % PRGMEM.Length] = data;
    }

    private static void PUSH(int value) {
        Write(0x100 + SP, value);
        SP--;
    }

    private static int POP() {
        SP++;
        return Read((0x100 + SP));
    }

    private static void SetSR(int value) {
        SR.C = ((value & 0x01) != 0) ? 1 : 0;
        SR.Z = ((value & 0x02) != 0) ? 1 : 0;
        SR.I = ((value & 0x04) != 0) ? 1 : 0;
        SR.D = ((value & 0x08) != 0) ? 1 : 0;
        SR.B = ((value & 0x10) != 0) ? 1 : 0;
        SR.U = ((value & 0x20) != 0) ? 1 : 0;
        SR.V = ((value & 0x40) != 0) ? 1 : 0;
        SR.N = ((value & 0x80) != 0) ? 1 : 0;
    }

    private static int GetSR() {
        int res = (SR.C != 0) ? 0x01 : 0x00;
        res |= (SR.Z != 0) ? 0x02 : 0x00;
        res |= (SR.I != 0) ? 0x04 : 0x00;
        res |= (SR.D != 0) ? 0x08 : 0x00;
        res |= (SR.B != 0) ? 0x10 : 0x00;
        res |= (SR.U != 0) ? 0x20 : 0x00;
        res |= (SR.V != 0) ? 0x40 : 0x00;
        res |= (SR.N != 0) ? 0x80 : 0x00;
        return res;
    }

    public static void Reset() {
        SetSR(0);
        SR.U = 1;
        A = 0;
        X = 0;
        Y = 0;
        SP = 0xFD;
        NMIF = 0;
        cycles = 8;
        prg_offset = 0;
        int lo = Read(0xFFFC);
        int hi = Read(0xFFFD);
        PC = (hi << 8) | lo;
        for (int i = 0; i < CPU_RAM.length; i++)
            CPU_RAM[i] = 0;
    }

    public static void NMI_INT() {
        PUSH(PC >> 8);
        PUSH(PC & 0xFF);
        PUSH(GetSR());
        SR.B = 0;
        SR.I = 1;
        SR.U = 1;
        int lo = Read(0xFFFA);
        int hi = Read(0xFFFB);
        PC = (hi << 8) | lo;
        NMIF = 0;
        cycles = 7;
    }

    private static int IMM() {
        int ret = PC;
        PC = (PC + 1) & 0xFFFF;
        return ret;
    }

    private static int ZP0() {
        int ret = PC;
        PC = (PC + 1) & 0xFFFF;
        return Read(ret);
    }

    private static int ZPX() {
        int ret = PC;
        PC = (PC + 1) & 0xFFFF;
        return (Read(ret) + X) & 0xFF;
    }

    private static int ZPY() {
        int ret = PC;
        PC = (PC + 1) & 0xFFFF;
        return (Read(ret) + Y) & 0xFF;
    }

    private static int ABS() {
        int lo = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int hi = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        return (hi << 8) | lo;
    }

    private static int ABX() {
        int lo = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int hi = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int addr = (((hi << 8) | lo) + X) & 0xFFFF;
        if ((int)(addr >> 8) != (int)hi)
            cycles++;
        return addr;
    }

    private static int ABY() {
        int lo = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int hi = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int addr = (((hi << 8) | lo) + Y) & 0xFFFF;
        if ((addr >> 8) != hi)
            cycles++;
        return addr;
    }

    private static int IND() {
        int ptr_lo = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int ptr_hi = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int ptr = (ptr_hi << 8) | ptr_lo;
        if (ptr_lo == 0xFF)
            return (Read(ptr & 0xFF00) << 8) | Read(ptr + 0);
        return (Read(ptr + 1) << 8) | Read(ptr + 0);
    }

    private static int INDX() {
        int t = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int lo = Read((t + X) & 0xFF);
        int hi = Read((t + X + 1) & 0xFF);
        return (hi << 8) | lo;
    }

    private static int INDY() {
        int t = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int lo = Read(t & 0xFF);
        int hi = Read((t + 1) & 0xFF);
        int addr = (((hi << 8) | lo) + Y) & 0xFFFF;
        if ((addr >> 8) != hi)
            cycles++;
        return addr;
    }

    private static int REL() {
        int rel = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        if ((rel & 0x80) != 0)
            rel |= 0xFF00;
        return rel;
    }

    private static void ADC(int addr) {
        int D = Read(addr);
        int S = A + D + SR.C;
        SR.C = S >> 8;
        SR.Z = ((S & 0xFF) == 0) ? 1 : 0;
        SR.V = (((0xFF ^ A ^ D) & (A ^ S) & 0x80) >> 7) != 0 ? 1 : 0;
        SR.N = (S & 0xFF) >> 7;
        A = S & 0xFF;
    }

    private static void AND(int addr) {
        int D = Read(addr);
        A = A & D;
        SR.N = A >> 7;
        SR.Z = (A == 0) ? 1 : 0;
    }

    private static void ASL_A() {
        SR.C = A >> 7;
        A = (A << 1) & 0xFF;
        SR.N = A >> 7;
        SR.Z = (A == 0) ? 1 : 0;
    }

    private static void ASL(int addr) {
        int D = Read(addr);
        SR.C = D >> 7;
        D = (D << 1) & 0xFF;
        SR.N = D >> 7;
        SR.Z = (D == 0) ? 1 : 0;
        Write(addr, D);
        cycles = 0;
    }

    private static void BCC(int rel_addr) {
        if (SR.C == 0) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BCS(int rel_addr) {
        if (SR.C == 1) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BEQ(int rel_addr) {
        if (SR.Z == 1) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BNE(int rel_addr) {
        if (SR.Z == 0) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BPL(int rel_addr) {
        if (SR.N == 0) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BVC(int rel_addr) {
        if (SR.V == 0) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BVS(int rel_addr) {
        if (SR.V == 1) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BMI(int rel_addr) {
        if (SR.N == 1) {
            cycles++;
            int addr = (PC + rel_addr) & 0xFFFF;
            if ((addr & 0xFF00) != (PC & 0xFF00))
                cycles++;
            PC = addr;
        }
    }

    private static void BIT(int addr) {
        int D = Read(addr);
        SR.Z = ((A & D) == 0) ? 1 : 0;
        SR.N = D >> 7;
        SR.V = (D >> 6) & 0x01;
    }

    private static void BRK() {
        PC = (PC + 1) & 0xFFFF;
        PUSH(PC >> 8);
        PUSH(PC & 0xFF);
        PUSH(GetSR());
        SR.I = 1;
        PC = Read(0xFFFE) | (Read(0xFFFF) << 8);
    }

    private static void CLC() {
        SR.C = 0;
    }

    private static void CLD() {
        SR.D = 0;
    }

    private static void CLI() {
        SR.I = 0;
    }

    private static void CLV() {
        SR.V = 0;
    }

    private static void CMP(int addr) {
        int D = Read(addr);
        SR.C = (A >= D) ? 1 : 0;
        SR.Z = (A == D) ? 1 : 0;
        SR.N = ((A - D) & 0xFF) >> 7;
    }

    private static void CPX(int addr) {
        int D = Read(addr);
        SR.C = (X >= D) ? 1 : 0;
        SR.Z = (X == D) ? 1 : 0;
        SR.N = ((X - D) & 0xFF) >> 7;
    }

    private static void CPY(int addr) {
        int D = Read(addr);
        SR.C = (Y >= D) ? 1 : 0;
        SR.Z = (Y == D) ? 1 : 0;
        SR.N = ((Y - D) & 0xFF) >> 7;
    }

    private static void DEC(int addr) {
        int D = Read(addr);
        D = (D - 1) & 0xFF;
        Write(addr, D);
        SR.Z = (D == 0) ? 1 : 0;
        SR.N = D >> 7;
        cycles = 0;
    }

    private static void DEX() {
        X = (X - 1) & 0xFF;
        SR.Z = (X == 0) ? 1 : 0;
        SR.N = X >> 7;
    }

    private static void DEY() {
        Y = (Y - 1) & 0xFF;
        SR.Z = (Y == 0) ? 1 : 0;
        SR.N = Y >> 7;
    }

    private static void EOR(int addr) {
        int D = Read(addr);
        A = A ^ D;
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void INC(int addr) {
        int D = Read(addr);
        D = (D + 1) & 0xFF;
        Write(addr, D);
        SR.Z = (D == 0) ? 1 : 0;
        SR.N = D >> 7;
        cycles = 0;
    }

    private static void INX() {
        X = (X + 1) & 0xFF;
        SR.Z = (X == 0) ? 1 : 0;
        SR.N = X >> 7;
    }

    private static void INY() {
        Y = (Y + 1) & 0xFF;
        SR.Z = (Y == 0) ? 1 : 0;
        SR.N = Y >> 7;
    }

    private static void JMP(int rel_addr) {
        PC = rel_addr;
        cycles = 0;
    }

    private static void JSR() {
        int lo = Read(PC);
        PC = (PC + 1) & 0xFFFF;
        int hi = Read(PC);
        PUSH(PC >> 8);
        PUSH(PC & 0xFF);
        PC = (hi << 8) | lo;
        cycles = 0;
    }

    private static void LDA(int addr) {
        A = Read(addr);
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void LDX(int addr) {
        X = Read(addr);
        SR.Z = (X == 0) ? 1 : 0;
        SR.N = X >> 7;
    }

    private static void LDY(int addr) {
        Y = Read(addr);
        SR.Z = (Y == 0) ? 1 : 0;
        SR.N = Y >> 7;
    }

    private static void LSR_A() {
        SR.C = (A & 0x01);
        A >>= 1;
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void LSR(int addr) {
        int D = Read(addr);
        SR.C = (D & 0x01);
        D >>= 1;
        Write(addr, D);
        SR.Z = (D == 0) ? 1 : 0;
        SR.N = D >> 7;
        cycles = 0;
    }

    private static void ORA(int addr) {
        A = A | Read(addr);
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void PHA() {
        PUSH(A);
    }

    private static void PHP() {
        PUSH(GetSR());
    }

    private static void PLA() {
        A = POP();
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void PLP() {
        SetSR(POP());
    }

    private static void ROL_A() {
        int C = SR.C;
        SR.C = A >> 7;
        A = ((A << 1) | C) & 0xFF;
        SR.N = A >> 7;
        SR.Z = (A == 0) ? 1 : 0;
    }

    private static void ROL(int addr) {
        int D = Read(addr);
        int C = SR.C;
        SR.C = D >> 7;
        D = ((D << 1) | C) & 0xFF;
        Write(addr, D);
        SR.N = D >> 7;
        SR.Z = (D == 0) ? 1 : 0;
        cycles = 0;
    }

    private static void ROR_A() {
        int C = SR.C;
        SR.C = ((A & 0x01) != 0) ? 1 : 0;
        A = (A >> 1) | ((C != 0) ? 0x80 : 0x00);
        SR.N = A >> 7;
        SR.Z = (A == 0) ? 1 : 0;
    }

    private static void ROR(int addr) {
        int D = Read(addr);
        int C = SR.C;
        SR.C = ((D & 0x01) != 0) ? 1 : 0;
        D = (D >> 1) | ((C != 0) ? 0x80 : 0x00);
        Write(addr, D);
        SR.N = D >> 7;
        SR.Z = (D == 0) ? 1 : 0;
        cycles = 0;
    }

    private static void RTI() {
        SetSR(POP());
        int PCL = POP();
        int PCH = POP();
        PC = (PCH << 8) | PCL;
    }

    private static void RTS() {
        int PCL = POP();
        int PCH = POP();
        PC = (PCH << 8) | PCL;
        PC = (PC + 1) & 0xFFFF;
    }

    private static void SBC(int addr) {
        int D = Read(addr) ^ 0xFF;
        int S = A + D + SR.C;
        SR.C = S >> 8;
        SR.Z = ((S & 0xFF) == 0) ? 1 : 0;
        SR.V = (((0xFF ^ A ^ D) & (A ^ S) & 0x80) >> 7) != 0 ? 1 : 0;
        SR.N = (S & 0xFF) >> 7;
        A = S & 0xFF;
    }

    private static void SEC() {
        SR.C = 1;
    }

    private static void SED() {
        SR.D = 1;
    }

    private static void SEI() {
        SR.I = 1;
    }

    private static void STA(int addr) {
        Write(addr, A);
        cycles = 0;
    }

    private static void STX(int addr) {
        Write(addr, X);
    }

    private static void STY(int addr) {
        Write(addr, Y);
    }

    private static void TAX() {
        X = A;
        SR.Z = (X == 0) ? 1 : 0;
        SR.N = X >> 7;
    }

    private static void TAY() {
        Y = A;
        SR.Z = (Y == 0) ? 1 : 0;
        SR.N = Y >> 7;
    }

    private static void TSX() {
        X = SP;
        SR.Z = (X == 0) ? 1 : 0;
        SR.N = X >> 7;
    }

    private static void TXA() {
        A = X;
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void TYA() {
        A = Y;
        SR.Z = (A == 0) ? 1 : 0;
        SR.N = A >> 7;
    }

    private static void TXS() {
        SP = X;
    }

    private static void NOP() {

    }

    private static final byte[] OpcodeCycle = {
            7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
            2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
            2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
            2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
            2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
    };

    public static boolean Clock(int n_cycle) {
        int count = 0;
        cycles = 0;
        SR.U = 1;
        if (CPU.NMIF == 1) {
            CPU.NMI_INT();
            count = cycles;
        }
        while (count < n_cycle) {
            cycles = 0;
            SR.U = 1;
            int opcode = Read(PC);
            PC = (PC + 1) & 0xFFFF;
            switch (opcode) {
                case 0x00: BRK(); break;
                case 0x01: ORA(INDX()); break;
                case 0x05: ORA(ZP0()); break;
                case 0x06: ASL(ZP0()); break;
                case 0x08: PHP(); break;
                case 0x09: ORA(IMM()); break;
                case 0x0A: ASL_A(); break;
                case 0x0D: ORA(ABS()); break;
                case 0x0E: ASL(ABS()); break;
                case 0x10: BPL(REL()); break;
                case 0x11: ORA(INDY()); break;
                case 0x15: ORA(ZPX()); break;
                case 0x16: ASL(ZPX()); break;
                case 0x18: CLC(); break;
                case 0x19: ORA(ABY()); break;
                case 0x1D: ORA(ABX()); break;
                case 0x1E: ASL(ABX()); break;
                case 0x20: JSR(); break;
                case 0x21: AND(INDX()); break;
                case 0x24: BIT(ZP0()); break;
                case 0x25: AND(ZP0()); break;
                case 0x26: ROL(ZP0()); break;
                case 0x28: PLP(); break;
                case 0x29: AND(IMM()); break;
                case 0x2A: ROL_A(); break;
                case 0x2C: BIT(ABS()); break;
                case 0x2D: AND(ABS()); break;
                case 0x2E: ROL(ABS()); break;
                case 0x30: BMI(REL()); break;
                case 0x31: AND(INDY()); break;
                case 0x35: AND(ZPX()); break;
                case 0x36: ROL(ZPX()); break;
                case 0x38: SEC(); break;
                case 0x39: AND(ABY()); break;
                case 0x3D: AND(ABX()); break;
                case 0x3E: ROL(ABX()); break;
                case 0x40: RTI(); break;
                case 0x41: EOR(INDX()); break;
                case 0x45: EOR(ZP0()); break;
                case 0x46: LSR(ZP0()); break;
                case 0x48: PHA(); break;
                case 0x49: EOR(IMM()); break;
                case 0x4A: LSR_A(); break;
                case 0x4C: JMP(ABS()); break;
                case 0x4D: EOR(ABS()); break;
                case 0x4E: LSR(ABS()); break;
                case 0x50: BVC(REL()); break;
                case 0x51: EOR(INDY()); break;
                case 0x55: EOR(ZPX()); break;
                case 0x56: LSR(ZPX()); break;
                case 0x58: CLI(); break;
                case 0x59: EOR(ABY()); break;
                case 0x5D: EOR(ABX()); break;
                case 0x5E: LSR(ABX()); break;
                case 0x60: RTS(); break;
                case 0x61: ADC(INDX()); break;
                case 0x65: ADC(ZP0()); break;
                case 0x66: ROR(ZP0()); break;
                case 0x68: PLA(); break;
                case 0x69: ADC(IMM()); break;
                case 0x6A: ROR_A(); break;
                case 0x6C: JMP(IND()); break;
                case 0x6D: ADC(ABS()); break;
                case 0x6E: ROR(ABS()); break;
                case 0x70: BVS(REL()); break;
                case 0x71: ADC(INDY()); break;
                case 0x75: ADC(ZPX()); break;
                case 0x76: ROR(ZPX()); break;
                case 0x78: SEI(); break;
                case 0x79: ADC(ABY()); break;
                case 0x7D: ADC(ABX()); break;
                case 0x7E: ROR(ABX()); break;
                case 0x81: STA(INDX()); break;
                case 0x84: STY(ZP0()); break;
                case 0x85: STA(ZP0()); break;
                case 0x86: STX(ZP0()); break;
                case 0x88: DEY(); break;
                case 0x8A: TXA(); break;
                case 0x8C: STY(ABS()); break;
                case 0x8D: STA(ABS()); break;
                case 0x8E: STX(ABS()); break;
                case 0x90: BCC(REL()); break;
                case 0x91: STA(INDY()); break;
                case 0x94: STY(ZPX()); break;
                case 0x95: STA(ZPX()); break;
                case 0x96: STX(ZPY()); break;
                case 0x98: TYA(); break;
                case 0x99: STA(ABY()); break;
                case 0x9A: TXS(); break;
                case 0x9D: STA(ABX()); break;
                case 0xA0: LDY(IMM()); break;
                case 0xA1: LDA(INDX()); break;
                case 0xA2: LDX(IMM()); break;
                case 0xA4: LDY(ZP0()); break;
                case 0xA5: LDA(ZP0()); break;
                case 0xA6: LDX(ZP0()); break;
                case 0xA8: TAY(); break;
                case 0xA9: LDA(IMM()); break;
                case 0xAA: TAX(); break;
                case 0xAC: LDY(ABS()); break;
                case 0xAD: LDA(ABS()); break;
                case 0xAE: LDX(ABS()); break;
                case 0xB0: BCS(REL()); break;
                case 0xB1: LDA(INDY()); break;
                case 0xB4: LDY(ZPX()); break;
                case 0xB5: LDA(ZPX()); break;
                case 0xB6: LDX(ZPY()); break;
                case 0xB8: CLV(); break;
                case 0xB9: LDA(ABY()); break;
                case 0xBA: TSX(); break;
                case 0xBC: LDY(ABX()); break;
                case 0xBD: LDA(ABX()); break;
                case 0xBE: LDX(ABY()); break;
                case 0xC0: CPY(IMM()); break;
                case 0xC1: CMP(INDX()); break;
                case 0xC4: CPY(ZP0()); break;
                case 0xC5: CMP(ZP0()); break;
                case 0xC6: DEC(ZP0()); break;
                case 0xC8: INY(); break;
                case 0xC9: CMP(IMM()); break;
                case 0xCA: DEX(); break;
                case 0xCC: CPY(ABS()); break;
                case 0xCD: CMP(ABS()); break;
                case 0xCE: DEC(ABS()); break;
                case 0xD0: BNE(REL()); break;
                case 0xD1: CMP(INDY()); break;
                case 0xD5: CMP(ZPX()); break;
                case 0xD6: DEC(ZPX()); break;
                case 0xD8: CLD(); break;
                case 0xD9: CMP(ABY()); break;
                case 0xDD: CMP(ABX()); break;
                case 0xDE: DEC(ABX()); break;
                case 0xE0: CPX(IMM()); break;
                case 0xE1: SBC(INDX()); break;
                case 0xE4: CPX(ZP0()); break;
                case 0xE5: SBC(ZP0()); break;
                case 0xE6: INC(ZP0()); break;
                case 0xE8: INX(); break;
                case 0xE9: SBC(IMM()); break;
                case 0xEA: NOP(); break;
                case 0xEC: CPX(ABS()); break;
                case 0xED: SBC(ABS()); break;
                case 0xEE: INC(ABS()); break;
                case 0xF0: BEQ(REL()); break;
                case 0xF1: SBC(INDY()); break;
                case 0xF5: SBC(ZPX()); break;
                case 0xF6: INC(ZPX()); break;
                case 0xF8: SED(); break;
                case 0xF9: SBC(ABY()); break;
                case 0xFD: SBC(ABX()); break;
                case 0xFE: INC(ABX()); break;
                case 0x1C:
                case 0x3C:
                case 0x5C:
                case 0x7C:
                case 0xDC:
                case 0xFC:
                    cycles++;
                    break;
                default: {
                    NOP();
                    return false;
                }
            }
            cycles += OpcodeCycle[opcode];
            count += cycles;
        }
        return true;
    }
}