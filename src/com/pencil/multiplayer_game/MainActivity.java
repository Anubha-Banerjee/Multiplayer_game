package com.pencil.multiplayer_game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.SurfaceGestureDetector;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.algorithm.collision.LineCollisionChecker;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;



import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;


public class MainActivity  extends SimpleBaseGameActivity implements SensorEventListener, IOnSceneTouchListener{
	// ===========================================================
	// Constants
	// ===========================================================

	/* Initializing the Random generator produces a comparable result over different versions. */
	private static final long RANDOM_SEED = 1234567890;

	private static final int CAMERA_WIDTH = 540;
	private static final int CAMERA_HEIGHT = 960;
	public static float sensorAx = 0.0f; 
	public static float sensorAy = 0.0f;   
	public static float sensorAz = 0.0f;
	static int screenRotation;
	private SensorManager mSensorManager;
	float ax=0, ay=0, az=0;	
	double distanceX = 0, distanceY = 0;

	private static final int LINE_COUNT = 100;
	private boolean isHorizontal = false;
	boolean previous_isHorizontal = isHorizontal;

	static double vx=0;
	// speed
	static double vy=0;
	static double constantFriction = 0;
	Rect r;
	// acceleration
	public static final float DistanceScale = 10;	
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
	public EngineOptions onCreateEngineOptions() {
		
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);  
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// don't want automatic orientation change 
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public void onCreateResources() {

	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		scene.setOnSceneTouchListener(this);

		SurfaceGestureDetector surfaceGestureDetector = new SurfaceGestureDetector(
				this) {

			@Override
			protected boolean onSwipeUp() {
				System.out.println("onSwipeUp");
				return true;
			}

			@Override
			protected boolean onSwipeRight() {
				System.out.println("onSwipeRight");
				return true; 
			}

			@Override
			protected boolean onSwipeLeft() {
				System.out.println("onSwipeLeft");
				return true;
			}

			@Override
			protected boolean onSwipeDown() {
				System.out.println("onSwipeDown");
				return true;
			}

			@Override
			protected boolean onSingleTap() {
				System.out.println("onSingleTap");
				return true;
			}

			@Override
			protected boolean onDoubleTap() {
				System.out.println("onDoubleTap");
				return true;
			}
		};
		
		 //surfaceGestureDetector.setEnabled(true);
		 //scene.setOnSceneTouchListener(surfaceGestureDetector);
		
		final Random random = new Random(RANDOM_SEED);
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		scene.registerUpdateHandler(new IUpdateHandler() {
			 float x_start = 0.2f * CAMERA_WIDTH;		
			 float y_start = 0.2f  * CAMERA_HEIGHT;
			 float y_end = 0.2f * CAMERA_HEIGHT;
			 float x_end = x_start;
			 float lineWidth = 2 * 5;		
			 List<Line> lines = new ArrayList<Line>();
			
			float x = 100 ,y = 100;
			public void reset() {
			}
			// main game loop
			public void onUpdate(float pSecondsElapsed) {					
				// add the first line
				if (lines.size() == 0) {
					lines.add(new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager));
					Line lineAdded = lines.get(lines.size() - 1);
					scene.attachChild(lineAdded);
				}
				// direction changed
				else if (previous_isHorizontal != isHorizontal) {
					Line lastLine = lines.get(lines.size() - 1);
					x_start = lastLine.getX2();
					y_start = lastLine.getY2();
					x_end = x_start;
					y_end = y_start;
					lines.add(new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager));
					Line lineAdded = lines.get(lines.size() - 1);
					scene.attachChild(lineAdded);
				}
				// moving in same direction
				else {
					if (isHorizontal) {
						x_end = (float) (x_end + 1);
					}
					else {
						y_end = (float) (y_end + 1);
					}

					// pop the line from stack and scene
					scene.detachChild(lines.get(lines.size() - 1));
					lines.remove(lines.get(lines.size() - 1));

					// push a new longer line on stack and scene
					lines.add(new Line(x_start, y_start, x_end, y_end,
							lineWidth, vertexBufferObjectManager));
					scene.attachChild(lines.get(lines.size() - 1));
				}
			}
		});		
		return scene;
	}	
	
	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		previous_isHorizontal = isHorizontal;
		if (pSceneTouchEvent.isActionDown()) {
			if (isHorizontal == false)
				isHorizontal = true;
			else
				isHorizontal = false;

			return true;
		}
		return false;
	}
	
	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		// TODO Auto-generated method stub
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{					
			az = sensorEvent.values[2];
			// figure out correct ax, ay depending on orientation
			// cross fingers and hope this will work on all devices
			// see http://android-developers.blogspot.in/2010/09/one-screen-turn-deserves-another.html for details
			switch(screenRotation)
			{
                case Surface.ROTATION_0:
                    ax = -sensorEvent.values[0];
                    ay = sensorEvent.values[1];
                    break;
                default:
                    ax = sensorEvent.values[1];
                    ay = sensorEvent.values[0];
                    break	;					
			}		
			
			// low pass filter
			final float alpha = 0.5f; 
			sensorAx = alpha * sensorAx + (1 - alpha) * ax;
			sensorAy = alpha * sensorAy + (1 - alpha) * ay;	
			sensorAz = alpha * sensorAz + (1 - alpha) * az;
			    	   
		}	
	}
	protected void onResume() { 
        super.onResume();      
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
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

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
