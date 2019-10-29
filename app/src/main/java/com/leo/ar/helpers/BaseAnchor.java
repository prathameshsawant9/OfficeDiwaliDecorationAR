package com.leo.ar.helpers;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;

public abstract class BaseAnchor extends AnchorNode {
    abstract public void setImage(AugmentedImage image);
}
