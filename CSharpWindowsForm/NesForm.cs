using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading;
using System.Windows.Forms;
using System.Diagnostics;

namespace NES {
    public partial class NesForm : Form {
        private Thread NES_Thread;
        private double fps = 0;

        public NesForm() {
            InitializeComponent();
        }

        private void loadNESROMToolStripMenuItem_Click(object sender, EventArgs e) {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "NES ROM|*.nes";
            if (openFileDialog.ShowDialog() == DialogResult.OK) {
                if (NES_Thread != null)
                    NES_Thread.Abort();
                FileStream rom = new FileStream(openFileDialog.FileName, FileMode.Open);
                byte[] data = new byte[16];
                rom.Read(data, 0, data.Length);
                string name = "";
                for (int i = 0; i < 4; i++)
                    name += (char)data[i];
                byte prg_rom_chunks = data[4];
                byte chr_rom_chunks = data[5];
                byte mapper1 = data[6];
                byte mapper2 = data[7];
                byte prg_ram_size = data[8];
                byte tv_system1 = data[9];
                byte tv_system2 = data[10];

                NES.CPU.PRGMEM = new byte[prg_rom_chunks * 16384];
                rom.Read(NES.CPU.PRGMEM, 0, NES.CPU.PRGMEM.Length);
                if (chr_rom_chunks == 0)
                    chr_rom_chunks = 1;
                NES.PPU.CHRMEM = new byte[chr_rom_chunks * 8192];
                rom.Read(NES.PPU.CHRMEM, 0, NES.PPU.CHRMEM.Length);

                NES.PPU.Mapper1 = mapper1;

                NES.CPU.Reset();
                NES.PPU.Reset();

                rom.Close();

                if (NES_Thread != null) {
                    NES_Thread.Abort();
                    NES.CPU.Reset();
                    NES.PPU.Reset();
                }
                int FPS = 60;
                NES_Thread = new Thread(delegate () {
                    while (true) {
                        Stopwatch stopwatch = Stopwatch.StartNew();
                        if (!Clock())
                            return;
                        long time;
                        do {
                            time = stopwatch.ElapsedTicks;
                        } while (time < ((10000000 + FPS / 2) / FPS));
                        fps = time;
                        stopwatch.Stop();
                    }
                });
                NES_Thread.IsBackground = true;
                NES_Thread.Priority = ThreadPriority.Highest;
                NES_Thread.Start();
            }
            openFileDialog.Dispose();
        }

        private void createArrayToolStripMenuItem_Click(object sender, EventArgs e) {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "NES ROM|*.nes";
            if (openFileDialog.ShowDialog() == DialogResult.OK) {
                //if (NES_Thread != null)
                //    NES_Thread.Abort();
                Waiting waiting = new Waiting(delegate () {
                    FileStream rom = new FileStream(openFileDialog.FileName, FileMode.Open);
                    byte[] data = new byte[rom.Length];
                    rom.Read(data, 0, data.Length);
                    rom.Close();
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < data.Length; i++) {
                        if ((i > 0) && (i % 32) == 0)
                            str.Append("\n");
                        str.Append("0x");
                        str.Append(ToHex(data[i]));
                        str.Append(", ");
                    }
                    this.BeginInvoke((MethodInvoker)delegate () {
                        Clipboard.SetText(str.ToString());
                    });
                });
                waiting.ShowDialog();
            }
            openFileDialog.Dispose();
        }

        private void Form1_KeyDown(object sender, KeyEventArgs e) {
            if (e.KeyData == Keys.D) NES.CPU.Controller[0] |= 0x01;
            if (e.KeyData == Keys.A) NES.CPU.Controller[0] |= 0x02;
            if (e.KeyData == Keys.S) NES.CPU.Controller[0] |= 0x04;
            if (e.KeyData == Keys.W) NES.CPU.Controller[0] |= 0x08;

            if (e.KeyData == Keys.Enter) NES.CPU.Controller[0] |= 0x10;
            if (e.KeyData == Keys.I) NES.CPU.Controller[0] |= 0x20;
            //if (e.KeyData == Keys.O) NES.CPU.Controller[0] |= 0x40;
            if (e.KeyData == Keys.P) NES.CPU.Controller[0] |= 0xC0;

            if (e.KeyData == Keys.D) NES.CPU.Controller[1] |= 0x01;
            if (e.KeyData == Keys.A) NES.CPU.Controller[1] |= 0x02;
            if (e.KeyData == Keys.S) NES.CPU.Controller[1] |= 0x04;
            if (e.KeyData == Keys.W) NES.CPU.Controller[1] |= 0x08;

            if (e.KeyData == Keys.Enter) NES.CPU.Controller[1] |= 0x10;
            if (e.KeyData == Keys.I) NES.CPU.Controller[1] |= 0x20;
            //if (e.KeyData == Keys.O) NES.CPU.Controller[1] |= 0x40;
            if (e.KeyData == Keys.P) NES.CPU.Controller[1] |= 0xC0;

            if ((e.KeyData == Keys.O) && (timer1.Enabled == false)) {
                NES.CPU.Controller[0] |= 0x40;
                NES.CPU.Controller[1] |= 0x40;
                timer1.Enabled = true;
            }
        }

