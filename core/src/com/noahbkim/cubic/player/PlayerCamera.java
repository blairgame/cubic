package com.noahbkim.cubic.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.noahbkim.cubic.Cubic;
import com.noahbkim.cubic.player.OrbitCamera.Defaults;
import com.noahbkim.cubic.utility.Updatable;

/**
 * Specialized camera that maintains an orbiting 3rd person view of the target model.
 * @author Noah Kim
 * @author Arman Siddique
 */
public class PlayerCamera extends OrbitCamera implements Updatable {
	private float altitude;
	private boolean followPlayer;
	
	public PlayerCamera(Player target) {
		this(target, Defaults.fieldOfView, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	public PlayerCamera(Player target, float fieldOfView, int width, int height) {
		super(fieldOfView, width, height);
		player = target;
		altitude = Defaults.startingAltitude;
		followPlayer = true;
	}
	
	@Override
	public void update() {
		if (player != null && followPlayer) {
			/* Get input. */
			input();
			
			/* Move and rotate the camera. */
			Vector3 origin = player.getTranslation();
			Vector3 translation = new Vector3();
			translation.x = (float)(radius * MathUtils.sinDeg(altitude) * MathUtils.cosDeg(player.azimuth)) + origin.x;
			translation.y = (float)(radius * MathUtils.cosDeg(altitude)) + origin.y;
			translation.z = (float)(radius * MathUtils.sinDeg(altitude) * MathUtils.sinDeg(player.azimuth)) + origin.z;
			position.set(translation);
			up.set(Vector3.Y);
			lookAt(origin);
			
			updatePerspectiveCamera();
		} else {
			super.update();
		}
	}
	
	@Override
	public void input() {
		if (followPlayer) {
			float rotY = Gdx.input.getDeltaY() * Cubic.defaults.mouseSensitivity;
			if (altitude + rotY < -1.0f && altitude + rotY > -89.0f)
				altitude += rotY;
		} else {
			super.input();
		}
	}
	
	public boolean isFollowingTarget() {
		return followPlayer;
	}
	
	public void followTarget(boolean follow) {
		followPlayer = follow;
		if (follow) {
			altitude = rotation.getPitch();
		} else {
			rotation = (new Quaternion(Vector3.X, altitude)).mulLeft(new Quaternion(Vector3.Y, player.azimuth)); 
		}
	}
}
