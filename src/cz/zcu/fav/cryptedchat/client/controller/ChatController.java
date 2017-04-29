package cz.zcu.fav.cryptedchat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ChatController implements MainController.MessageHandler {

    @FXML private VBox container;

    @Override
    public void addMessage(String message, boolean fromMe) {
        container.getChildren().add(new Message(message, fromMe));
    }

    private static class Message extends TextFlow {
        private static final String TEXT_YOU = "Ty: ";
        private static final String TEXT_OTHER = "Ten druhý: ";

        public Message(String message, boolean fromMe) {
            getChildren().addAll(
                new Text(fromMe ? TEXT_YOU : TEXT_OTHER),
                new Text(message)
            );
            if (!fromMe) {
                setStyle("-fx-text-alignment: left;");
            }
        }
    }
}
