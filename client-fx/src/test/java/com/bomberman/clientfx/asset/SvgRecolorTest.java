package com.bomberman.clientfx.asset;

import com.bomberman.clientfx.ui.theme.TeamColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SvgRecolorTest {

    private static final String SVG =
            "<rect fill=\"#E53935\"/><rect fill=\"#b71c1c\"/><stop stop-color=\"#FF8A80\"/><rect fill=\"#E539350A\"/><rect fill=\"#2A1F3D\"/>";

    @Test
    void redIsTheKeyTeamAndStaysUnchanged() {
        assertSame(SVG, SvgRecolor.recolor(SVG, TeamColor.RED));
        assertSame(SVG, SvgRecolor.recolor(SVG, null));
    }

    @Test
    void keyColoursAreSwappedCaseInsensitivelyAndOthersAreKept() {
        assertEquals(
                "<rect fill=\"#1E88E5\"/><rect fill=\"#0D47A1\"/><stop stop-color=\"#90CAF9\"/><rect fill=\"#E539350A\"/><rect fill=\"#2A1F3D\"/>",
                SvgRecolor.recolor(SVG, TeamColor.BLUE)
        );
    }

    @Test
    void everyTeamGetsItsOwnPalette() {
        for (TeamColor team : TeamColor.values()) {
            String recoloured = SvgRecolor.recolor("#E53935 #B71C1C #FF8A80", team);
            assertEquals(team.main() + " " + team.shade() + " " + team.light(), recoloured);
        }
    }

    @Test
    void slotsAndUsersMapOntoTheFourTeams() {
        assertEquals(TeamColor.RED, TeamColor.forSlot(0));
        assertEquals(TeamColor.YELLOW, TeamColor.forSlot(3));
        assertEquals(TeamColor.RED, TeamColor.forSlot(4));
        assertEquals(TeamColor.BLUE, TeamColor.forUser(5));
        assertEquals(TeamColor.YELLOW, TeamColor.forUser(-1));
    }
}
