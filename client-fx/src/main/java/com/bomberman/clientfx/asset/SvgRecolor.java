package com.bomberman.clientfx.asset;

import com.bomberman.clientfx.ui.theme.TeamColor;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Repaints a character SVG for another team by swapping the three key colours in one pass. */
public final class SvgRecolor {

    private static final TeamColor KEY = TeamColor.RED;
    private static final Pattern KEY_COLOURS = Pattern.compile(
            "#(" + hex(KEY.main()) + "|" + hex(KEY.shade()) + "|" + hex(KEY.light()) + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    private SvgRecolor() {
    }

    public static String recolor(String svg, TeamColor team) {
        if (team == null || team == KEY) {
            return svg;
        }
        Map<String, String> replacements = Map.of(
                hex(KEY.main()), team.main(),
                hex(KEY.shade()), team.shade(),
                hex(KEY.light()), team.light()
        );
        Matcher matcher = KEY_COLOURS.matcher(svg);
        StringBuilder result = new StringBuilder(svg.length());
        while (matcher.find()) {
            String replacement = replacements.get(matcher.group(1).toUpperCase(Locale.ROOT));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String hex(String colour) {
        return colour.substring(1).toUpperCase(Locale.ROOT);
    }
}
