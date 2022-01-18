package com.electricthanhtung.nes;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class GameList extends AppCompatActivity {
    private int itemSelected = 0;
    private LinearLayout[] gameListItem;
    private int[] gameRomID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game_list);

        initGameROMID();

        gameListItem = new LinearLayout[10];
        gameListItem[0] = findViewById(R.id.gameItem0);
        gameListItem[1] = findViewById(R.id.gameItem1);
        gameListItem[2] = findViewById(R.id.gameItem2);
        gameListItem[3] = findViewById(R.id.gameItem3);
        gameListItem[4] = findViewById(R.id.gameItem4);
        gameListItem[5] = findViewById(R.id.gameItem5);
        gameListItem[6] = findViewById(R.id.gameItem6);
        gameListItem[7] = findViewById(R.id.gameItem7);
        gameListItem[8] = findViewById(R.id.gameItem8);
        gameListItem[9] = findViewById(R.id.gameItem9);

        ObjectAnimator animation = ObjectAnimator.ofFloat(gameListItem[0], "translationX", dpToPx(30f));
        animation.setDuration(200);
        animation.start();
    }

    private void initGameROMID() {
        gameRomID = new int[10];
        for(int i = 0; i < gameRomID.length; i++)
            gameRomID[i] = -1;

        gameRomID[0] = R.raw.super_mario_bros;
        gameRomID[1] = R.raw._1943;
        gameRomID[2] = R.raw.adventure_island_classic;
        gameRomID[3] = R.raw.battle_city;
        gameRomID[7] = R.raw.bomber_man;
        gameRomID[9] = R.raw.contra;
    }

    private float dpToPx(float dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
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
        int selectedOld = itemSelected;
        switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if(itemSelected > 0)
                    itemSelected--;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(itemSelected < 9)
                    itemSelected++;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                Intent intent = new Intent(this, NesPlayActivity.class);
                intent.putExtra("GAME_ID", gameRomID[itemSelected]);
                startActivity(intent);
                break;
        }
        if(selectedOld != itemSelected) {
            ObjectAnimator animation1 = ObjectAnimator.ofFloat(gameListItem[selectedOld], "translationX", 0);
            animation1.setDuration(200);
            animation1.start();

            ObjectAnimator animation2 = ObjectAnimator.ofFloat(gameListItem[itemSelected], "translationX", dpToPx(30f));
            animation2.setDuration(200);
            animation2.start();
        }
        return true;
    }

    private boolean keyUp(int keyCode) {
        return true;
    }
}