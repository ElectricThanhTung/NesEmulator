using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Drawing;

namespace NES.NES {
    class PPU {
        public static byte Mapper1 = 0;
        public static int chr_offset = 0;
        public static byte[] CHRMEM = new byte[8192];
        public static byte[] PPU_Reg = new byte[8];
        private static byte[][] TableName = new byte[2][];
        private static byte[] Palette = new byte[32];

        private static short scanline = 0;
        private static byte[] OAMDATA = new byte[64 * 4];
        private static byte[] image_data = new byte[256 * 220 * 4];

        private struct OAMStruct {
            public byte y;
            public byte id;
            public byte attribute;
            public byte x;
        };

        private static OAMStruct[] spriteScanline = new OAMStruct[8];
        private static OAMStruct[] OAM = new OAMStruct[64];

        private struct vram_addr_struct {
            public byte coarse_x;
            public byte coarse_y;
            public byte nametable_x;
            public byte nametable_y;
            public byte fine_y;
            public byte unused;
        };

        private static vram_addr_struct vram_addr, tram_addr;

        private static ushort vram_addr_reg {
            set {
                vram_addr.coarse_x = (byte)(value & 0x1F);
                vram_addr.coarse_y = (byte)((value >> 5) & 0x1F);
                vram_addr.nametable_x = (byte)((value >> 10) & 0x01);
                vram_addr.nametable_y = (byte)((value >> 11) & 0x01);
                vram_addr.fine_y = (byte)((value >> 12) & 0x07);
                vram_addr.unused = (byte)((value >> 15));
            }
            get {
                return (ushort)((vram_addr.unused << 15) | (vram_addr.fine_y << 12) | (vram_addr.nametable_y << 11) | (vram_addr.nametable_x << 10) | (vram_addr.coarse_y << 5) | vram_addr.coarse_x);
            }
        }

        private static ushort tram_addr_reg {
            set {
                tram_addr.coarse_x = (byte)(value & 0x1F);
                tram_addr.coarse_y = (byte)((value >> 5) & 0x1F);
                tram_addr.nametable_x = (byte)((value >> 10) & 0x01);
                tram_addr.nametable_y = (byte)((value >> 11) & 0x01);
                tram_addr.fine_y = (byte)((value >> 12) & 0x07);
                tram_addr.unused = (byte)((value >> 15));
            }
            get {
                return (ushort)((tram_addr.unused << 15) | (tram_addr.fine_y << 12) | (tram_addr.nametable_y << 11) | (tram_addr.nametable_x << 10) | (tram_addr.coarse_y << 5) | tram_addr.coarse_x);
            }
        }

        private static Color[] palScreen = {
            Color.FromArgb(84, 84, 84),
            Color.FromArgb(0, 30, 116),
            Color.FromArgb(8, 16, 144),
            Color.FromArgb(48, 0, 136),
            Color.FromArgb(68, 0, 100),
            Color.FromArgb(92, 0, 48),
            Color.FromArgb(84, 4, 0),
            Color.FromArgb(60, 24, 0),
            Color.FromArgb(32, 42, 0),
            Color.FromArgb(8, 58, 0),
            Color.FromArgb(0, 64, 0),
            Color.FromArgb(0, 60, 0),
            Color.FromArgb(0, 50, 60),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),

            Color.FromArgb(152, 150, 152),
            Color.FromArgb(8, 76, 196),
            Color.FromArgb(48, 50, 236),
            Color.FromArgb(92, 30, 228),
            Color.FromArgb(136, 20, 176),
            Color.FromArgb(160, 20, 100),
            Color.FromArgb(152, 34, 32),
            Color.FromArgb(120, 60, 0),
            Color.FromArgb(84, 90, 0),
            Color.FromArgb(40, 114, 0),
            Color.FromArgb(8, 124, 0),
            Color.FromArgb(0, 118, 40),
            Color.FromArgb(0, 102, 120),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),

