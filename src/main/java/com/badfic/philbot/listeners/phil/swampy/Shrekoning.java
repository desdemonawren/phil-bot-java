package com.badfic.philbot.listeners.phil.swampy;

import com.badfic.philbot.config.Constants;
import com.badfic.philbot.config.PhilMarker;
import com.badfic.philbot.data.DiscordUser;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Shrekoning extends BaseSwampy implements PhilMarker {

    public static final Set<Long> COMMON_POINTS = new HashSet<>(Arrays.asList(500L, 1_000L));
    public static final Set<Long> RARE_POINTS = new HashSet<>(Arrays.asList(2_000L, 4_000L));
    public static final long LEGENDARY_POINTS = 7_000;
    public static final String SHREKONING = "https://cdn.discordapp.com/attachments/741053845098201099/763280555793580042/the_shrekoning.png";

    public Shrekoning() {
        requiredRole = Constants.ADMIN_ROLE;
        name = "shrekoning";
    }

    @Override
    public void execute(CommandEvent event) {
        shrekoning();
    }

    @Scheduled(cron = "0 37 4 * * ?", zone = "GMT")
    public void shrekoning() {
        List<DiscordUser> allUsers = discordUserRepository.findAll();

        long totalPointsGiven = 0;
        StringBuilder description = new StringBuilder("Shrek has infiltrated the swamp! He's giving \uD83E\uDDC5 \uD83E\uDDC5 \uD83E\uDDC5 " +
                "to all the chaos children!\n\n");
        for (DiscordUser user : allUsers) {
            try {
                Member memberById = philJda.getGuilds().get(0).getMemberById(user.getId());
                if (memberById != null
                        && !memberById.getUser().isBot()
                        && hasRole(memberById, Constants.CHAOS_CHILDREN_ROLE)
                        && user.getXp() > SWEEP_OR_TAX_WINNER_ORGANIC_POINT_THRESHOLD
                        && user.getUpdateTime().isAfter(LocalDateTime.now().minusHours(24))) {

                    long points;
                    if (ThreadLocalRandom.current().nextInt(100) < 5) {
                        points = LEGENDARY_POINTS;
                    } else if (ThreadLocalRandom.current().nextInt(100) < 25) {
                        points = pickRandom(RARE_POINTS);
                    } else {
                        points = pickRandom(COMMON_POINTS);
                    }

                    givePointsToMember(points, memberById);
                    totalPointsGiven += points;
                    description
                            .append(NumberFormat.getIntegerInstance().format(points))
                            .append(" \uD83E\uDDC5")
                            .append(" for <@!")
                            .append(user.getId())
                            .append(">\n");
                }
            } catch (Exception ignored) {}
        }

        description.append("\nShrek gave a total of ")
                .append(NumberFormat.getIntegerInstance().format(totalPointsGiven))
                .append(" \uD83E\uDDC5 to the chaos children!");

        MessageEmbed message = new EmbedBuilder()
                .setTitle("The Shrekoning")
                .setImage(SHREKONING)
                .setColor(Constants.HALOWEEN_ORANGE)
                .setDescription(description.toString())
                .build();

        philJda.getTextChannelsByName(Constants.SWAMPYS_CHANNEL, false)
                .get(0)
                .sendMessage(message)
                .queue();
    }

}
