package com.badfic.philbot.listeners.phil.swampy;
import com.badfic.philbot.config.Constants;
import com.badfic.philbot.data.DiscordUser;
import com.badfic.philbot.data.phil.Rank;
import com.google.common.collect.ImmutableMap;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.dv8tion.jda.api.entities.Member;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AdventCommand extends BaseSwampy {
    private final Map<Integer, AdventDay> days;

    public AdventCommand() throws Exception {
        name = "advent";
        aliases = new String[] {"legends"};
        help = "`!!advent` to do your daily legends advent calendar.";

        List<String> lines = IOUtils.readLines(Objects.requireNonNull(Rank.class.getClassLoader().getResourceAsStream("advent.tsv")), StandardCharsets.UTF_8);

        ImmutableMap.Builder<Integer, AdventDay> builder = ImmutableMap.builder();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] values = StringUtils.split(line, '\t');
            String thumbnail = values[1];
            long points = Long.parseLong(values[2]);
            String largeImage = values.length > 3 ? values[3] : null;
            String special = values.length > 4 ? values[4] : null;
            builder.put(26 - i, new AdventDay(26 - i, thumbnail, points, largeImage, special));
        }

        days = builder.build();
    }

    @Override
    protected void execute(CommandEvent event) {
        Member member = event.getMember();

        DiscordUser discordUser = getDiscordUserByMember(member);

        if (discordUser.getAdventCounter() <= 1) {
            event.replyError("The legends advent event only goes 25 days. Congratulations, you finished it.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAdventTime = discordUser.getLastAdvent().plusHours(24);
        if (now.isBefore(nextAdventTime) && discordUser.getAdventCounter() != 0) {
            Duration duration = Duration.between(now, nextAdventTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before you can advent again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before you can advent again");
            }
            return;
        }

        discordUser.setAdventCounter(discordUser.getAdventCounter() - 1);
        discordUser.setLastAdvent(LocalDateTime.now());
        discordUser = discordUserRepository.save(discordUser);

        AdventDay adventDay = days.get(discordUser.getAdventCounter());

        givePointsToMember(adventDay.points, member, PointsStat.ADVENT).whenComplete((v, err) -> {
            if (err != null) {
                event.replyError("Failed to give " + member.getAsMention() + " points, ask a " + Constants.ADMIN_ROLE + " for assistance.");
            } else {
                String description = "Congratulations, " + member.getAsMention()
                        + " your point prize today is " + NumberFormat.getIntegerInstance().format(adventDay.points) + " points!\n\n";

                if (adventDay.special != null) {
                    description += adventDay.special + "\n\n";
                }

                String footer = "Happy soon-to-be Legends premiere day Swamplings! For the rest of April and into May we'll be doing a Daily Legends " +
                        "calendar with a funny blurb and a transparent image. Like the advent calendar in December, if you miss a day you'll be a " +
                        "day behind in prizes.";

                event.reply(Constants.simpleEmbed("Advent Day " + adventDay.day, description, adventDay.largeImage, footer,
                        Constants.COLOR_OF_THE_MONTH, adventDay.thumbnail));
            }
        });
    }

    private static class AdventDay {
        private final int day;
        private final String thumbnail;
        private final long points;
        private final String largeImage;
        private final String special;

        public AdventDay(int day, String thumbnail, long points, String largeImage, String special) {
            this.day = day;
            this.thumbnail = StringUtils.defaultIfBlank(thumbnail, null);
            this.points = points;
            this.largeImage = StringUtils.defaultIfBlank(largeImage, null);
            this.special = StringUtils.defaultIfBlank(special, null);
        }
    }
}