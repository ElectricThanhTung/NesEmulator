package com.electricthanhtung.nes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.electricthanhtung.nes.nes.CPU;
import com.electricthanhtung.nes.nes.PPU;

import org.json.JSONArray;
import org.json.JSONException;

public class NesPlayActivity extends AppCompatActivity {
    private com.electricthanhtung.nes.view.NesScreen nesImageView;
    private boolean timer2IsRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_nes_play);

        nesImageView = findViewById(R.id.nesGameImageView);

        Intent intent = getIntent();
        int gameId = intent.getIntExtra("GAME_ID", -1);
        if(gameId >= 0) {
            loadGame(readResuorceFile(gameId));
            timer.start();
        }


    }

    private byte[] readResuorceFile(int id) {
        InputStream is = getResources().openRawResource(id);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String s;
        try {
            while((s = br.readLine()) != null)
                sb.append(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONArray jsonData = new JSONArray(sb.toString());
            byte[] rawData = new byte[jsonData.length()];
            for(int i = 0; i < jsonData.length(); i++)
                rawData[i] = (byte)jsonData.getInt(i);
            return rawData;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    CountDownTimer timer = new CountDownTimer(15, 15) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            this.start();
            Clock();
        }
    };

    private void loadGame(byte[] rom) {
        byte[] data = new byte[16];
        int read_count = 0;
        System.arraycopy(rom, read_count, data, 0, data.length);
        read_count += data.length;
        String name = "";
        for (int i = 0; i < 4; i++)
            name += (char)data[i];
        byte prg_rom_chunks = data[4];
        byte chr_rom_chunks = data[5];
        byte mapper1 = data[6];
        byte mapper2 = data[7];
        byte prg_ram_size = data[8];
        byte tv_system1 = data[9];
        byte tv_system2 = data[10];

        CPU.PRGMEM = new byte[prg_rom_chunks * 16384];
        System.arraycopy(rom, read_count, CPU.PRGMEM, 0, CPU.PRGMEM.length);
        read_count += CPU.PRGMEM.length;
        if (chr_rom_chunks == 0)
            chr_rom_chunks = 1;
        if(read_count < rom.length) {
            PPU.CHRMEM = new byte[chr_rom_chunks * 8192];
            System.arraycopy(rom, read_count, PPU.CHRMEM, 0, rom.length - read_count);
            read_count += PPU.CHRMEM.length;
        }

        PPU.Mapper1 = mapper1;

        CPU.Reset();
        PPU.Reset();
    }

    private boolean Clock() {
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

        CPU.Clock(113);
        for (int i = 10; i < 230; i++) {
            PPU.ScanLine(i);
            CPU.Clock(113);
        }

        PPU.PPU_Reg[2] |= 0x80;
        if ((PPU.PPU_Reg[0] & 0x80) != 0)
            CPU.NMIF = 1;

        int h = nesImageView.getHeight();
        if(h > 0) {
            int w = h * 256 / 220;
            //nesImageView.setLayoutParams(new ViewGroup.LayoutParams(w, h));
            nesImageView.setImageBitmap(PPU.CreateImage());
        }

        for (int i = 0; i < 20; i++)
            CPU.Clock(113);

        PPU.PPU_Reg[2] &= 0x1F;

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
            return keyDown(event.getKeyCode());
        else if(event.getAction() == KeyEvent.ACTION_UP)
            return keyUp(event.getKeyCode());
        return super.dispatchKeyEvent(event);
    }

    private boolean keyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            timer.cancel();
            timer2.cancel();
            finish();
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) CPU.Controller[0] |= 0x01;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) CPU.Controller[0] |= 0x02;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) CPU.Controller[0] |= 0x04;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) CPU.Controller[0] |= 0x08;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) CPU.Controller[0] |= 0x10;
        if (keyCode == KeyEvent.KEYCODE_1) CPU.Controller[0] |= 0x20;
        //if (keyCode == KeyEvent.KEYCODE_O) CPU.Controller[0] |= 0x40;
        if (keyCode == KeyEvent.KEYCODE_2) CPU.Controller[0] |= 0xC0;

        if (keyCode == KeyEvent.KEYCODE_D) CPU.Controller[1] |= 0x01;
        if (keyCode == KeyEvent.KEYCODE_A) CPU.Controller[1] |= 0x02;
        if (keyCode == KeyEvent.KEYCODE_S) CPU.Controller[1] |= 0x04;
        if (keyCode == KeyEvent.KEYCODE_W) CPU.Controller[1] |= 0x08;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) CPU.Controller[1] |= 0x10;
        if (keyCode == KeyEvent.KEYCODE_I) CPU.Controller[1] |= 0x20;
        //if (keyCode == KeyEvent.KEYCODE_O) CPU.Controller[1] |= 0x40;
        if (keyCode == KeyEvent.KEYCODE_P) CPU.Controller[1] |= 0xC0;

        if ((keyCode == KeyEvent.KEYCODE_O) && !timer2IsRunning) {
            CPU.Controller[0] |= 0x40;
            CPU.Controller[1] |= 0x40;
            timer2IsRunning = true;
            timer2.start();
        }
        return true;
    }

    private boolean keyUp(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) CPU.Controller[0] &= 0xFE;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) CPU.Controller[0] &= 0xFD;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) CPU.Controller[0] &= 0xFB;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) CPU.Controller[0] &= 0xF7;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) CPU.Controller[0] &= 0xEF;
        if (keyCode == KeyEvent.KEYCODE_1) CPU.Controller[0] &= 0xDF;
        //if (keyCode == KeyEvent.KEYCODE_O) CPU.Controller[0] &= 0xBF;
        if (keyCode == KeyEvent.KEYCODE_2) CPU.Controller[0] &= 0x3F;

        if (keyCode == KeyEvent.KEYCODE_D) CPU.Controller[1] &= 0xFE;
        if (keyCode == KeyEvent.KEYCODE_A) CPU.Controller[1] &= 0xFD;
        if (keyCode == KeyEvent.KEYCODE_S) CPU.Controller[1] &= 0xFB;
        if (keyCode == KeyEvent.KEYCODE_W) CPU.Controller[1] &= 0xF7;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) CPU.Controller[1] &= 0xEF;
        if (keyCode == KeyEvent.KEYCODE_I) CPU.Controller[1] &= 0xDF;
        //if (keyCode == KeyEvent.KEYCODE_O) CPU.Controller[1] &= 0xBF;
        if (keyCode == KeyEvent.KEYCODE_P) CPU.Controller[1] &= 0x3F;

        if ((keyCode == KeyEvent.KEYCODE_O) && timer2IsRunning) {
            timer2.cancel();
            timer2IsRunning = false;
            CPU.Controller[0] &= 0xBF;
            CPU.Controller[1] &= 0xBF;
        }
        return true;
    }

    private CountDownTimer timer2  = new CountDownTimer(50, 50) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            this.start();
            CPU.Controller[0] ^= 0x40;
            CPU.Controller[1] ^= 0x40;
        }
    };
}