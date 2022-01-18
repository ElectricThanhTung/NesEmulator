package com.electricthanhtung.nes.nes;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.ByteBuffer;

public class PPU {
    public static byte Mapper1 = 0;
    public static int chr_offset = 0;
    public static byte[] CHRMEM = new byte[8192];
    public static byte[] PPU_Reg = new byte[8];
    private static byte[][] TableName = new byte[2][];
    private static byte[] Palette = new byte[32];

    private static short scanline = 0;
    private static byte[] OAMDATA = new byte[64 * 4];
    private static byte[] image_data = new byte[256 * 220 * 4];

    private static Bitmap nesScreenBitmap = Bitmap.createBitmap(256, 220, Bitmap.Config.ARGB_8888);

    private static class OAMStruct {
        public int y;
        public int id;
        public int attribute;
        public int x;
    };

    private static OAMStruct[] OAM;

    private static class vram_addr_struct {
        public int coarse_x;
        public int coarse_y;
        public int nametable_x;
        public int nametable_y;
        public int fine_y;
        public int unused;
    };

    private static vram_addr_struct vram_addr = new vram_addr_struct();
    private static vram_addr_struct tram_addr = new vram_addr_struct();

    static int max = 0;
    private static void setVramAddrReg(int value) {
        vram_addr.coarse_x = value & 0x1F;
        vram_addr.coarse_y = (value >> 5) & 0x1F;
        vram_addr.nametable_x = (value >> 10) & 0x01;
        vram_addr.nametable_y = (value >> 11) & 0x01;
        vram_addr.fine_y = (value >> 12) & 0x07;
        vram_addr.unused = (value >> 15);
    }

    private static int getVramAddrReg() {
        return (vram_addr.unused << 15) | (vram_addr.fine_y << 12) | (vram_addr.nametable_y << 11) | (vram_addr.nametable_x << 10) | (vram_addr.coarse_y << 5) | vram_addr.coarse_x;
    }

    private static void setTramAddrReg(int value) {
        tram_addr.coarse_x = value & 0x1F;
        tram_addr.coarse_y = (value >> 5) & 0x1F;
        tram_addr.nametable_x = (value >> 10) & 0x01;
        tram_addr.nametable_y = (value >> 11) & 0x01;
        tram_addr.fine_y = (value >> 12) & 0x07;
        tram_addr.unused = (value >> 15);
    }

    private static int getTramAddrReg() {
        return (tram_addr.unused << 15) | (tram_addr.fine_y << 12) | (tram_addr.nametable_y << 11) | (tram_addr.nametable_x << 10) | (tram_addr.coarse_y << 5) | tram_addr.coarse_x;
    }

    private static final int[] palScreen = {
            Color.rgb(84, 84, 84),
            Color.rgb(0, 30, 116),
            Color.rgb(8, 16, 144),
            Color.rgb(48, 0, 136),
            Color.rgb(68, 0, 100),
            Color.rgb(92, 0, 48),
            Color.rgb(84, 4, 0),
            Color.rgb(60, 24, 0),
            Color.rgb(32, 42, 0),
            Color.rgb(8, 58, 0),
            Color.rgb(0, 64, 0),
            Color.rgb(0, 60, 0),
            Color.rgb(0, 50, 60),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),

