package com.noahbkim.cubic.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;
import com.noahbkim.cubic.Cubic;
import com.noahbkim.cubic.physics.PhysicsWorld;
import com.noahbkim.cubic.utility.Models;
import com.noahbkim.cubic.utility.Updatable;

/**
 * General player class based off of a model instance.
 * This class handles movement but is separated from the camera.
 */
public class Player extends ModelInstance implements RenderableProvider, Updatable, Disposable {
	
	/**
	 * Spawn a default player.
	 * @return a new player
	 */
	public static Player spawn(PhysicsWorld world) {
		Vector3 dimensions = new Vector3(1f, 1f, 1f);
		float mass = 2.0f;
		Model model = Models.box(dimensions);
		return new Player(world, model, dimensions, mass);
	}
	
	/** Model. */
	public PhysicsWorld world;
	public Vector3 dimensions;
	public float mass;

	/** Movement. */
	public boolean movementEnabled;
	public boolean rotationEnabled;
	public float azimuth;
	public boolean control;
	public float lastRotation;
	
	public float angularVelocityLimit;
	public float angularAccelerationFactor;
	public float linearVelocityLimit;
	public float jumpForce;
	
	private Vector3 up;
	
	/** Physics. */
	private Vector3 localInertia;
	private PlayerMotionState motionState;
	public btCollisionShape collisionShape;
	public btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	public btRigidBody rigidBody;
	
	/**
	 * Instantiate a new player with a custom model. s
	 * @param model the model of the player.
	 */
	private Player(PhysicsWorld world, Model model, Vector3 dimensions, float mass) {
		/* Create the model. */
		super(model);
		this.world = world;
		this.dimensions = dimensions;
		this.mass = mass;
		
		/* Set up physics. */
		localInertia = new Vector3();
		motionState = new PlayerMotionState(this);
		collisionShape = new btBoxShape(dimensions.scl(0.5f));
		collisionShape.calculateLocalInertia(mass, localInertia);
		constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia);
		rigidBody = new btRigidBody(constructionInfo);
		rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
		rigidBody.activate(true);
		rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		rigidBody.setFriction(0.6f);
		
		/* Constants. */
		angularVelocityLimit = 4.0f;
		angularAccelerationFactor = 0.2f;
		linearVelocityLimit = 500f;
		jumpForce = 5f;
		
		/* Allow movement. */
		rotationEnabled = true;
		movementEnabled = true;
		lastRotation = 0;
		up = new Vector3(0.0f, 1.0f, 0.0f);
		
		control = true;
	}

	/**
	 * Move the player with the current input.
	 */
	public void input() {
		if (rotationEnabled) {
			/* Determine the mouse acceleration. */
			float rotation = Gdx.input.getDeltaX() * Cubic.defaults.mouseSensitivity;
			float mouseAcceleration = rotation - lastRotation;
			
			Vector3 current = rigidBody.getAngularVelocity();
			if (control) {
				Vector3 adjusted = up.cpy().scl(-rotation);
				current.y = adjusted.y;
				rigidBody.setAngularVelocity(current);	
			}			
			
//			/* Apply an impulse if below terminal. */
//			if (rigidBody.getAngularVelocity().len2() < angularVelocityLimit) {
//				float magnitude = Math.signum(mouseAcceleration) * (float)Math.sqrt(Math.abs(mouseAcceleration) * angularAccelerationFactor);
//				Vector3 r = new Vector3(1, 0, 0);
//				Vector3 f = (new Vector3(0, 0, 1)).scl(magnitude);
//				/* TODO: scale to meet the limit. *//*
//				rigidBody.applyTorqueImpulse(r.crs(f));
//			}
			
			/* Get the azimuth for the camera. */
			Quaternion bodyRot = new Quaternion();
			rigidBody.getCenterOfMassTransform().getRotation(bodyRot);
			azimuth = -bodyRot.getYaw();
		} 
		
		if (movementEnabled) {
			/* Get the raw vector of the keyboard input. */
			Vector3 joystick = new Vector3();
			if (Gdx.input.isKeyPressed(Input.Keys.W)) joystick.x += 1;
			if (Gdx.input.isKeyPressed(Input.Keys.S)) joystick.x -= 1;
			if (Gdx.input.isKeyPressed(Input.Keys.A)) joystick.z -= 1;
			if (Gdx.input.isKeyPressed(Input.Keys.D)) joystick.z += 1;
			if (Gdx.input.isKeyPressed(Input.Keys.R)) rigidBody.applyTorqueImpulse(new Vector3(1, 1, 1));
			if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && control) {
				joystick.y += jumpForce;
				control = false;
			}
			
			/* Relate the vector to the cube's orientation. */
			joystick = joystick.rotate(up, -azimuth);
			
			/* Limit the joystick. */
			if (rigidBody.getLinearVelocity().len2() < linearVelocityLimit) {
				/* TODO: scale to meet the limit. */
				rigidBody.applyCentralImpulse(joystick);
			}
		}
	}
	
	/**
	 * Update the entire player. 
	 */
	@Override
	public void update() {
		input();
	}
	
	/**
	 * Dispose of the player. 
	 */
	@Override
	public void dispose() {
		collisionShape.dispose();
		constructionInfo.dispose();
		rigidBody.dispose();
		model.dispose();
	}
	
	/** 
	 * Get the translation of the player. 
	 * @return a vector containing the player's coordinates.
	 */
	public Vector3 getTranslation() {
		return new Vector3(transform.val[Matrix4.M03], transform.val[Matrix4.M13], transform.val[Matrix4.M23]);
	}
}