        private void Form1_KeyUp(object sender, KeyEventArgs e) {
            if (e.KeyData == Keys.D) NES.CPU.Controller[0] &= 0xFE;
            if (e.KeyData == Keys.A) NES.CPU.Controller[0] &= 0xFD;
            if (e.KeyData == Keys.S) NES.CPU.Controller[0] &= 0xFB;
            if (e.KeyData == Keys.W) NES.CPU.Controller[0] &= 0xF7;

            if (e.KeyData == Keys.Enter) NES.CPU.Controller[0] &= 0xEF;
            if (e.KeyData == Keys.I) NES.CPU.Controller[0] &= 0xDF;
            //if (e.KeyData == Keys.O) NES.CPU.Controller[0] &= 0xBF;
            if (e.KeyData == Keys.P) NES.CPU.Controller[0] &= 0x3F;

            if (e.KeyData == Keys.D) NES.CPU.Controller[1] &= 0xFE;
            if (e.KeyData == Keys.A) NES.CPU.Controller[1] &= 0xFD;
            if (e.KeyData == Keys.S) NES.CPU.Controller[1] &= 0xFB;
            if (e.KeyData == Keys.W) NES.CPU.Controller[1] &= 0xF7;

            if (e.KeyData == Keys.Enter) NES.CPU.Controller[1] &= 0xEF;
            if (e.KeyData == Keys.I) NES.CPU.Controller[1] &= 0xDF;
            //if (e.KeyData == Keys.O) NES.CPU.Controller[1] &= 0xBF;
            if (e.KeyData == Keys.P) NES.CPU.Controller[1] &= 0x3F;

            if ((e.KeyData == Keys.O) && (timer1.Enabled == true)) {
                timer1.Enabled = false;
                NES.CPU.Controller[0] &= 0xBF;
                NES.CPU.Controller[1] &= 0xBF;
            }
        }

        private void Form1_FormClosing(object sender, FormClosingEventArgs e) {
            if (NES_Thread != null)
                NES_Thread.Abort();
        }

        private bool Clock() {
            //if (NES.PPU.Clock()) {
            //    if (NES.PPU.Screen != null) {
            //        Bitmap bitmap;
            //        if ((pictureBox1.Width < 1) || (pictureBox1.Height < 1))
            //            bitmap = new Bitmap(256, 240);
            //        else 
            //            bitmap = new Bitmap(pictureBox1.Width, pictureBox1.Height);
            //        Graphics g = Graphics.FromImage(bitmap);

            //        g.InterpolationMode = InterpolationMode.NearestNeighbor;
            //        g.SmoothingMode = SmoothingMode.None;
            //        g.PixelOffsetMode = PixelOffsetMode.None;
            //        g.DrawImage(NES.PPU.Screen, 0, 0, bitmap.Width, bitmap.Height);
            //        g.Dispose();

            //        this.BeginInvoke((MethodInvoker)delegate () {
            //            if (pictureBox1.Image != null)
            //                pictureBox1.Image.Dispose();
            //            pictureBox1.Image = bitmap;

            //        });

            //        Application.DoEvents();
            //    }
            //}
            //if (!NES.CPU.Clock(113))
            //    return false;
            //bool res = true;
            //for (byte i = 0; i < 56; i++) {
            //    if (!NES.CPU.Clock())
            //        return false;
            //    if (!NES.CPU.Clock())
            //        return false;
            //}
            //if (!NES.CPU.Clock())
            //    return false;
            //if (++cycles == 3) {
            //    cycles = 0;
            //    if (!NES.CPU.Clock())
            //        return false;
            //}
            //if (++cycles == 3) {
            //    cycles = 0;
            //    if (!NES.CPU.Clock())
            //        return false;
            //}

            //for (int i = 0; i < 341; i++) {
            //    if (++cycles == 3) {
            //        cycles = 0;
            //        if (!NES.CPU.Clock())
            //            res = false;
            //    }
            //}

            NES.CPU.Clock(113);
            for (int i = 10; i < 230; i++) {
                NES.PPU.ScanLine(i);
                NES.CPU.Clock(113);
            }

            NES.PPU.PPU_Reg[2] |= 0x80;
            if ((NES.PPU.PPU_Reg[0] & 0x80) > 0)
                NES.CPU.NMIF = 1;

            Bitmap Screen = NES.PPU.CreateImage();
            if (Screen != null) {
                Bitmap bitmap;
                if ((pictureBox1.Width < 1) || (pictureBox1.Height < 1))
                    bitmap = new Bitmap(256, 220);
                else
                    bitmap = new Bitmap(pictureBox1.Width, pictureBox1.Height);
                Graphics g = Graphics.FromImage(bitmap);

                g.InterpolationMode = InterpolationMode.NearestNeighbor;
                g.SmoothingMode = SmoothingMode.None;
                g.PixelOffsetMode = PixelOffsetMode.None;
                g.DrawImage(Screen, 0, 0, bitmap.Width, bitmap.Height);
                Font font = new Font("Microsoft Sans Serif", 20.25F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
                g.DrawString(fps + "", font, new SolidBrush(Color.Blue), new Point(5, 5));
                g.Dispose();
                font.Dispose();

                this.BeginInvoke((MethodInvoker)delegate () {
                    if (pictureBox1.Image != null)
                        pictureBox1.Image.Dispose();
                    pictureBox1.Image = bitmap;

                });

                Application.DoEvents();
            }

            for (int i = 0; i < 20; i++)
                NES.CPU.Clock(113);

            NES.PPU.PPU_Reg[2] &= 0x1F;

            return true;
        }

        private string ToHex(int value) {
            string hex = "0123456789ABCDEF";
            return (char)hex[value / 16] + "" + (char)hex[value % 16];
        }

        private void timer1_Tick(object sender, EventArgs e) {
            NES.CPU.Controller[0] ^= 0x40;
            NES.CPU.Controller[1] ^= 0x40;
        }
    }
}
