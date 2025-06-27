package dev.balafini.factions.util;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class FormatterUtil {
    private static final DecimalFormat KDR_FORMATTER = new DecimalFormat("0.00");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private FormatterUtil() {
    }

    public static String formatKdr(double kdr) {
        return KDR_FORMATTER.format(kdr);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return DATE_FORMATTER.format(instant);
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 1) {
            return "0s";
        }

        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
