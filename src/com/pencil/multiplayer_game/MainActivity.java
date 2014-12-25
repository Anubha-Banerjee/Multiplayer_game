package com.pencil.multiplayer_game;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.SurfaceGestureDetector;
import org.andengine.input.touch.detector.SurfaceGestureDetectorAdapter;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.appcompat.R.color;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

public class MainActivity extends SimpleBaseGameActivity implements
		SensorEventListener, IAccelerationListener, IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	/*
	 * Initializing the Random generator produces a comparable result over
	 * different versions.
	 */
	private static final long RANDOM_SEED = 1234567890;

	private static final int CAMERA_WIDTH = 540;
	private static final int CAMERA_HEIGHT = 960;
	public static float sensorAx = 0.0f;
	public static float sensorAy = 0.0f;
	public static float sensorAz = 0.0f;
	static int screenRotation;
	private SensorManager mSensorManager;
	float ax = 0, ay = 0, az = 0;
	double distanceX = 0, distanceY = 0;
	float x_start = 0.2f * CAMERA_WIDTH;
	float y_start = 0.2f * CAMERA_HEIGHT;
	float y_end = 0.2f * CAMERA_HEIGHT;
	float x_end = x_start;
	float lineWidth = 2;
	Sprite pencil;
	Sprite pen;
	Body penBody;	
	boolean isTouch = false;
	Scene scene;
	private static final int LINE_COUNT = 100;

	static double vx = 0;
	// speed
	static double vy = 0;
	static double constantFriction = 0;
	Rect r;
	List<Line> lines = new ArrayList<Line>();
	private enum direction {
		left, right, up, down
	};

	private direction currentDirection = direction.down,
			previousDirection = currentDirection;
	private SurfaceGestureDetector mSGDA;
	// acceleration
	public static final float DistanceScale = 10;

	private ITexture pencilTexture;
	private ITextureRegion pencilTextureRegion;
	private TiledTextureRegion penTextureRegion;
	private PhysicsWorld mPhysicsWorld;
	private BuildableBitmapTextureAtlas mBitmapTextureAtlas;
	private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(0.1f, 0, 1);

	// 
	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	protected void onCreate(final Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);

		this.mSGDA = new SurfaceGestureDetector(null) {

			@Override
			protected boolean onSingleTap() {
				// TODO Auto-generated method stub
				for(int i = 0; i < lines.size(); i++) {
					scene.detachChild(lines.get(i));
				}
				if(lines.size() > 0)
					lines.clear();
				return false;
			}

			@Override
			protected boolean onDoubleTap() {
				if(isTouch) {
					isTouch = false;					
					scene.attachChild(pen);
					scene.detachChild(pencil);
				}
				else {
					isTouch = true;
					scene.attachChild(pencil);
					scene.detachChild(pen);					
				}
				return false;
			}

			@Override
			protected boolean onSwipeUp() {
				previousDirection = currentDirection;
				if (currentDirection != direction.down) {
					currentDirection = direction.up;
				}
				return false;
			}

			@Override
			protected boolean onSwipeDown() {
				previousDirection = currentDirection;
				if (currentDirection != direction.up) {
					currentDirection = direction.down;
				}
				return false;
			}

			@Override
			protected boolean onSwipeLeft() {
				previousDirection = currentDirection;
				if (currentDirection != direction.right) {
					currentDirection = direction.left;
				}
				return false;
			}

			@Override
			protected boolean onSwipeRight() {
				previousDirection = currentDirection;
				if (currentDirection != direction.left) {
					currentDirection = direction.right;
				}
				return false;
			}
		};

		this.mSGDA.setEnabled(true);

	}

	@Override
	public EngineOptions onCreateEngineOptions() {

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// don't want automatic orientation change
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED,
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public Scene onCreateScene() {

		this.mEngine.registerUpdateHandler(new FPSLogger());
		final Color gray = new Color(0.5f,0.5f,0.5f);
		scene = new Scene();
		scene.setBackground(new Background(1,1,1));
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);
		// scene.setOnSceneTouchListener(this);
		scene.setOnSceneTouchListener(mSGDA);

		final Random random = new Random(RANDOM_SEED);
		final VertexBufferObjectManager vertexBufferObjectManager = this
				.getVertexBufferObjectManager();
		
		pen = new AnimatedSprite(x_end, y_end-penTextureRegion.getHeight(), this.penTextureRegion, this.getVertexBufferObjectManager());
		penBody = PhysicsFactory.createCircleBody(this.mPhysicsWorld, pen, BodyType.DynamicBody, FIXTURE_DEF);
		penBody.setLinearDamping(12);
		penBody.setGravityScale(20);

		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2 - pen.getWidth(), vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2 + pen.getWidth(), 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0, 0);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		scene.attachChild(ground);
		scene.attachChild(roof);
		scene.attachChild(left);
		scene.attachChild(right);	
	
		
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(pen, penBody, true, true));
		scene.registerUpdateHandler(this.mPhysicsWorld);
		
		pencil = new Sprite(x_end, y_end,
				this.pencilTextureRegion, this.getVertexBufferObjectManager());
		
		if(isTouch) {
			scene.attachChild(pencil);
		}
		scene.registerUpdateHandler(new IUpdateHandler() {

			
			public void reset() {
			}

			// main game loop
			public void onUpdate(float pSecondsElapsed) {
				
				if(!isTouch) {					
					Vector2 linearVel = penBody.getLinearVelocity();
					previousDirection = currentDirection;
					if(Math.abs(linearVel.x) > Math.abs(linearVel.y)) {
						penBody.setLinearVelocity(linearVel.x, 0);
						if(linearVel.x > 0 && currentDirection != direction.left) {
							currentDirection = direction.right;
						}
						else if(linearVel.x < 0 && currentDirection != direction.right) {
							currentDirection = direction.left;
						}
					}
					else {						
						penBody.setLinearVelocity(0, linearVel.y);
						if(linearVel.y > 0 && currentDirection != direction.up)  {
							currentDirection = direction.down;
						}
						else if(linearVel.y < 0 && currentDirection != direction.down) {
							currentDirection = direction.up;
						}
					}
				}
				// add the first line
				if (lines.size() == 0) {
					lines.add(new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager));
					Line lineAdded = lines.get(lines.size() - 1);
					scene.attachChild(lineAdded);
				}
				// direction changed
				else if (previousDirection != currentDirection) {
					Line lastLine = lines.get(lines.size() - 1);
					x_start = lastLine.getX2();
					y_start = lastLine.getY2();
					x_end = x_start;
					y_end = y_start;
					lines.add(new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager));
					Line lineAdded = lines.get(lines.size() - 1);
					lineAdded.setColor(gray);
					scene.attachChild(lineAdded);
				}
				// moving in same direction
				else {
					if(!isTouch) {
						x_end = pen.getX();
						y_end = pen.getY()+ pen.getHeight();
						scene.detachChild(pen);
						scene.attachChild(pen);
					}
					if(isTouch) {
						switch (currentDirection) { 
						case right:
							x_end = (float) (x_end + 2);
							break;
						case left:
							x_end = (float) (x_end - 2);
							break;
						case down:
							y_end = (float) (y_end + 2);
							break;
						case up:
							y_end = (float) (y_end - 2);
							break;
						}
						pencil.setX(x_end);
						pencil.setY(y_end-pencil.getHeight());
						scene.detachChild(pencil);
						scene.attachChild(pencil);						
					}
					
					// pop the line from stack and scene
					scene.detachChild(lines.get(lines.size() - 1));
					lines.remove(lines.get(lines.size() - 1));

					// push a new longer line on stack and scene
					Line longerLine = new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager);
					lines.add(longerLine);
					longerLine.setColor(gray);
					scene.attachChild(longerLine);
					
				}

				// check collision of lines
				for(int i = 0; i < lines.size()-2; i++) {
					Line currentLine = lines.get(lines.size() - 1);
					if(currentLine.collidesWith(lines.get(i))) {
						currentLine.setColor(1,0,0);
					}
				}				
				previousDirection = currentDirection;				
			}
		});
	
		return scene;
	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene,
			final TouchEvent pSceneTouchEvent) {
		return false;
	}


	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("sprites/");
		this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 512, 512, TextureOptions.NEAREST);
		
		this.penTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "pencil.png", 1, 1); // 64x32

		try {
			this.mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 1));
			this.mBitmapTextureAtlas.load();
			
			this.mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 1));
			this.mBitmapTextureAtlas.load();
			
			
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}
		
		this.pencilTexture = new BuildableBitmapTextureAtlas(
				this.getTextureManager(), 512, 512, TextureOptions.NEAREST);

		// load the textures
		try {
			// load the ship texture
			this.pencilTexture = new BitmapTexture(this.getTextureManager(),
					new IInputStreamOpener() {
						@Override
						public InputStream open() throws IOException {
							return getAssets().open("sprites/pencil.png");
						}
					});
			this.pencilTexture.load();
			this.pencilTextureRegion = TextureRegionFactory
					.extractFromTexture(this.pencilTexture);

		} catch (IOException e1) {
			Log.e("exception: :", "Exception loading textures");
			e1.printStackTrace();
		}

	}
	@Override
	public void onAccelerationChanged(final AccelerationData pAccelerationData) {
		int accelerationScale = 1;
		final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX()*accelerationScale,
				pAccelerationData.getY()*accelerationScale);
		this.mPhysicsWorld.setGravity(gravity);
		Vector2Pool.recycle(gravity);
	}
	@Override
	public void onResumeGame() {
		super.onResumeGame();
		this.enableAccelerationSensor(this);
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();
		this.disableAccelerationSensor();
	}

	@Override
	public void onAccelerationAccuracyChanged(AccelerationData pAccelerationData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
