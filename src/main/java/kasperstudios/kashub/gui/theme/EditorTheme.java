package kasperstudios.kashub.gui.theme;

public class EditorTheme {
    public final String id;
    public final String name;

    public final int backgroundColor;
    public final int sidebarColor;
    public final int toolbarColor;
    public final int statusBarColor;

    public final int accentColor;
    public final int accentColorHover;
    public final int buttonColor;
    public final int buttonHoverColor;

    public final int textColor;
    public final int textDimColor;
    public final int lineNumberColor;
    public final int selectionColor;
    public final int cursorColor;

    public final int keywordColor;
    public final int stringColor;
    public final int numberColor;
    public final int commentColor;
    public final int functionColor;
    public final int variableColor;
    public final int operatorColor;

    public final int consoleBackground;
    public final int consoleInfoColor;
    public final int consoleWarnColor;
    public final int consoleErrorColor;
    public final int consoleSuccessColor;

    public static final EditorTheme DEFAULT = new EditorTheme(
        "default", "Default",
        0xFF1E1E2E,
        0xFF181825,
        0xFF11111B,
        0xFF11111B,

        0xFFCBA6F7,
        0xFFDDBDFF,
        0xFF45475A,
        0xFF585B70,

        0xFFCDD6F4,
        0xFF6C7086,
        0xFF6C7086,
        0x4489B4FA,
        0xFFF5E0DC,

        0xFFCBA6F7,
        0xFFA6E3A1,
        0xFFFAB387,
        0xFF6C7086,
        0xFF89B4FA,
        0xFFF9E2AF,
        0xFF94E2D5,

        0xFF11111B,
        0xFF89B4FA,
        0xFFF9E2AF,
        0xFFF38BA8,
        0xFFA6E3A1
    );

    public static EditorTheme getDefault() {
        return DEFAULT;
    }

    public EditorTheme(String id, String name, int backgroundColor, int sidebarColor, int toolbarColor, int statusBarColor,
                       int accentColor, int accentColorHover, int buttonColor, int buttonHoverColor,
                       int textColor, int textDimColor, int lineNumberColor, int selectionColor, int cursorColor,
                       int keywordColor, int stringColor, int numberColor, int commentColor, int functionColor,
                       int variableColor, int operatorColor, int consoleBackground, int consoleInfoColor,
                       int consoleWarnColor, int consoleErrorColor, int consoleSuccessColor) {
        this.id = id;
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.sidebarColor = sidebarColor;
        this.toolbarColor = toolbarColor;
        this.statusBarColor = statusBarColor;
        this.accentColor = accentColor;
        this.accentColorHover = accentColorHover;
        this.buttonColor = buttonColor;
        this.buttonHoverColor = buttonHoverColor;
        this.textColor = textColor;
        this.textDimColor = textDimColor;
        this.lineNumberColor = lineNumberColor;
        this.selectionColor = selectionColor;
        this.cursorColor = cursorColor;
        this.keywordColor = keywordColor;
        this.stringColor = stringColor;
        this.numberColor = numberColor;
        this.commentColor = commentColor;
        this.functionColor = functionColor;
        this.variableColor = variableColor;
        this.operatorColor = operatorColor;
        this.consoleBackground = consoleBackground;
        this.consoleInfoColor = consoleInfoColor;
        this.consoleWarnColor = consoleWarnColor;
        this.consoleErrorColor = consoleErrorColor;
        this.consoleSuccessColor = consoleSuccessColor;
    }
}
