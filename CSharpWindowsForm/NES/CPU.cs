using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace NES.NES {
    class CPU {
        public static byte[] PRGMEM = new byte[32768];
        public static byte NMIF = 0;
        public static byte[] Controller = new byte[2];

        public static ushort PC = 0;
        public static byte A = 0, X = 0, Y = 0, SP = 0;
        private static byte[] Controller_state = new byte[2];

        private struct SR_Struct {
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

        public static byte Read(ushort addr) {
            if (addr < 0x2000)
                return CPU_RAM[addr & 0x07FF];
            else if (addr < 0x4000)
                return NES.PPU.ReadReg(addr);
            else if (addr == 0x4014)
                return NES.PPU.ReadReg(addr);
            else if (addr >= 0x4016 && addr <= 0x4017) {
                byte data = (byte)((Controller_state[addr - 0x4016] & 0x80) > 0 ? 1 : 0);
                Controller_state[addr - 0x4016] <<= 1;
                return data;
            }
            else if (addr >= 0x8000 && addr <= 0xBFFF)
                return PRGMEM[(addr & 0x3FFF) + prg_offset];
            else if (addr >= 0xC000)
                return PRGMEM[(addr & 0x3FFF) + (PRGMEM.Length - 16 * 1024)];

            //else if (addr >= 0x8000)
            //    return PRGMEM[addr % PRGMEM.Length];
            return 0;
        }

        public static void Write(ushort addr, byte data) {
            if (addr < 0x2000)
                CPU_RAM[addr & 0x07FF] = data;
            else if (addr < 0x4000)
                NES.PPU.WriteReg(addr, data);
            else if (addr == 0x4014)
                NES.PPU.WriteReg(addr, data);
            else if (addr >= 0x4016 && addr <= 0x4017) {
                //Controller_state[addr - 0x4016] = Controller[addr - 0x4016];
                Controller_state[0] = Controller[0];
                Controller_state[1] = Controller[1];
            }
            else if (addr >= 0x8000) {
                if ((NES.PPU.Mapper1 >> 4) == 3)
                    NES.PPU.chr_offset = (data & 0x03) * 8192;
                else
                    prg_offset = data * 16384;
            }

            //else if (addr > 0x8000)
            //    PRGMEM[addr % PRGMEM.Length] = data;
        }

        private static void PUSH(byte value) {
            Write((ushort)(0x100 + SP), value);
            SP--;
        }

        private static byte POP() {
            SP++;
            return Read((ushort)(0x100 + SP));
        }

        private static void SetSR(byte value) {
            SR.C = ((value & 0x01) > 0) ? 1 : 0;
            SR.Z = ((value & 0x02) > 0) ? 1 : 0;
            SR.I = ((value & 0x04) > 0) ? 1 : 0;
            SR.D = ((value & 0x08) > 0) ? 1 : 0;
            SR.B = ((value & 0x10) > 0) ? 1 : 0;
            SR.U = ((value & 0x20) > 0) ? 1 : 0;
            SR.V = ((value & 0x40) > 0) ? 1 : 0;
            SR.N = ((value & 0x80) > 0) ? 1 : 0;
        }

        private static byte GetSR() {
            byte res = (byte)((SR.C > 0) ? 0x01 : 0x00);
            res |= (byte)((SR.Z > 0) ? 0x02 : 0x00);
            res |= (byte)((SR.I > 0) ? 0x04 : 0x00);
            res |= (byte)((SR.D > 0) ? 0x08 : 0x00);
            res |= (byte)((SR.B > 0) ? 0x10 : 0x00);
            res |= (byte)((SR.U > 0) ? 0x20 : 0x00);
            res |= (byte)((SR.V > 0) ? 0x40 : 0x00);
            res |= (byte)((SR.N > 0) ? 0x80 : 0x00);
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
            byte lo = Read(0xFFFC);
            byte hi = Read(0xFFFD);
            PC = (ushort)((hi << 8) | lo);
            for (int i = 0; i < CPU_RAM.Length; i++)
                CPU_RAM[i] = 0;
        }

        public static void NMI_INT() {
            PUSH((byte)(PC >> 8));
            PUSH((byte)PC);
            PUSH(GetSR());
            SR.B = 0;
            SR.I = 1;
            SR.U = 1;
            byte lo = Read(0xFFFA);
            byte hi = Read(0xFFFB);
            PC = (ushort)((hi << 8) | lo);
            NMIF = 0;
            cycles = 7;
        }

        private static ushort IMM() {
            return PC++;
        }

        private static ushort ZP0() {
            return Read(PC++);
        }

        private static ushort ZPX() {
            return (ushort)((Read(PC++) + X) & 0xFF);
        }

        private static ushort ZPY() {
            return (ushort)((Read(PC++) + Y) & 0xFF);
        }

        private static ushort ABS() {
            byte lo = Read(PC++);
            byte hi = Read(PC++);
            return (ushort)((hi << 8) | lo);
        }

        private static ushort ABX() {
            byte lo = Read(PC++);
            byte hi = Read(PC++);
            ushort addr = (ushort)(((hi << 8) | lo) + X);
            if ((int)(addr >> 8) != (int)hi)
                cycles++;
            return addr;
        }

        private static ushort ABY() {
            byte lo = Read(PC++);
            byte hi = Read(PC++);
            ushort addr = (ushort)(((hi << 8) | lo) + Y);
            if ((int)(addr >> 8) != (int)hi)
                cycles++;
            return addr;
        }

        private static ushort IND() {
            byte ptr_lo = Read(PC++);
            byte ptr_hi = Read(PC++);
            ushort ptr = (ushort)((ptr_hi << 8) | ptr_lo);
            if (ptr_lo == 0xFF)
                return (ushort)((Read((ushort)(ptr & 0xFF00)) << 8) | Read((ushort)(ptr + 0)));
            return (ushort)((Read((ushort)(ptr + 1)) << 8) | Read((ushort)(ptr + 0)));
        }

        private static ushort INDX() {
            byte t = Read(PC++);
            byte lo = Read((ushort)((t + X) & 0xFF));
            byte hi = Read((ushort)((t + X + 1) & 0xFF));
            return (ushort)((hi << 8) | lo);
        }

        private static ushort INDY() {
            byte t = Read(PC++);
            byte lo = Read((ushort)(t & 0xFF));
            byte hi = Read((ushort)((t + 1) & 0xFF));
            ushort addr = (ushort)(((hi << 8) | lo) + Y);
            if ((addr >> 8) != hi)
                cycles++;
            return addr;
        }

        private static ushort REL() {
            ushort rel = Read(PC++);
            if ((rel & 0x80) > 0)
                rel |= 0xFF00;
            return rel;
        }

        private static void ADC(ushort addr) {
            byte D = Read(addr);
            ushort S = (ushort)(A + D + SR.C);
            SR.C = S >> 8;
            SR.Z = ((byte)S == 0) ? 1 : 0;
            SR.V = (((0xFF ^ A ^ D) & (A ^ S) & 0x80) >> 7) > 0 ? 1 : 0;
            SR.N = (byte)S >> 7;
            A = (byte)S;
        }

        private static void AND(ushort addr) {
            byte D = Read(addr);
            A = (byte)(A & D);
            SR.N = A >> 7;
            SR.Z = (A == 0) ? 1 : 0;
        }

        private static void ASL_A() {
            SR.C = A >> 7;
            A <<= 1;
            SR.N = A >> 7;
            SR.Z = (A == 0) ? 1 : 0;
        }

        private static void ASL(ushort addr) {
            byte D = Read(addr);
            SR.C = D >> 7;
            D <<= 1;
            SR.N = D >> 7;
            SR.Z = (D == 0) ? 1 : 0;
            Write(addr, D);
            cycles = 0;
        }

        private static void BCC(ushort rel_addr) {
            if (SR.C == 0) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BCS(ushort rel_addr) {
            if (SR.C == 1) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BEQ(ushort rel_addr) {
            if (SR.Z == 1) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BNE(ushort rel_addr) {
            if (SR.Z == 0) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BPL(ushort rel_addr) {
            if (SR.N == 0) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BVC(ushort rel_addr) {
            if (SR.V == 0) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BVS(ushort rel_addr) {
            if (SR.V == 1) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BMI(ushort rel_addr) {
            if (SR.N == 1) {
                cycles++;
                ushort addr = (ushort)(PC + rel_addr);
                if ((addr & 0xFF00) != (PC & 0xFF00))
                    cycles++;
                PC = addr;
            }
        }

        private static void BIT(ushort addr) {
            byte D = Read(addr);
            SR.Z = ((A & D) == 0) ? 1 : 0;
            SR.N = D >> 7;
            SR.V = (D >> 6) & 0x01;
        }

        private static void BRK() {
            PC++;
            PUSH((byte)(PC >> 8));
            PUSH((byte)PC);
            PUSH(GetSR());
            SR.I = 1;
            PC = (ushort)(Read(0xFFFE) | (Read(0xFFFF) << 8));
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

        private static void CMP(ushort addr) {
            byte D = Read(addr);
            SR.C = (A >= D) ? 1 : 0;
            SR.Z = (A == D) ? 1 : 0;
            SR.N = (byte)(A - D) >> 7;
        }

        private static void CPX(ushort addr) {
            byte D = Read(addr);
            SR.C = (X >= D) ? 1 : 0;
            SR.Z = (X == D) ? 1 : 0;
            SR.N = (byte)(X - D) >> 7;
        }

        private static void CPY(ushort addr) {
            byte D = Read(addr);
            SR.C = (Y >= D) ? 1 : 0;
            SR.Z = (Y == D) ? 1 : 0;
            SR.N = (byte)(Y - D) >> 7;
        }

        private static void DEC(ushort addr) {
            byte D = Read(addr);
            D--;
            Write(addr, D);
            SR.Z = (D == 0) ? 1 : 0;
            SR.N = D >> 7;
            cycles = 0;
        }

        private static void DEX() {
            X--;
            SR.Z = (X == 0) ? 1 : 0;
            SR.N = X >> 7;
        }

        private static void DEY() {
            Y--;
            SR.Z = (Y == 0) ? 1 : 0;
            SR.N = Y >> 7;
        }

        private static void EOR(ushort addr) {
            byte D = Read(addr);
            A = (byte)(A ^ D);
            SR.Z = (A == 0) ? 1 : 0;
            SR.N = A >> 7;
        }

        private static void INC(ushort addr) {
            byte D = Read(addr);
            D++;
            Write(addr, D);
            SR.Z = (D == 0) ? 1 : 0;
            SR.N = D >> 7;
            cycles = 0;
        }

        private static void INX() {
            X++;
            SR.Z = (X == 0) ? 1 : 0;
            SR.N = X >> 7;
        }

        private static void INY() {
            Y++;
            SR.Z = (Y == 0) ? 1 : 0;
            SR.N = Y >> 7;
        }

        private static void JMP(ushort rel_addr) {
            PC = rel_addr;
            cycles = 0;
        }

        private static void JSR() {
            byte lo = Read(PC++);
            byte hi = Read(PC);
            PUSH((byte)(PC >> 8));
            PUSH((byte)PC);
            PC = (ushort)((hi << 8) | lo);
            cycles = 0;
        }

        private static void LDA(ushort addr) {
            A = Read(addr);
            SR.Z = (A == 0) ? 1 : 0;
            SR.N = A >> 7;
        }

        private static void LDX(ushort addr) {
            X = Read(addr);
            SR.Z = (X == 0) ? 1 : 0;
            SR.N = X >> 7;
        }

        private static void LDY(ushort addr) {
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

        private static void LSR(ushort addr) {
            byte D = Read(addr);
            SR.C = (D & 0x01);
            D >>= 1;
            Write(addr, D);
            SR.Z = (D == 0) ? 1 : 0;
            SR.N = D >> 7;
            cycles = 0;
        }

        private static void ORA(ushort addr) {
            A = (byte)(A | Read(addr));
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
            byte C = (byte)SR.C;
            SR.C = A >> 7;
            A = (byte)((A << 1) | C);
            SR.N = A >> 7;
            SR.Z = (A == 0) ? 1 : 0;
        }

        private static void ROL(ushort addr) {
            byte D = Read(addr);
            byte C = (byte)SR.C;
            SR.C = D >> 7;
            D = (byte)((D << 1) | C);
            Write(addr, D);
            SR.N = D >> 7;
            SR.Z = (D == 0) ? 1 : 0;
            cycles = 0;
        }

        private static void ROR_A() {
            byte C = (byte)SR.C;
            SR.C = ((A & 0x01) > 0) ? 1 : 0;
            A = (byte)((A >> 1) | ((C > 0) ? 0x80 : 0x00));
            SR.N = A >> 7;
            SR.Z = (A == 0) ? 1 : 0;
        }

        private static void ROR(ushort addr) {
            byte D = Read(addr);
            byte C = (byte)SR.C;
            SR.C = ((D & 0x01) > 0) ? 1 : 0;
            D = (byte)((D >> 1) | ((C > 0) ? 0x80 : 0x00));
            Write(addr, D);
            SR.N = D >> 7;
            SR.Z = (D == 0) ? 1 : 0;
            cycles = 0;
        }

        private static void RTI() {
            SetSR(POP());
            byte PCL = POP();
            byte PCH = POP();
            PC = (ushort)((PCH << 8) | PCL);
        }

        private static void RTS() {
            byte PCL = POP();
            byte PCH = POP();
            PC = (ushort)((PCH << 8) | PCL);
            PC++;
        }

        private static void SBC(ushort addr) {
            byte D = (byte)(Read(addr) ^ 0xFF);
            ushort S = (ushort)(A + D + SR.C);
            SR.C = S >> 8;
            SR.Z = ((byte)S == 0) ? 1 : 0;
            SR.V = (((0xFF ^ A ^ D) & (A ^ S) & 0x80) >> 7) > 0 ? 1 : 0;
            SR.N = (byte)S >> 7;
            A = (byte)S;
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

        private static void STA(ushort addr) {
            Write(addr, A);
            cycles = 0;
        }

        private static void STX(ushort addr) {
            Write(addr, X);
        }

        private static void STY(ushort addr) {
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

        private static byte[] OpcodeCycle = {
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

        public static bool Clock(int n_cycle) {
            int count = 0;
            cycles = 0;
            SR.U = 1;
            if (NES.CPU.NMIF == 1) {
                NES.CPU.NMI_INT();
                count = cycles;
            }
            while (count < n_cycle) {
                cycles = 0;
                SR.U = 1;
                byte opcode = Read(PC++);
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
                            MessageBox.Show("Invalid instruction!");
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
}
