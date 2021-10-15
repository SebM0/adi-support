package com.axway.adi.tools.util;

import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.TableCell;

public class ImageTableCell<S> extends TableCell<S, Number> {
    final FontIcon[] images;

    public ImageTableCell(String[] imagePaths) {
        images = new FontIcon[imagePaths.length];
        for (int i = 0; i < imagePaths.length; i++) {
            images[i] = createImage(imagePaths[i]);
        }
    }

    @Override
    protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
            setGraphic(images[item.intValue()]);
        }
    }

    private static FontIcon createImage(String name) {
        FontIcon icon = new FontIcon(name);
        icon.setIconSize(20);
        return icon;
    }
}