            Color.rgb(152, 150, 152),
            Color.rgb(8, 76, 196),
            Color.rgb(48, 50, 236),
            Color.rgb(92, 30, 228),
            Color.rgb(136, 20, 176),
            Color.rgb(160, 20, 100),
            Color.rgb(152, 34, 32),
            Color.rgb(120, 60, 0),
            Color.rgb(84, 90, 0),
            Color.rgb(40, 114, 0),
            Color.rgb(8, 124, 0),
            Color.rgb(0, 118, 40),
            Color.rgb(0, 102, 120),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),

            Color.rgb(236, 238, 236),
            Color.rgb(76, 154, 236),
            Color.rgb(120, 124, 236),
            Color.rgb(176, 98, 236),
            Color.rgb(228, 84, 236),
            Color.rgb(236, 88, 180),
            Color.rgb(236, 106, 100),
            Color.rgb(212, 136, 32),
            Color.rgb(160, 170, 0),
            Color.rgb(116, 196, 0),
            Color.rgb(76, 208, 32),
            Color.rgb(56, 204, 108),
            Color.rgb(56, 180, 204),
            Color.rgb(60, 60, 60),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),

            Color.rgb(236, 238, 236),
            Color.rgb(168, 204, 236),
            Color.rgb(188, 188, 236),
            Color.rgb(212, 178, 236),
            Color.rgb(236, 174, 236),
            Color.rgb(236, 174, 212),
            Color.rgb(236, 180, 176),
            Color.rgb(228, 196, 144),
            Color.rgb(204, 210, 120),
            Color.rgb(180, 222, 120),
            Color.rgb(168, 226, 144),
            Color.rgb(152, 226, 180),
            Color.rgb(160, 214, 228),
            Color.rgb(160, 162, 160),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
    };

    public static void PPU_Write(int addr, int data) {
        if (addr >= 0x3F00) {
            addr &= 0x1F;
            //if (addr == 0x10) addr = 0x00;
            //else if (addr == 0x14) addr = 0x04;
            //else if (addr == 0x18) addr = 0x08;
            //else if (addr == 0x1C) addr = 0x0C;
            //Palette[addr] = data;
            if (addr == 0x10 || addr == 0x00) {
                Palette[0] = Palette[0 + 16] = (byte)data;
                Palette[4] = Palette[4 + 16] = (byte)data;
                Palette[8] = Palette[8 + 16] = (byte)data;
                Palette[12] = Palette[12 + 16] = (byte)data;
            }
            else if ((addr & 0x03) != 0x00)
                Palette[addr] = (byte)data;
        }
        else if (addr >= 0x2000) {
            addr &= 0x0FFF;
            if ((Mapper1 & 0x01) != 0) {
                // Vertical
                if (addr <= 0x03FF)
                    TableName[0][addr & 0x03FF] = (byte)data;
                else if (addr <= 0x07FF)
                    TableName[1][addr & 0x03FF] = (byte)data;
                else if (addr <= 0x0BFF)
                    TableName[0][addr & 0x03FF] = (byte)data;
                else
                    TableName[1][addr & 0x03FF] = (byte)data;
            }
            else {
                // Horizontal
                if (addr <= 0x03FF)
                    TableName[0][addr & 0x03FF] = (byte)data;
                else if (addr <= 0x07FF)
                    TableName[0][addr & 0x03FF] = (byte)data;
                else if (addr <= 0x0BFF)
                    TableName[1][addr & 0x03FF] = (byte)data;
                else
                    TableName[1][addr & 0x03FF] = (byte)data;
            }
        }
        else
            CHRMEM[addr + chr_offset] = (byte)data;
    }

    public static int PPU_Read(int addr) {
        if (addr >= 0x3F00) {
            addr &= 0x1F;
            //if ((addr & 0x03) == 0x00)
            //    addr = 0x00;
            return (int)(Palette[addr] & (((PPU_Reg[1] & 0x01) != 0) ? 0x30 : 0x3F)) & 0xFF;
        }
        else if (addr >= 0x2000) {
            addr &= 0x0FFF;
            if ((Mapper1 & 0x01) != 0) {
                // Vertical
                if (addr <= 0x03FF)
                    return (int)TableName[0][addr & 0x03FF] & 0xFF;
                else if ( addr <= 0x07FF)
                    return (int)TableName[1][addr & 0x03FF] & 0xFF;
                else if (addr <= 0x0BFF)
                    return (int)TableName[0][addr & 0x03FF] & 0xFF;
                else
                    return (int)TableName[1][addr & 0x03FF] & 0xFF;
            }
            else {
                // Horizontal
                if (addr <= 0x03FF)
                    return (int)TableName[0][addr & 0x03FF] & 0xFF;
                else if (addr <= 0x07FF)
                    return (int)TableName[0][addr & 0x03FF] & 0xFF;
                else if (addr <= 0x0BFF)
                    return (int)TableName[1][addr & 0x03FF] & 0xFF;
                else
                    return (int)TableName[1][addr & 0x03FF] & 0xFF;
            }
        }
        else
            return (int)CHRMEM[addr + chr_offset] & 0xFF;
    }

    private static byte address_latch = 0x00;

    private static int delete = 0x00;
    private static int fine_x = 0;

    public static void WriteReg(int addr, int data) {
        addr &= 0x1F;
        if (addr == 0x14) {
            for (int i = 0; i < 256; i++) {
                if ((i + PPU_Reg[3]) >= OAMDATA.length)
                    break;
                OAMDATA[i + PPU_Reg[3]] = (byte)CPU.Read(data * 256 + i);
            }
            for(int i = 0; i < 64; i++) {
                OAM[i].y = (int)OAMDATA[i * 4 + 0] & 0xFF;
                OAM[i].id= (int)OAMDATA[i * 4 + 1] & 0xFF;
                OAM[i].attribute = (int)OAMDATA[i * 4 + 2] & 0xFF;
                OAM[i].x = (int)OAMDATA[i * 4 + 3] & 0xFF;
            }
            return;
        }
        addr &= 0x07;
        PPU_Reg[addr] = (byte)data;
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
                    fine_x = data & 0x07;
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
                    setTramAddrReg(((data & 0x3F) << 8) | (getTramAddrReg() & 0x00FF));
                    address_latch = 1;
                }
                else {
                    setTramAddrReg((getTramAddrReg() & 0xFF00) | data);
                    setVramAddrReg(getTramAddrReg());
                    address_latch = 0;
                }
                break;
            case 0x07: // PPU Data
                int vram = getVramAddrReg();
                PPU_Write(vram, data);
                setVramAddrReg(vram + (((PPU_Reg[0] & 0x04) != 0) ? 32 : 1));
                break;
        }
    }

    private static byte ppu_data_old = 0;
    public static int ReadReg(int addr) {
        addr &= 0x1F;
        if (addr == 0x14)
            return 0;
        addr &= 0x07;
        int res = (int)PPU_Reg[addr] & 0xFF;
        switch (addr) {
            case 0x00: // Control
                break;
            case 0x01: // Mask
                break;
            case 0x02: // Status
                res = (res & 0xE0) | (ppu_data_old & 0x1F);
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
                int vram = getVramAddrReg();
                PPU_Reg[7] = ppu_data_old;
                ppu_data_old = (byte)PPU_Read(vram);
                if (vram >= 0x3F00)
                    PPU_Reg[7] = ppu_data_old;
                setVramAddrReg(vram + (((PPU_Reg[0] & 0x04) != 0) ? 32 : 1));
                return (int)PPU_Reg[7] & 0xFF;
        }
        return res;
    }

    public static Bitmap CreateImage() {
        ByteBuffer buffer = ByteBuffer.wrap(image_data);
        nesScreenBitmap.copyPixelsFromBuffer(buffer);

        return nesScreenBitmap;
    }

    private static void setPixel(int x, int y, int color) {
        if (x >= 256)
            return;
        y -= 10;
        if (y >= 230)
            return;
        int temp = (y * 256 + x) * 4;
        image_data[temp + 3] = (byte)255;
        image_data[temp + 2] = (byte)Color.blue(color);
        image_data[temp + 1] = (byte)Color.green(color);
        image_data[temp + 0] = (byte)Color.red(color);
    }

    public static void Reset() {
        if(OAM == null) {
            OAM = new OAMStruct[64];
            for(int i = 0; i < OAM.length; i++)
                OAM[i] = new OAMStruct();
        }
        scanline = 0;
        setVramAddrReg(0);
        setTramAddrReg(0);
        chr_offset = 0;
        TableName[0] = new byte[1024];
        TableName[1] = new byte[1024];
        for (int i = 0; i < PPU_Reg.length; i++)
            PPU_Reg[i] = 0;
    }

    private static int GetColourFromPaletteRam(int palette, int pixel) {
        return palScreen[PPU_Read((0x3F00 + (palette << 2) + pixel) & 0xFFFF)];
    }

    private static final int[] shifter1 = { 0, 1, 2, 3, 4, 5, 6, 7 };
    private static final int[] shifter2 = { 7, 6, 5, 4, 3, 2, 1, 0 };

    public static void ScanLine(int line) {
        int[] color_value = new int[256];

        if ((PPU_Reg[1] & 0x08) != 0) {
            int bg_pattern_addr = (PPU_Reg[0] & 0x10) << 8;
            int coarse_y = ((line + tram_addr.fine_y) / 8 + tram_addr.coarse_y) & 0xFF;
            int nametable_y_offset = (((coarse_y / 30) ^ tram_addr.nametable_y) << 11) & 0xFFFF;
            coarse_y %= 30;

            int temp = ((line + tram_addr.fine_y) % 8 + bg_pattern_addr) & 0xFFFF;
            int temp2 = (nametable_y_offset + 0x2000 + coarse_y * 32) & 0xFFFF;
            int temp3 = ((coarse_y / 4) * 8 + 0x23C0 + nametable_y_offset) & 0xFFFF;

            int index = (-fine_x) & 0xFFFF;

            for (int n = 0; n < 33; n++) {
                int coarse_x = (n + tram_addr.coarse_x) & 0xFF;
                int nametable_x_offset = ((coarse_x / 32) ^ tram_addr.nametable_x) << 10;
                coarse_x %= 32;

                int bg_tile_addr = (PPU_Read((coarse_x + temp2 + nametable_x_offset) & 0xFFFF) * 16 + temp) & 0xFFFF;
                int tile_lo = PPU_Read(bg_tile_addr);
                int tile_hi = PPU_Read((bg_tile_addr + 8) & 0xFFFF);

                int attribute_index = (((coarse_y << 1) & 0x04) + (coarse_x & 0x02)) & 0xFF;
                int palette = ((PPU_Read(((coarse_x / 4) + temp3 + nametable_x_offset) & 0xFFFF) >> attribute_index) & 0x03) & 0xFF;

                for (int i = 0; i < 8; i++) {
                    if (index < 256) {
                        int value = (tile_lo >> (7 - i)) & 0x01;
                        value |= ((tile_hi >> (7 - i)) & 0x01) << 1;
                        color_value[index] = GetColourFromPaletteRam(palette, value);
                    }
                    index = (index + 1) & 0xFFFF;
                }
            }
        }

        boolean bSpriteZeroBeingRendered = false;
        if ((PPU_Reg[1] & 0x10) != 0) {
            int sp_pattern_addr = (PPU_Reg[0] & 0x08) << 9;
            int spirte_size = ((PPU_Reg[0] & 0x20) != 0 ? 16 : 8);
            int spirte_count = 0;
            int color_null = palScreen[Palette[0]];
            PPU_Reg[2] &= 0xDF;
            for (int n = 0; n < 64; n++) {
                int diff = (line - (OAM[n].y + 1)) & 0xFFFF;
                if (diff < spirte_size) {
                    if (spirte_count < 8) {
                        int sprite_addr;
                        if (spirte_size == 8) {
                            if ((OAM[n].attribute & 0x80) != 0)
                                diff = (7 - diff) & 0xFFFF;
                            sprite_addr = (OAM[n].id * 16 + sp_pattern_addr + diff) & 0xFFFF;
                        }
                        else {
                            if ((OAM[n].attribute & 0x80) != 0)
                                diff = (15 - diff) & 0xFFFF;
                            sprite_addr = (((OAM[n].id & 0xFE) + (OAM[n].id & 0x01) * 256 + (diff / 8)) * 16 + (diff % 8)) & 0xFFFF;
                        }
                        int tile_lo = PPU_Read(sprite_addr);
                        int tile_hi = PPU_Read(sprite_addr + 8);

                        if ((tile_lo | tile_hi) != 0) {
                            int palette = (OAM[n].attribute & 0x03) + 4;
                            int[] shifter = (OAM[n].attribute & 0x40) == 0 ? shifter2 : shifter1;

                            int N = 8;
                            if ((OAM[n].x + 7) > 255)
                                N = (256 - OAM[n].x) & 0xFF;

                            if ((OAM[n].attribute & 0x20) != 0) {
                                for (int i = 0; i < N; i++) {
                                    if (color_value[OAM[n].x + i] == color_null) {
                                        int value = (tile_lo >> shifter[i]) & 0x01;
                                        value |= ((tile_hi >> shifter[i]) & 0x01) << 1;
                                        color_value[OAM[n].x + i] = GetColourFromPaletteRam(palette, value);
                                    }
                                }
                            }
                            else for (int i = 0; i < N; i++) {
                                int value = (tile_lo >> shifter[i]) & 0x01;
                                value |= ((tile_hi >> shifter[i]) & 0x01) << 1;
                                if (value != 0)
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

        if (bSpriteZeroBeingRendered && ((PPU_Reg[1] & 0x18) != 0))
            PPU_Reg[2] |= 0x40;

        for (int i = 0; i < 256; i++)
            setPixel(i, line, color_value[i]);
    }

    public static boolean Clock() {
        boolean res = false;

        if(scanline >= 0 && scanline <= 240)
            ScanLine(scanline);

        if (++scanline >= 261) {
            PPU_Reg[2] &= 0x1F;
            scanline = -1;
        }
        else if (scanline == 241) {
            PPU_Reg[2] |= 0x80;
            if ((PPU_Reg[0] & 0x80) != 0)
                CPU.NMIF = 1;
            res = true;
        }

        return res;
    }
}
