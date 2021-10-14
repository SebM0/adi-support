package com.axway.adi.tools.util;

import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageTableCell<S> extends TableCell<S, Number> {
    final ImageView imageview = new ImageView();
    final Image[] images;

    public ImageTableCell(String[] imagePaths) {
        images = new Image[imagePaths.length];
        for (int i = 0; i < imagePaths.length; i++) {
            images[i] = createImage(imagePaths[i]);
        }
        imageview.setFitHeight(20);
        imageview.setFitWidth(20);
        setGraphic(imageview);
    }

    @Override
    protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
            imageview.setImage(images[item.intValue()]);
        }
    }

    private static Image createImage(String name) {
        return new Image("file:resources/" + name + ".png");
    }
}
