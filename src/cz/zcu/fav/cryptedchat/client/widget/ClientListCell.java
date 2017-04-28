package cz.zcu.fav.cryptedchat.client.widget;

import cz.zcu.fav.cryptedchat.client.Controller;
import cz.zcu.fav.cryptedchat.client.Controller.Client;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.converter.NumberStringConverter;

public class ClientListCell extends ListCell<Controller.Client> {

    private final HBox container = new HBox();
    private final Circle statusIndicator = new Circle(10);
    private final Label clientId = new Label();

    {
        container.getChildren().addAll(statusIndicator, clientId);
    }

    @Override
    protected void updateItem(Client item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
            setText(null);
            return;
        }

        statusIndicator.fillProperty().bind(
            Bindings.when(item.onlineProperty()).then(Color.GREEN).otherwise(Color.RED)
        );
        clientId.textProperty().bindBidirectional(item.idProperty(), new NumberStringConverter());
        setGraphic(container);
    }
}
