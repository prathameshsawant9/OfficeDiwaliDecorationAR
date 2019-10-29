package com.leo.ar;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;
import com.leo.ar.helpers.BaseActivity;
import com.leo.ar.helpers.BaseAnchor;
import com.leo.ar.nodes.Augmented2DTextureNode;
import com.leo.ar.nodes.AugmentedDynamic3DNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static java.security.AccessController.getContext;

public class MainActivity extends BaseActivity implements Scene.OnUpdateListener {
    private ArFragment arFragment;
    private TextView txtLogs;

    private final Map<AugmentedImage, AnchorNode> augmentedImageMap = new HashMap<>();
    private int detectionCounter = 0;
    private int trackingCounter = 0;

    private final Handler myHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            txtLogs.setText(trackingCounter+" / "+detectionCounter);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregister();
    }

    /*
     * INIT
     */

    private void init(){
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        txtLogs = findViewById(R.id.tvLogs);
        register();
    }

    private void register(){
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
    }

    private void unregister(){
        try{
            arFragment.getArSceneView().getScene().removeOnUpdateListener(this);
        }catch (Exception e){
            Timber.i(e);
        }
    }

    /*
     * AR Frame Listener
     */

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    detectionCounter += 1;
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    Timber.i("Image Tracking ");

                    // Only if its not on the screen
                    if (! augmentedImageMap.containsKey(augmentedImage)){

                        BaseAnchor augmentedNode = null;

                        switch (augmentedImage.getName()){

                            // wall decor 1
                            // Image = abstract_3
                            case "abs_3":
                                augmentedNode = new Augmented2DTextureNode(this,
                                        0.5F, 0.8F,
                                        R.drawable.diwali_wall_decoration_1,
                                        new Vector3(0F, 0F, 1.05F * augmentedImage.getExtentZ()));
                                break;

                            // rangoli
                            // Image = abstract_5
                            case "abs_5":
                                augmentedNode = new Augmented2DTextureNode(this, 1.5F, R.drawable.diwali_rangoli_scaled_571_640);
                                break;

                            // diya
                            // Image = abstract_6
                            case "abs_6":
                                augmentedNode = new AugmentedDynamic3DNode(this, "diya_textured");
                                break;

                            // kandil
                            // Image = abstract_7
                            case "abs_7":
                                augmentedNode = new AugmentedDynamic3DNode(this, "kandil_texture");
                                break;

                            // wall decor 2
                            // Image = abstract_8
                            case "abs_8":
                                augmentedNode = new Augmented2DTextureNode(this,
                                        0.25F,1F, R.drawable.diwali_wall_decoration_2,
                                        new Vector3(0F, 0F, 1.45F * augmentedImage.getExtentZ()));
                                break;

                            default:
                                // Nothing
                                break;
                        }

                        // Add to the frame
                        if (augmentedNode != null){
                            augmentedNode.setImage(augmentedImage);
                            addNode(augmentedNode);
                            augmentedImageMap.put(augmentedImage, augmentedNode);
                            trackingCounter += 1;
                        }
                    }

                    break;

                case STOPPED:
                    Timber.i("Image out of scope ");
                    augmentedImageMap.remove(augmentedImage);
                    trackingCounter -= 1;
                    break;
            }

            myHandler.sendEmptyMessage(0);
        }
    }

    /*
     * AR Image DB
     */

    public void loadARResources(Config config, Session session){
        setUpARDatabase(config, session);
    }

    public void setUpARDatabase(Config config, Session session){
        AssetManager assetManager = getContext() != null ? getAssets() : null;
        if (assetManager == null) {
            Timber.i("Asset Manager is null");
            return;
        }


        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);

        // Load Images in DB

        // Option 1
        // You can load it at runtime but then your it adds more delay, so its better to make db using arcoreimg tool
        // augmentedImageDatabase.addImage("abs_1", getBitmap(assetManager, "abstract_1.jpg"));


        // Option 2
        // Just the load the DB generated using arcoreimg tool
        // Much faster
        try (InputStream is = getAssets().open("abstract_images.imgdb")) {
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
        } catch (IOException e) {
            Timber.i(e);
            Timber.i("IO exception loading augmented image database.");
        }

        // push the changes
        config.setAugmentedImageDatabase(augmentedImageDatabase);
    }

    private Bitmap getBitmap(AssetManager assetManager, String filename){
        return getAssetImage(assetManager, filename);
    }

    private void addNode(AnchorNode anchorNode){
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }

    public Bitmap getAssetImage(AssetManager assetManager, String assetFilename){
        try (InputStream is = assetManager.open(assetFilename)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
           Timber.i(e);
        }
        return null;
    }
}
