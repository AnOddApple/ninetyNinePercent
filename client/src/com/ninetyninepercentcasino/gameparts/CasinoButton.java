package com.ninetyninepercentcasino.gameparts;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Models an interactive game button for normal player actions in a casino game
 * @author Grant Liang
 */
public class CasinoButton extends Actor {
    protected Sprite buttonSprite;
    protected boolean isAvailable;

    public CasinoButton(){
        isAvailable = false;
        setTouchable(Touchable.enabled);
        addListener(new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                if(isAvailable) buttonSprite.setColor(65, 65, 65, 0.8f);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor){
                if(isAvailable) buttonSprite.setColor(1, 1,1 ,1f);
            }
        });
    }
    public void draw(Batch batch, float parentAlpha){
        buttonSprite.draw(batch);
    }
    public void disable(){
        isAvailable = false;
        buttonSprite.setColor(65, 65, 65, 0.8f);
    }
    public void enable(){
        isAvailable = true;
        buttonSprite.setColor(1, 1,1 ,1f);
    }
}
