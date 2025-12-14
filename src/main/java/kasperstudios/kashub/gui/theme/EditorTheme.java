package kasperstudios.kashub.gui.theme;

public class EditorTheme {
    public final String id;
    public final String name;
    
    // Main colors
    public final int backgroundColor;
    public final int sidebarColor;
    public final int toolbarColor;
    public final int statusBarColor;
    
    // Accent colors
    public final int accentColor;
    public final int accentColorHover;
    public final int buttonColor;
    public final int buttonHoverColor;
    
    // Text colors
    public final int textColor;
    public final int textDimColor;
    public final int lineNumberColor;
    public final int selectionColor;
    public final int cursorColor;
    
    // Syntax highlighting
    public final int keywordColor;
    public final int stringColor;
    public final int numberColor;
    public final int commentColor;
    public final int functionColor;
    public final int variableColor;
    public final int operatorColor;
    
    // Console colors
    public final int consoleBackground;
    public final int consoleInfoColor;
    public final int consoleWarnColor;
    public final int consoleErrorColor;
    public final int consoleSuccessColor;
    
    public static final EditorTheme DEFAULT = new EditorTheme(
        "default", "Default",
        0xFF1E1E2E, // backgroundColor
        0xFF181825, // sidebarColor
        0xFF11111B, // toolbarColor
        0xFF11111B, // statusBarColor
        
        0xFFCBA6F7, // accentColor
        0xFFDDBDFF, // accentColorHover
        0xFF45475A, // buttonColor
        0xFF585B70, // buttonHoverColor
        
        0xFFCDD6F4, // textColor
        0xFF6C7086, // textDimColor
        0xFF6C7086, // lineNumberColor
        0x4489B4FA, // selectionColor
        0xFFF5E0DC, // cursorColor
        
        0xFFCBA6F7, // keywordColor
        0xFFA6E3A1, // stringColor
        0xFFFAB387, // numberColor
        0xFF6C7086, // commentColor
        0xFF89B4FA, // functionColor
        0xFFF9E2AF, // variableColor
        0xFF94E2D5, // operatorColor
        
        0xFF11111B, // consoleBackground
        0xFF89B4FA, // consoleInfoColor
        0xFFF9E2AF, // consoleWarnColor
        0xFFF38BA8, // consoleErrorColor
        0xFFA6E3A1  // consoleSuccessColor
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
