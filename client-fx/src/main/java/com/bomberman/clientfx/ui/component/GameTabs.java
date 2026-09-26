package com.bomberman.clientfx.ui.component;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Segmented tab switch, e.g. LOGIN / REGISTER. Exactly one tab is always selected. */
public final class GameTabs extends HBox {

    private final ToggleGroup group = new ToggleGroup();
    private final ReadOnlyIntegerWrapper selectedIndex = new ReadOnlyIntegerWrapper(this, "selectedIndex", 0);

    public GameTabs(String... labels) {
        getStyleClass().add("game-tabs");
        for (int index = 0; index < labels.length; index++) {
            ToggleButton tab = new ToggleButton(labels[index]);
            tab.getStyleClass().setAll("game-tab");
            tab.setToggleGroup(group);
            tab.setUserData(index);
            getChildren().add(tab);
        }
        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                group.selectToggle(previous);
            } else {
                selectedIndex.set((Integer) selected.getUserData());
            }
        });
        select(0);
        setMaxWidth(USE_PREF_SIZE);
    }

    public void select(int index) {
        group.selectToggle(group.getToggles().get(index));
    }

    public ReadOnlyIntegerProperty selectedIndexProperty() {
        return selectedIndex.getReadOnlyProperty();
    }
}
