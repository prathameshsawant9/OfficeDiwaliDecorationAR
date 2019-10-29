package com.leo.ar.nodes;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.leo.ar.helpers.BaseAnchor;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class Augmented2DTextureNode extends BaseAnchor {

    private WeakReference<ModelRenderable> modelRenderable = new WeakReference(null);
    private AugmentedImage detectedImage;
    private Vector3 localPosition = new Vector3(0F, 0F, 0F);

    public Augmented2DTextureNode(Context context, float scaleSize, int drawableImage) {
        if(modelRenderable.get() == null){
            loadTexture(context, scaleSize, scaleSize, drawableImage);
        }
    }

    public Augmented2DTextureNode(Context context, float scaleSizeX, float scaleSizeZ, int drawableImage){
        if(modelRenderable.get() == null){
            loadTexture(context, scaleSizeX, scaleSizeZ, drawableImage);
        }
    }

    public Augmented2DTextureNode(Context context, float scaleSizeX, float scaleSizeZ, int drawableImage, Vector3 localPosition){
        // update position
        this.localPosition = localPosition;

        if(modelRenderable.get() == null){
            loadTexture(context, scaleSizeX, scaleSizeZ, drawableImage);
        }
    }

    private void loadTexture(Context context, float scalSizeX, float scaleSizeZ, int drawableImage){
        Texture.builder().setSource(BitmapFactory.decodeResource(context.getResources(), drawableImage))
                .setSampler(Texture.Sampler.builder().setWrapMode(Texture.Sampler.WrapMode.CLAMP_TO_EDGE).build())
                .build()
                .thenCompose(
                        texture -> MaterialFactory.makeTransparentWithTexture(context, texture)
                ).thenAccept(material -> {
            Vector3 size = new Vector3(scalSizeX, 0F, scaleSizeZ);
            Vector3 center = new Vector3(0F, 0F, 0F);

            modelRenderable = new WeakReference<>(ShapeFactory.makeCube(size, center, material));

            setImage(detectedImage);
        });
    }

    @Override
    public void setImage(AugmentedImage image) {
        detectedImage = image;

        if (modelRenderable.get() == null)
            return;

        setAnchor(image.createAnchor(image.getCenterPose()));
        addNode();
    }

    /**
     * This is required specially when you want to move/scale a object,
     * because you can directly transform a already anchored object
     */
    private void addNode(){
        // Reference
        // https://github.com/google-ar/sceneform-android-sdk/blob/master/samples/augmentedimage/app/src/main/java/com/google/ar/sceneform/samples/augmentedimage/AugmentedImageNode.java

        Node textureNode = new Node();
        textureNode.setRenderable(modelRenderable.get());
        Timber.i("MainActivity Local position: %s ", textureNode.getLocalPosition());
        textureNode.setLocalScale(new Vector3(0.5F, 0.5F, 0.5F));
        textureNode.setLocalPosition(localPosition);
        textureNode.setParent(this);
    }
}
