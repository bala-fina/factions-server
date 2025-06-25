package dev.balafini.factions.util;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FactionUtils {
    private static final DecimalFormat KDR_FORMATTER = new DecimalFormat("0.00");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private FactionUtils() {}

    public static String formatKdr(double kdr) {
        return KDR_FORMATTER.format(kdr);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return DATE_FORMATTER.format(instant);
    }
}
