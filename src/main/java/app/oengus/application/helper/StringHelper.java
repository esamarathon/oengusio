package app.oengus.application.helper;

import app.oengus.domain.IUsername;

public class StringHelper {
    public static String escapeMarkdown(String input) {
        return input.replace("*", "\\*")
            .replace("_", "\\_")
            .replace("`", "\\`")
            .replace(">", "\\>")
            .replace("||", "\\||")
            ;
    }

    public static String getUserDisplay(IUsername user) {
        return "%s (%s)".formatted(user.getDisplayName(), user.getUsername());
    }
}