            Color.FromArgb(236, 238, 236),
            Color.FromArgb(76, 154, 236),
            Color.FromArgb(120, 124, 236),
            Color.FromArgb(176, 98, 236),
            Color.FromArgb(228, 84, 236),
            Color.FromArgb(236, 88, 180),
            Color.FromArgb(236, 106, 100),
            Color.FromArgb(212, 136, 32),
            Color.FromArgb(160, 170, 0),
            Color.FromArgb(116, 196, 0),
            Color.FromArgb(76, 208, 32),
            Color.FromArgb(56, 204, 108),
            Color.FromArgb(56, 180, 204),
            Color.FromArgb(60, 60, 60),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),

            Color.FromArgb(236, 238, 236),
            Color.FromArgb(168, 204, 236),
            Color.FromArgb(188, 188, 236),
            Color.FromArgb(212, 178, 236),
            Color.FromArgb(236, 174, 236),
            Color.FromArgb(236, 174, 212),
            Color.FromArgb(236, 180, 176),
            Color.FromArgb(228, 196, 144),
            Color.FromArgb(204, 210, 120),
            Color.FromArgb(180, 222, 120),
            Color.FromArgb(168, 226, 144),
            Color.FromArgb(152, 226, 180),
            Color.FromArgb(160, 214, 228),
            Color.FromArgb(160, 162, 160),
            Color.FromArgb(0, 0, 0),
            Color.FromArgb(0, 0, 0),
        };

        public static void PPU_Write(ushort addr, byte data) {
            if (addr >= 0x3F00) {
                addr &= 0x1F;
                //if (addr == 0x10) addr = 0x00;
                //else if (addr == 0x14) addr = 0x04;
                //else if (addr == 0x18) addr = 0x08;
                //else if (addr == 0x1C) addr = 0x0C;
                //Palette[addr] = data;

                if (addr == 0x10 || addr == 0x00) {
                    Palette[0] = Palette[0 + 16] = data;
                    Palette[4] = Palette[4 + 16] = data;
                    Palette[8] = Palette[8 + 16] = data;
                    Palette[12] = Palette[12 + 16] = data;
                }
                else if ((addr & 0x03) != 0x00)
                    Palette[addr] = data;
            }
            else if (addr >= 0x2000) {
                addr &= 0x0FFF;
                if ((Mapper1 & 0x01) > 0) {
                    // Vertical
                    if (addr <= 0x03FF)
                        TableName[0][addr & 0x03FF] = data;
                    else if (addr <= 0x07FF)
                        TableName[1][addr & 0x03FF] = data;
                    else if (addr <= 0x0BFF)
                        TableName[0][addr & 0x03FF] = data;
                    else
                        TableName[1][addr & 0x03FF] = data;
                }
                else {
                    // Horizontal
                    if (addr <= 0x03FF)
                        TableName[0][addr & 0x03FF] = data;
                    else if (addr <= 0x07FF)
                        TableName[0][addr & 0x03FF] = data;
                    else if (addr <= 0x0BFF)
                        TableName[1][addr & 0x03FF] = data;
                    else
                        TableName[1][addr & 0x03FF] = data;
                }
            }
            else
                CHRMEM[addr + chr_offset] = data;
        }

        public static byte PPU_Read(ushort addr) {
            if (addr >= 0x3F00) {
                addr &= 0x1F;
                //if ((addr & 0x03) == 0x00)
                //    addr = 0x00;
                return (byte)(Palette[addr] & (((PPU_Reg[1] & 0x01) > 0) ? 0x30 : 0x3F));
            }
            else if (addr >= 0x2000) {
                addr &= 0x0FFF;
                if ((Mapper1 & 0x01) > 0) {
                    // Vertical
                    if (addr <= 0x03FF)
                        return TableName[0][addr & 0x03FF];
                    else if ( addr <= 0x07FF)
                        return TableName[1][addr & 0x03FF];
                    else if (addr <= 0x0BFF)
                        return TableName[0][addr & 0x03FF];
                    else
                        return TableName[1][addr & 0x03FF];
                }
                else {
                    // Horizontal
                    if (addr <= 0x03FF)
                        return TableName[0][addr & 0x03FF];
                    else if (addr <= 0x07FF)
                        return TableName[0][addr & 0x03FF];
                    else if (addr <= 0x0BFF)
                        return TableName[1][addr & 0x03FF];
                    else
                        return TableName[1][addr & 0x03FF];
                }
            }
            else
                return CHRMEM[addr + chr_offset];
        }

        private static byte address_latch = 0x00;

        private static byte delete = 0x00;
        private static byte fine_x = 0;

        public static void WriteReg(ushort addr, byte data) {
            addr &= 0x1F;
            if (addr == 0x14) {
                for (int i = 0; i < 256; i++) {
                    if ((i + PPU_Reg[3]) >= OAMDATA.Length)
                        break;
                    OAMDATA[i + PPU_Reg[3]] = NES.CPU.Read((ushort)(data * 256 + i));
                }
                for(int i = 0; i < 64; i++) {
                    OAM[i].y = OAMDATA[i * 4 + 0];
                    OAM[i].id= OAMDATA[i * 4 + 1];
                    OAM[i].attribute = OAMDATA[i * 4 + 2];
                    OAM[i].x = OAMDATA[i * 4 + 3];
                }
                return;
            }
            addr &= 0x07;
            PPU_Reg[addr] = data;
            switch (addr) {
                case 0x00: // Control
                    tram_addr.nametable_x = (byte)(PPU_Reg[0] & 0x01);
                    tram_addr.nametable_y = (byte)((PPU_Reg[0] >> 1) & 0x01);
                    break;
                case 0x01: // Mask
                    break;
                case 0x02: // Status
                    break;
                case 0x03: // OAM Address
                    break;
                case 0x04: // OAM Data
                    break;
                case 0x0005: // Scroll
                    if (address_latch == 0) {
                        fine_x = (byte)(data & 0x07);
                        tram_addr.coarse_x = (byte)((data >> 3) & 0x1F);
                        address_latch = 1;
                    }
                    else {
                        tram_addr.fine_y = (byte)(data & 0x07);
                        tram_addr.coarse_y = (byte)((data >> 3) & 0x1F);
                        address_latch = 0;
                        if (delete != tram_addr.coarse_y) {
                            delete = tram_addr.coarse_y;
                            break;
                        }
                    }
                    break;
                case 0x0006: // PPU Address
                    if (address_latch == 0) {
                        tram_addr_reg = (ushort)(((data & 0x3F) << 8) | (tram_addr_reg & 0x00FF));
                        address_latch = 1;
                    }
                    else {
                        tram_addr_reg = (ushort)((tram_addr_reg & 0xFF00) | data);
                        vram_addr = tram_addr;
                        address_latch = 0;
                    }
                    break;
                case 0x07: // PPU Data
                    PPU_Write(vram_addr_reg, data);
                    vram_addr_reg += (ushort)(((PPU_Reg[0] & 0x04) > 0) ? 32 : 1);
                    break;
            }
        }

        private static byte ppu_data_old = 0;
        public static byte ReadReg(ushort addr) {
            addr &= 0x1F;
            if (addr == 0x14)
                return 0;
            addr &= 0x07;
            byte res = PPU_Reg[addr];
            switch (addr) {
                case 0x00: // Control
                    break;
                case 0x01: // Mask
                    break;
                case 0x02: // Status
                    res = (byte)((res & 0xE0) | (ppu_data_old & 0x1F));
                    //PPU_Reg[2] &= 0x7F;
                    address_latch = 0;
                    break;
                case 0x03: // OAM Address
                    break;
                case 0x04: // OAM Data
                    break;
                case 0x05: // Scroll
                    break;
                case 0x06: // PPU Address
                    break;
                case 0x07: // PPU Data
                    PPU_Reg[7] = ppu_data_old;
                    ppu_data_old = PPU_Read(vram_addr_reg);
                    if (vram_addr_reg >= 0x3F00)
                        PPU_Reg[7] = ppu_data_old;
                    vram_addr_reg += (ushort)(((PPU_Reg[0] & 0x04) > 0) ? 32 : 1);
                    return PPU_Reg[7];
            }
            return res;
        }

        public static Bitmap CreateImage() {
            byte[] rgbValues = image_data;
            Bitmap bitmap = new Bitmap(256, 220);
            Rectangle rec = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
            System.Drawing.Imaging.BitmapData bmpData = bitmap.LockBits(rec, System.Drawing.Imaging.ImageLockMode.ReadWrite, bitmap.PixelFormat);
            IntPtr ptr = bmpData.Scan0;
            Int32 bytes = bmpData.Stride * bitmap.Height;
            System.Runtime.InteropServices.Marshal.Copy(rgbValues, 0, ptr, bytes);
            bitmap.UnlockBits(bmpData);
            return bitmap;
        }

        private static void setPixel(int x, int y, Color color) {
            if (x >= 256)
                return;
            y -= 10;
            if (y >= 230)
                return;
            image_data[(y * 256 + x) * 4 + 3] = 255;
            image_data[(y * 256 + x) * 4 + 2] = color.R;
            image_data[(y * 256 + x) * 4 + 1] = color.G;
            image_data[(y * 256 + x) * 4 + 0] = color.B;
        }

        public static void Reset() {
            scanline = 0;
            vram_addr_reg = 0;
            tram_addr_reg = 0;
            chr_offset = 0;
            TableName[0] = new byte[1024];
            TableName[1] = new byte[1024];
            for (int i = 0; i < PPU_Reg.Length; i++)
                PPU_Reg[i] = 0;
        }

        private static Color GetColourFromPaletteRam(byte palette, byte pixel) {
            return palScreen[PPU_Read((ushort)(0x3F00 + (palette << 2) + pixel))];
        }

        private static byte[] shifter1 = { 0, 1, 2, 3, 4, 5, 6, 7 };
        private static byte[] shifter2 = { 7, 6, 5, 4, 3, 2, 1, 0 };

        public static void ScanLine(int line) {
            Color[] color_value = new Color[256];

            if ((PPU_Reg[1] & 0x08) > 0) {
                ushort bg_pattern_addr = (ushort)((PPU_Reg[0] & 0x10) << 8);
                byte coarse_y = (byte)((line + tram_addr.fine_y) / 8 + tram_addr.coarse_y);
                ushort nametable_y_offset = (ushort)(((coarse_y / 30) ^ tram_addr.nametable_y) << 11);
                coarse_y %= 30;

                ushort temp = (ushort)((line + tram_addr.fine_y) % 8 + bg_pattern_addr);
                ushort temp2 = (ushort)(nametable_y_offset + 0x2000 + coarse_y * 32);
                ushort temp3 = (ushort)((coarse_y / 4) * 8 + 0x23C0 + nametable_y_offset);

                ushort index = (ushort)-fine_x;

                for (byte n = 0; n < 33; n++) {
                    byte coarse_x = (byte)(n + tram_addr.coarse_x);
                    ushort nametable_x_offset = (ushort)(((coarse_x / 32) ^ tram_addr.nametable_x) << 10);
                    coarse_x %= 32;

                    ushort bg_tile_addr = (ushort)(PPU_Read((ushort)(coarse_x + temp2 + nametable_x_offset)) * 16 + temp);
                    byte tile_lo = PPU_Read(bg_tile_addr);
                    byte tile_hi = PPU_Read((ushort)(bg_tile_addr + 8));

                    byte attribute_index = (byte)(((coarse_y << 1) & 0x04) + (coarse_x & 0x02));
                    byte palette = (byte)((PPU_Read((ushort)((coarse_x / 4) + temp3 + nametable_x_offset)) >> attribute_index) & 0x03);

                    for (byte i = 0; i < 8; i++) {
                        if (index < 256) {
                            byte value = (byte)((tile_lo >> (7 - i)) & 0x01);
                            value |= (byte)(((tile_hi >> (7 - i)) & 0x01) << 1);
                            color_value[index] = GetColourFromPaletteRam(palette, value);
                        }
                        index++;
                    }
                }
            }

            bool bSpriteZeroBeingRendered = false;
            if ((PPU_Reg[1] & 0x10) > 0) {
                ushort sp_pattern_addr = (ushort)((PPU_Reg[0] & 0x08) << 9);
                byte spirte_size = (byte)((PPU_Reg[0] & 0x20) > 0 ? 16 : 8);
                byte spirte_count = 0;
                Color color_null = palScreen[Palette[0]];
                PPU_Reg[2] &= 0xDF;
                for (byte n = 0; n < 64; n++) {
                    ushort diff = (ushort)(line - (OAM[n].y + 1));
                    if (diff < spirte_size) {
                        if (spirte_count < 8) {
                            ushort sprite_addr;
                            if (spirte_size == 8) {
                                if ((OAM[n].attribute & 0x80) > 0)
                                    diff = (ushort)(7 - diff);
                                sprite_addr = (ushort)(OAM[n].id * 16 + sp_pattern_addr + diff);
                            }
                            else {
                                if ((OAM[n].attribute & 0x80) > 0)
                                    diff = (ushort)(15 - diff);
                                sprite_addr = (ushort)(((OAM[n].id & 0xFE) + (OAM[n].id & 0x01) * 256 + (diff / 8)) * 16 + (diff % 8));
                            }
                            byte tile_lo = PPU_Read(sprite_addr);
                            byte tile_hi = PPU_Read((ushort)(sprite_addr + 8));

                            if ((tile_lo | tile_hi) > 0) {
                                byte palette = (byte)((OAM[n].attribute & 0x03) + 4);
                                byte[] shifter = (OAM[n].attribute & 0x40) == 0 ? shifter2 : shifter1;

                                byte N = 8;
                                if ((OAM[n].x + 7) > 255)
                                    N = (byte)(256 - OAM[n].x);

                                if ((OAM[n].attribute & 0x20) > 0) {
                                    for (byte i = 0; i < N; i++) {
                                        if (color_value[OAM[n].x + i] == color_null) {
                                            byte value = (byte)((tile_lo >> shifter[i]) & 0x01);
                                            value |= (byte)(((tile_hi >> shifter[i]) & 0x01) << 1);
                                            color_value[OAM[n].x + i] = GetColourFromPaletteRam(palette, value);
                                        }
                                    }
                                }
                                else for (byte i = 0; i < N; i++) {
                                        byte value = (byte)((tile_lo >> shifter[i]) & 0x01);
                                        value |= (byte)(((tile_hi >> shifter[i]) & 0x01) << 1);
                                        if (value > 0)
                                            color_value[OAM[n].x + i] = GetColourFromPaletteRam(palette, value);
                                    }
                                if ((n == 0) && (diff < 8))
                                    bSpriteZeroBeingRendered = true;
                            }
                        }
                        else {
                            PPU_Reg[2] |= 0x20;
                            break;
                        }
                        spirte_count++;
                    }
                }
            }

            if (bSpriteZeroBeingRendered && ((PPU_Reg[1] & 0x18) > 0))
                PPU_Reg[2] |= 0x40;

            for (int i = 0; i < 256; i++)
                setPixel(i, line, color_value[i]);
        }

        public static bool Clock() {
            bool res = false;

            if(scanline >= 0 && scanline <= 240)
                ScanLine(scanline);

            if (++scanline >= 261) {
                PPU_Reg[2] &= 0x1F;
                scanline = -1;
            }
            else if (scanline == 241) {
                PPU_Reg[2] |= 0x80;
                if ((PPU_Reg[0] & 0x80) > 0)
                    NES.CPU.NMIF = 1;
                res = true;
            }

            return res;
        }
    }
}
