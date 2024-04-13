package com.ninetyninenercentcasino.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainCasino extends ApplicationAdapter {
	
	SpriteBatch batch;
	Texture allSpades;
	private TextureRegion region;
	private OrthographicCamera camera;
	private float rotationSpeed;
	private float angle = 0f;
	private void handleInput() {
		if(angle >= 360) angle -= 360;
		Vector2 cameraUp = new Vector2(0, 1);
		cameraUp.clamp(1, 1);
		cameraUp.rotateDeg(-angle);
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			camera.zoom += 0.02;
			//If the A Key is pressed, add 0.02 to the Camera's Zoom
		}
		if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
			camera.zoom -= 0.02;
			//If the Q Key is pressed, subtract 0.02 from the Camera's Zoom
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			Vector2 cameraLeft = new Vector2(cameraUp.x, cameraUp.y);
			cameraLeft.rotateDeg(90);
			camera.translate(cameraLeft.x, cameraLeft.y, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			Vector2 cameraRight = new Vector2(cameraUp.x, cameraUp.y);
			cameraRight.rotateDeg(270);
			camera.translate(cameraRight.x, cameraRight.y, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			Vector2 cameraDown = new Vector2(cameraUp.x, cameraUp.y);
			cameraDown.rotateDeg(180);
			camera.translate(cameraDown.x, cameraDown.y, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			camera.translate(cameraUp.x, cameraUp.y, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			camera.rotate(-rotationSpeed, 0, 0, 1);
			angle += rotationSpeed;
			//If the W Key is pressed, rotate the cameraera by -rotationSpeed around the Z-Axis
		}
		if (Gdx.input.isKeyPressed(Input.Keys.E)) {
			camera.rotate(rotationSpeed, 0, 0, 1);
			angle -= rotationSpeed;
			//If the E Key is pressed, rotate the cameraera by rotationSpeed around the Z-Axis
		}
	}
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");
	}

	@Override
	public void render () {
		ScreenUtils.clear(1, 0, 0, 1);
		batch.begin();
		batch.draw(img, 0, 0);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}
}
