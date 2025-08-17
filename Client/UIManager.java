package Client;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class UIManager {
    private final TextArea textArea;

    public UIManager(TextArea textArea) {
        this.textArea = textArea;
    }

    public void insertText(int pos, String text) {
        Platform.runLater(() -> {
            int safePos = Math.min(Math.max(0, pos), textArea.getLength());
            int currentCaret = textArea.getCaretPosition();

            textArea.insertText(safePos, text);

            // Move caret to the end of the inserted text if insertion was at current caret
            // position
            // This provides immediate feedback for the user who just typed
            if (safePos == currentCaret) {
                textArea.positionCaret(safePos + text.length());
            } else if (safePos < currentCaret) {
                // Insertion happened before caret, move caret forward
                textArea.positionCaret(currentCaret + text.length());
            }
            // If insertion happened after caret, don't move caret
        });
    }

    public void deleteText(int pos, int length) {
        Platform.runLater(() -> {
            // Ensure bounds are valid
            if (pos < 0 || pos >= textArea.getLength()) {
                return;
            }

            // Adjust length if it would exceed text bounds
            int actualLength = Math.min(length, textArea.getLength() - pos);
            if (actualLength <= 0) {
                return;
            }

            int currentCaret = textArea.getCaretPosition();

            // Perform the deletion
            textArea.deleteText(pos, pos + actualLength);

            // Position caret intelligently
            if (pos + actualLength <= currentCaret) {
                // Deletion happened completely before caret - move caret back
                textArea.positionCaret(currentCaret - actualLength);
            } else if (pos < currentCaret) {
                // Deletion overlaps with caret position - place caret at deletion start
                textArea.positionCaret(pos);
            }
            // If deletion happened after caret, don't move caret
        });
    }

    public void setText(String text) {
        Platform.runLater(() -> textArea.setText(text));
    }
}
