package kasperstudios.kashub.gui.theme;

import kasperstudios.kashub.config.KashubConfig;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final List<EditorTheme> themes = new ArrayList<>();
    private static int currentIndex = 0;
    
    static {
        // Catppuccin Mocha (default)
        themes.add(new EditorTheme(
            "catppuccin", "Catppuccin Mocha",
            0xFF1E1E2E, 0xFF181825, 0xFF11111B, 0xFF11111B,
            0xFFCBA6F7, 0xFFDDBDFF, 0xFF45475A, 0xFF585B70,
            0xFFCDD6F4, 0xFF6C7086, 0xFF6C7086, 0x4489B4FA, 0xFFF5E0DC,
            0xFFCBA6F7, 0xFFA6E3A1, 0xFFFAB387, 0xFF6C7086, 0xFF89B4FA, 0xFFF9E2AF, 0xFF94E2D5,
            0xFF11111B, 0xFF89B4FA, 0xFFF9E2AF, 0xFFF38BA8, 0xFFA6E3A1
        ));
        
        // Tokyo Night
        themes.add(new EditorTheme(
            "tokyo_night", "Tokyo Night",
            0xFF1A1B26, 0xFF16161E, 0xFF13131A, 0xFF13131A,
            0xFF7AA2F7, 0xFF89B4FA, 0xFF3B4261, 0xFF4C5478,
            0xFFC0CAF5, 0xFF565F89, 0xFF565F89, 0x447AA2F7, 0xFFC0CAF5,
            0xFF9D7CD8, 0xFF9ECE6A, 0xFFFF9E64, 0xFF565F89, 0xFF7AA2F7, 0xFFE0AF68, 0xFF89DDFF,
            0xFF16161E, 0xFF7AA2F7, 0xFFE0AF68, 0xFFF7768E, 0xFF9ECE6A
        ));
        
        // Dracula
        themes.add(new EditorTheme(
            "dracula", "Dracula",
            0xFF282A36, 0xFF21222C, 0xFF191A21, 0xFF191A21,
            0xFFBD93F9, 0xFFD4AFFF, 0xFF44475A, 0xFF565970,
            0xFFF8F8F2, 0xFF6272A4, 0xFF6272A4, 0x44BD93F9, 0xFFF8F8F2,
            0xFFFF79C6, 0xFFF1FA8C, 0xFFBD93F9, 0xFF6272A4, 0xFF50FA7B, 0xFFFFB86C, 0xFF8BE9FD,
            0xFF21222C, 0xFF8BE9FD, 0xFFFFB86C, 0xFFFF5555, 0xFF50FA7B
        ));
        
        // One Dark Pro
        themes.add(new EditorTheme(
            "one_dark", "One Dark Pro",
            0xFF282C34, 0xFF21252B, 0xFF1B1D23, 0xFF1B1D23,
            0xFF61AFEF, 0xFF7EC0F5, 0xFF3E4451, 0xFF4D5566,
            0xFFABB2BF, 0xFF5C6370, 0xFF5C6370, 0x4461AFEF, 0xFFABB2BF,
            0xFFC678DD, 0xFF98C379, 0xFFD19A66, 0xFF5C6370, 0xFF61AFEF, 0xFFE5C07B, 0xFF56B6C2,
            0xFF21252B, 0xFF61AFEF, 0xFFE5C07B, 0xFFE06C75, 0xFF98C379
        ));
        
        // Cyberpunk
        themes.add(new EditorTheme(
            "cyberpunk", "Cyberpunk",
            0xFF0D0D1A, 0xFF080810, 0xFF050508, 0xFF050508,
            0xFFFF00FF, 0xFFFF66FF, 0xFF1A1A2E, 0xFF2A2A4E,
            0xFF00FFFF, 0xFF0088AA, 0xFF0088AA, 0x44FF00FF, 0xFF00FFFF,
            0xFFFF00FF, 0xFF00FF00, 0xFFFFFF00, 0xFF666688, 0xFF00FFFF, 0xFFFF6600, 0xFFFF0066,
            0xFF080810, 0xFF00FFFF, 0xFFFFFF00, 0xFFFF0066, 0xFF00FF00
        ));
        
        // Midnight Blue
        themes.add(new EditorTheme(
            "midnight", "Midnight Blue",
            0xFF0A1628, 0xFF071020, 0xFF050A18, 0xFF050A18,
            0xFF4FC3F7, 0xFF81D4FA, 0xFF1A2744, 0xFF2A3754,
            0xFFE3F2FD, 0xFF5C7A99, 0xFF5C7A99, 0x444FC3F7, 0xFFE3F2FD,
            0xFF7C4DFF, 0xFF69F0AE, 0xFFFFAB40, 0xFF5C7A99, 0xFF40C4FF, 0xFFFFD740, 0xFF18FFFF,
            0xFF071020, 0xFF40C4FF, 0xFFFFD740, 0xFFFF5252, 0xFF69F0AE
        ));
        
        // Load saved theme
        String savedTheme = KashubConfig.getInstance().editorTheme;
        if (savedTheme != null) {
            for (int i = 0; i < themes.size(); i++) {
                if (themes.get(i).id.equals(savedTheme)) {
                    currentIndex = i;
                    break;
                }
            }
        }
    }
    
    public static EditorTheme getCurrentTheme() {
        return themes.get(currentIndex);
    }
    
    public static EditorTheme nextTheme() {
        currentIndex = (currentIndex + 1) % themes.size();
        return themes.get(currentIndex);
    }
    
    public static EditorTheme previousTheme() {
        currentIndex = (currentIndex - 1 + themes.size()) % themes.size();
        return themes.get(currentIndex);
    }
    
    public static EditorTheme getTheme(String id) {
        for (EditorTheme theme : themes) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return themes.get(0);
    }
    
    public static List<EditorTheme> getAllThemes() {
        return new ArrayList<>(themes);
    }
}
